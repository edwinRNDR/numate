import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openrndr.math.Vector3
import kotlin.reflect.KMutableProperty0

sealed class KeyTarget<T>
class TargetValue<T>(val value: T) : KeyTarget<T>()
class TargetProperty<T>(val property: KMutableProperty0<T>) : KeyTarget<T>()

class Key<T : Any>(
    val prop: KMutableProperty0<T>,
    var target: KeyTarget<T>,
    var start: Long,
    var duration: Double = 0.0
) {
    var startValue: T? = null
    var targetValue: T? = null
    var started = false
}

class Storyboard {
    val keys: MutableList<Key<Any>> = mutableListOf()
    var cursor = System.currentTimeMillis()

    fun update() {
        cursor = System.currentTimeMillis()

        keys.filter { it.started || (it.start <= cursor && cursor < it.start + (it.duration * 1000L)) }.forEach {
            if (!it.started) {
                it.startValue = it.prop.get()
                it.targetValue = when (val target = it.target) {
                    is TargetValue -> target.value
                    is TargetProperty -> target.property.get()
                }
                it.started = true
            }

            val dt = (((cursor - it.start) / 1000.0) / it.duration).coerceIn(0.0, 1.0)
            when (val targetValue = it.targetValue) {
                is Vector3 -> (it.prop).set(it.startValue as Vector3 * (1.0 - dt) + targetValue * dt)
                is Double -> (it.prop).set(it.startValue as Double * (1.0 - dt) + targetValue * dt)
            }
            if (dt >= 1.0) {
                it.started = false
            }
            println("${it.prop} :: ${it.startValue} -> ${it.targetValue} -> ${it.prop.get()}")
        }
    }

    infix fun Unit.and(kMutableProperty0: Unit) {}


    @JvmName("toDoubleProperty")
    infix fun KMutableProperty0<Double>.to(r: KMutableProperty0<Double>): Key<Double> {
        return Key(this, TargetProperty(r), cursor)
    }


    @JvmName("toVector3Property")
    infix fun KMutableProperty0<Vector3>.to(r: KMutableProperty0<Vector3>): Key<Vector3> {
        return Key(this, TargetProperty(r), cursor)
    }

    infix fun KMutableProperty0<Vector3>.to(r: Vector3): Key<Vector3> {
        return Key(this, TargetValue(r), cursor)
    }

    infix fun KMutableProperty0<Double>.to(r: Double): Key<Double> {
        return Key(this, TargetValue(r), cursor)
    }

    infix fun <T : Any> Key<T>.during(s: Double) {
        this.duration = s
        keys.add(this as Key<Any>)
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

fun storyboard(builder: Storyboard.() -> Unit) {

    val board = Storyboard()
    board.builder()

    GlobalScope.launch {
        while (true) {
            board.update()
            delay(25)
        }
    }
}


class A(var x: Double, var y: Double, var v: Vector3)

fun main(args: Array<String>) {
    val a = A(0.0, 0.0, Vector3(0.0, 0.0, 0.0))

    storyboard {
        now
        a::v to Vector3.ONE during 4.0
        a::x to 5.0 during 2.0
        complete
        (a::x to a::y) during 4.0
    }

    Thread.sleep(8000)
}
