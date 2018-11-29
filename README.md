# numate

Proof-of-concept implementation for a reflection-free Kotlin/OPENRNDR animation library. The aim for this work is to
provide a replacement for the `openrndr-animatable` library.

## Usage

```kotlin

// -- a basic class
class A(var x: Double, var y: Double, var v: Vector3)

// -- an instance of that basic class
val a = A(0.0, 0.0, Vector3(0.0, 0.0, 0.0))

storyboard {

    // -- explicitly move the animation cursor to the current time
    now

    // -- animate v to (1,1,1), this will take 4 seconds
    a::v to Vector3.ONE during 4.0

    // -- meanwhile animate x to 5.0
    a::x to 5.0 during 2.0

    // -- wait for last animation to complete (moves cursor)
    complete

    // -- animate x to value of y at the moment this animation segment starts
    a::x to a::y during 4.0

    complete

    // -- animate x to value of y + 1.0 at the moment this animation segment starts
    a::x to { a.y + 1.0 } during 4.0 then {
        println("done!")
    }
    complete

    // -- that's different from this where (y + 1.0) is evaluated immediately
    a::x to (a.y + 1.0) during 4.0 then {
        println("done!")
    }
}
```