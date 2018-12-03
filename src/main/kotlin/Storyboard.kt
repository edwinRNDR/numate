package org.openrndr.numate

import kotlinx.coroutines.*
import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix

import org.openrndr.math.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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
    var start: Double,
    var duration: Double = 0.0,
    var easer: (Double) -> Double = ::noEase

) {
    var complete: (() -> Unit)? = null
    var startValue: T? = null
    var targetValue: T? = null
    var started = false
    var finished = false
}

class Storyboard(val clock: () -> Double) {
    private val keys: MutableList<Key<Any>> = mutableListOf()
    var cursor = clock()
        private set

    /**
     * is storyboard finished
     */
    var finished = false
        private set

    var onFinished: (() -> Unit)? = null

    internal fun update() {
        cursor = clock()

        val toProcess = keys.filter { it.started || (it.start <= cursor && cursor < it.start + (it.duration)) }
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
            val dt = it.easer((((cursor - it.start)) / it.duration).coerceIn(0.0, 1.0))
            when (val targetValue = it.targetValue) {
                is ColorRGBa -> (it.subject).set(mix(it.startValue as ColorRGBa, targetValue, dt))
                is ColorHSVa -> (it.subject).set(mix(it.startValue as ColorHSVa, targetValue, dt))
                is Quaternion -> (it.subject).set(slerp(it.startValue as Quaternion, targetValue, dt))
                is Spherical -> (it.subject).set(it.startValue as Spherical * (1.0 - dt) + targetValue * dt)
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
                val finishedNow = keys.all { it.finished }
                if (finishedNow && !finished) {
                    cursor = it.start + it.duration
                    finished = true
                    onFinished?.invoke()
                }
            }
        }
    }

    /**
     * DSL
     */
    infix fun <T : Any> KMutableProperty0<T>.to(r: KMutableProperty0<T>): Key<T> {
        return Key(SubjectProperty(this), TargetProperty(r), cursor)
    }

    /**
     * DSL
     */
    @JvmName("toDoubleFunction")
    infix fun KMutableProperty0<Double>.to(f: (() -> Double)): Key<Double> {
        return Key(SubjectProperty(this), TargetFunction(f), cursor)
    }

    /**
     * DSL
     */
    @JvmName("toVector3Function")
    infix fun KMutableProperty0<Vector3>.to(f: (() -> Vector3)): Key<Vector3> {
        return Key(SubjectProperty(this), TargetFunction(f), cursor)
    }

    /**
     * DSL
     */
    infix fun <T : Any> KMutableProperty0<T>.to(v: T): Key<T> {
        return Key(SubjectProperty(this), TargetValue(v), cursor)
    }

    /**
     * DSL
     */

    infix fun <T : Any> ValueReference<T>.to(v: T): Key<T> {
        return Key(SubjectValueReference(this), TargetValue(v), cursor)
    }

    /**
     * DSL. set time of animation
     */
    infix fun <T : Any> Key<T>.during(s: Double): Key<T> {
        this.duration = s
        keys.add(this as Key<Any>)
        return this
    }

    /**
     * DSL. set easing of animation
     */
    infix fun <T : Any> Key<T>.eased(easer: (Double) -> Double): Key<T> {
        this.easer = easer
        keys.add(this as Key<Any>)
        return this
    }

    /**
     * DSL. add function to execute after animation is completed
     */
    infix fun <T : Any> Key<T>.then(f: () -> Unit) {
        this.complete = f
    }

    /**
     * DSL. move cursor to current time
     */
    val now: Unit
        get() {
            cursor = clock()
        }

    /**
     * DSL. wait for previously queued animation to complete
     */
    val complete: Unit
        get() {
            keys.lastOrNull()?.let {
                cursor = it.start + (it.duration).toLong()
            }
        }

    fun cancel() {
        finished = true
    }

    internal fun clear() {
        keys.clear()
        finished = false
    }

}

fun storyboard(
    scope: CoroutineScope,
    context: CoroutineContext,
    dispatcher: CoroutineDispatcher,
    clock: () -> Double,
    loop: Boolean = false,
    builder: Storyboard.() -> Unit
): Storyboard {
    val board = Storyboard(clock)
    board.builder()

    scope.launch(context + dispatcher) {
        while (!board.finished) {
            board.update()
            yield()
        }
    }
    if (loop) {
        board.onFinished = {
            board.clear()
            board.builder()
        }
    }
    return board
}

/**
 * builds and launches an animation storyboard
 */
fun Program.storyboard(loop: Boolean = false, builder: Storyboard.() -> Unit): Storyboard {
    return storyboard(GlobalScope, EmptyCoroutineContext, dispatcher, clock, loop, builder)
}

fun Program.storyboard(scope: CoroutineScope, loop: Boolean = false, builder: Storyboard.() -> Unit): Storyboard {
    return storyboard(scope, EmptyCoroutineContext, dispatcher, clock, loop, builder)
}