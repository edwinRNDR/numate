# numate

Proof-of-concept implementation for a reflection-free Kotlin/OPENRNDR animation library. The aim for this work is to
provide a simple library for interactive animations.

Numate (codename) relies on Kotlin (1.3) coroutines for its animation update cycles.

## Storyboard

The `Storyboard` class is Numate's primary building block. It is used to declare animations on _property references_.

A `Storyboard` is best made using the `Program.storyboard` extension method, which sets up the `Storyboard` to use the frame-bound coroutine dispatcher of `Program` and uses `Program`'s `clock` function.

#### Example Usage
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


### Looping animations

Storyboards can be constructed to loop forever like this.

```!kotlin
application {
    program {
        val a = object { var x: Double = 0.0; var y: Double = 0.0 }
        storyboard(loop = true) {
            a::x to (Math.random() * width) during 2.0 eased inOutQuad
            a::y to (Math.random() * height) during 2.0 eased inOutQuad
        }
        extend {
            drawer.circle(a.x, a.y, 50.0)
        }
    }
}
```
What is interesting here is that the _behaviour_ of the animation is looping, the actual animation is different at every cycle.

## Try it out

Numate is work-in-progress and its API is potentially subject to change. However, the animator
is fully functional and can be tried out if you are interested.

The easiest way to add Numate to your project is by adding the following
repository and dependencies to your `build.gradle`

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.edwinRNDR:numate:33598c74c5'
}
```

[![](https://jitpack.io/v/edwinRNDR/numate.svg)](https://jitpack.io/#edwinRNDR/numate)
