import io.lacuna.artifex.Vec
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.openrndr.launch
import org.openrndr.math.*
import kotlin.reflect.KMutableProperty0

sealed class KeyTarget<T>
class TargetValue<T>(val value: T) : KeyTarget<T>()
class TargetProperty<T>(val property: KMutableProperty0<T>) : KeyTarget<T>()
class TargetFunction<T>(val function: () -> T) : KeyTarget<T>()

class Key<T : Any>(
    val prop: KMutableProperty0<T>,
    var target: KeyTarget<T>,
    var start: Long,
    var duration: Double = 0.0

) {
    var complete: (()->Unit)? = null
    var startValue: T? = null
    var targetValue: T? = null
    var started = false
    var finished = false
}

class Storyboard {
    val keys: MutableList<Key<Any>> = mutableListOf()
    var cursor = System.currentTimeMillis()
    internal var finished = false

    fun update() {
        cursor = System.currentTimeMillis()

        val toProcess = keys.filter { it.started || (it.start <= cursor && cursor < it.start + (it.duration * 1000L)) }
        toProcess.forEach {
            if (!it.started) {
                it.startValue = it.prop.get()
                it.targetValue = when (val target = it.target) {
                    is TargetValue -> target.value
                    is TargetProperty -> target.property.get()
                    is TargetFunction -> target.function.invoke()
                }
                it.started = true
            }
            val dt = (((cursor - it.start) / 1000.0) / it.duration).coerceIn(0.0, 1.0)
            when (val targetValue = it.targetValue) {
                is ColorRGBa -> (it.prop).set(mix(it.startValue as ColorRGBa, targetValue, dt))
                is ColorHSVa -> (it.prop).set(mix(it.startValue as ColorHSVa, targetValue, dt))
                is Quaternion -> (it.prop).set(slerp(it.startValue as Quaternion, targetValue, dt))
                is Vector3 -> (it.prop).set(it.startValue as Vector3 * (1.0 - dt) + targetValue * dt)
                is Vector2 -> (it.prop).set(it.startValue as Vector2 * (1.0 - dt) + targetValue * dt)
                is Double -> (it.prop).set(it.startValue as Double * (1.0 - dt) + targetValue * dt)
                is Float -> (it.prop).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toFloat())
                is Int -> (it.prop).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toInt())
                is Long -> (it.prop).set((it.startValue as Float * (1.0 - dt) + targetValue * dt).toLong())
            }
            if (dt >= 1.0 && !it.finished) {
                it.finished = true
                it.complete?.invoke()
                finished = keys.all { it.finished }
            }
            //println("${it.prop} :: ${it.startValue} -> ${it.targetValue} -> ${it.prop.get()}")
        }
    }

    infix fun <T : Any> KMutableProperty0<T>.to(r: KMutableProperty0<T>): Key<T> {
        return Key(this, TargetProperty(r), cursor)
    }

    infix fun <T : Any> KMutableProperty0<T>.to(f: () -> T): Key<T> {
        return Key(this, TargetFunction(f), cursor)
    }


    infix fun KMutableProperty0<Vector3>.to(r: Vector3): Key<Vector3> {
        return Key(this, TargetValue(r), cursor)
    }

    infix fun KMutableProperty0<Double>.to(r: Double): Key<Double> {
        return Key(this, TargetValue(r), cursor)
    }

    infix fun <T : Any> Key<T>.during(s: Double): Key<T> {
        this.duration = s
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

fun storyboard(builder: Storyboard.() -> Unit) : Storyboard {
    val board = Storyboard()
    board.builder()

    GlobalScope.launch {
        while (!board.finished) {
            board.update()
            delay(25)
        }
    }
    return board
}

fun Program.storyboard(builder: Storyboard.() -> Unit) : Storyboard {
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


class A(var x: Double, var y: Double, var v: Vector3)

fun main(args: Array<String>) {
    val a = A(0.0, 0.0, Vector3(0.0, 0.0, 0.0))

    storyboard {
        now
        a::v to Vector3.ONE during 4.0 then {
            println("finished!")
        }
        a::x to 5.0 during 2.0 then {
            println("finished! a::X")
        }
        complete
        a::x to a::y during 4.0 then {
            println("finished !!")
        }
        complete
        a::x to { a.y + 1.0 } during 4.0 then {
            println("done!")
        }
    }

    Thread.sleep(16000)
}
