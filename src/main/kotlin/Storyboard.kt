package org.openrndr.numate

import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.openrndr.launch
import org.openrndr.math.*
import kotlin.reflect.KMutableProperty0

class ValueReference<T>(val set: (T) -> Unit, val get: () -> T)

sealed class Subject<T> {
    abstract fun set(v: T)
    abstract fun get(): T
}

private class SubjectProperty<T>(val property: KMutableProperty0<T>) : Subject<T>() {
    override fun set(v: T) = property.set(v)
    override fun get(): T = property.get()
}

private class SubjectValueReference<T>(val valueReference: ValueReference<T>) : Subject<T>() {
    override fun set(v: T) {
        valueReference.set(v)
    }
    override fun get(): T = valueReference.get()
}

sealed class KeyTarget<T>
class TargetValue<T>(val value: T) : KeyTarget<T>()
class TargetProperty<T>(val property: KMutableProperty0<T>) : KeyTarget<T>()
class TargetFunction<T>(val function: () -> T) : KeyTarget<T>()

class Key<T : Any>(
    val subject: Subject<T>,
    var target: KeyTarget<T>,
    var start: Long,
    var duration: Double = 0.0,
    var easer: (Double) -> Double = ::noEase

) {
    var complete: (() -> Unit)? = null
    var startValue: T? = null
    var targetValue: T? = null
    var started = false
    var finished = false
}

class Storyboard {
    val keys: MutableList<Key<Any>> = mutableListOf()
    var cursor = System.currentTimeMillis()

    /**
     * is storyboard finished
     */
    var finished = false

    internal fun update() {
        cursor = System.currentTimeMillis()

        val toProcess = keys.filter { it.started || (it.start <= cursor && cursor < it.start + (it.duration * 1000L)) }
        toProcess.forEach {
            if (!it.started) {
                it.startValue = it.subject.get()
                it.targetValue = when (val target = it.target) {
                    is TargetValue -> target.value
                    is TargetProperty -> target.property.get()
                    is TargetFunction -> target.function.invoke()
                }
                it.started = true
            }
            val dt = it.easer((((cursor - it.start) / 1000.0) / it.duration).coerceIn(0.0, 1.0))
            when (val targetValue = it.targetValue) {
                is ColorRGBa -> (it.subject).set(mix(it.startValue as ColorRGBa, targetValue, dt))
                is ColorHSVa -> (it.subject).set(mix(it.startValue as ColorHSVa, targetValue, dt))
                is Quaternion -> (it.subject).set(slerp(it.startValue as Quaternion, targetValue, dt))
                is Vector3 -> (it.subject).set(it.startValue as Vector3 * (1.0 - dt) + targetValue * dt)
                is Vector2 -> (it.subject).set(it.startValue as Vector2 * (1.0 - dt) + targetValue * dt)
                is Double -> (it.subject).set(it.startValue as Double * (1.0 - dt) + targetValue * dt)
                is Float -> (it.subject).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toFloat())
                is Int -> (it.subject).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toInt())
                is Long -> (it.subject).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toLong())
            }
            if (dt >= 1.0 && !it.finished) {
                it.finished = true
                it.complete?.invoke()
                finished = keys.all { it.finished }
            }
        }
    }

    infix fun <T : Any> KMutableProperty0<T>.to(r: KMutableProperty0<T>): Key<T> {
        return Key(SubjectProperty(this), TargetProperty(r), cursor)
    }

    infix fun KMutableProperty0<Double>.to(f: (() -> Double)): Key<Double> {
        return Key(SubjectProperty(this), TargetFunction(f), cursor)
    }

    infix fun <T : Any> KMutableProperty0<T>.to(v: T): Key<T> {
        return Key(SubjectProperty(this), TargetValue(v), cursor)
    }

    infix fun <T: Any> ValueReference<T>.to(v : T) : Key<T> {
        return Key(SubjectValueReference(this), TargetValue(v), cursor)
    }

    infix fun <T : Any> Key<T>.during(s: Double): Key<T> {
        this.duration = s
        keys.add(this as Key<Any>)
        return this
    }

    infix fun <T : Any> Key<T>.eased(easer: (Double) -> Double): Key<T> {
        this.easer = easer
        keys.add(this as Key<Any>)
        return this
    }

    infix fun <T : Any> Key<T>.then(f: () -> Unit) {
        this.complete = f
    }

    val now: Unit
        get() {
            cursor = System.currentTimeMillis()
        }

    val complete: Unit
        get() {
            keys.lastOrNull()?.let {
                cursor = it.start + (it.duration * 1000).toLong()
            }
        }
}

/*
fun storyboard(builder: Storyboard.() -> Unit): Storyboard {
    val board = Storyboard()
    board.builder()

    GlobalScope.launch {
        while (!board.finished) {
            board.update()
            delay(25)
        }
    }
    return board
}*/

/**
 * builds and launches an animation storyboard
 */
fun Program.storyboard(builder: Storyboard.() -> Unit): Storyboard {
    val board = Storyboard()
    board.builder()

    launch {
        while (!board.finished) {
            board.update()
            yield()
        }
    }
    return board
}