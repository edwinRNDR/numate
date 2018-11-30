package org.openrndr.numate

import org.openrndr.math.Vector2
import org.openrndr.math.bezier

private fun abezier(cx0:Double, cy0:Double, cx1:Double, cy1:Double, t:Double): Double {
    return bezier(Vector2(0.0, 0.0), Vector2(cx0, cy0), Vector2(cx1, cy1), Vector2(1.0, 1.0), t).y
}

fun noEase(t: Double) = t

private fun easeInSine(t: Double) = abezier(0.47, 00.0, 0.745, 0.715, t)
private fun easeOutSine(t: Double) = abezier(0.39, 0.575, 0.565, 1.0, t)
private fun easeInOutSine(t : Double) = abezier(0.445, 0.05, 0.55, 0.95, t)

private fun easeInQuad(t: Double) = abezier(0.55, 0.085, 0.68, 0.53, t)
private fun easeOutQuad(t: Double) = abezier(0.25 ,0.46, 0.45, 0.94, t)
private fun easeInOutQuad(t: Double) = abezier(0.455, 0.03, 0.515, 0.995, t)

private fun easeInCubic(t: Double) = abezier(0.55, 0.055, 0.675, 0.19, t)
private fun easeOutCubic(t: Double) = abezier(0.215, 0.61, 0.355, 1.0, t)
private fun easeInOutCubic(t : Double) = abezier(0.645, 0.045, 0.355, 1.0, t)

private fun easeInQuint(t: Double) = abezier(0.755, 0.05, 0.855, 0.06, t)
private fun easeOutQuint(t: Double) = abezier(0.23, 1.0, 0.32, 1.0, t)
private fun easeInOutQuint(t : Double) = abezier(0.86, 0.0, 0.07, 1.0, t)

private fun easeInCirc(t: Double) = abezier(0.6, 0.04, 0.98, 0.335, t)
private fun easeOutCirc(t: Double) = abezier(0.075, 0.82, 0.165, 1.0, t)
private fun easeInOutCirc(t: Double) = abezier(0.785, 0.135, 0.15, 0.86, t)

private fun easeInQuart(t: Double) = abezier(0.895, 0.03, 0.685, 0.22, t)
private fun easeOutQuart(t: Double) = abezier(0.165, 0.84, 0.44, 1.0, t)
private fun easeInOutQuart(t: Double) = abezier(0.77, 0.0, 0.175, 1.0, t)

private fun easeInExpo(t: Double) = abezier(0.95, 0.05, 0.795, 0.035, t)
private fun easeOutExpo(t: Double) = abezier(0.19, 1.0, 0.22, 1.0, t)
private fun easeInOutExpo(t: Double) = abezier(1.0, 0.0, 0.0, 1.0, t)

private fun easeInBack(t: Double) = abezier(0.6, -0.28, 0.735, 0.045, t)
private fun easeOutBack(t: Double) = abezier(0.175, 0.885, 0.3, 1.275, t)
private fun easeInOutBack(t: Double) = abezier(0.68, -0.55, 0.265, 1.55 ,t)

val inSine = ::easeInSine
val outSine = ::easeOutSine
val inOutSine = ::easeInOutSine

val inQuad = ::easeInQuad
val outQuad = ::easeOutQuad
val inOutQuad = ::easeInOutQuad

val inCubic = ::easeInCubic
val outCubic = ::easeOutCubic
val inOutCubic = ::easeInOutCubic

val inQuint = ::easeInQuint
val outQuint = ::easeOutQuint
val inOutQuint = ::easeInOutQuint

val inCirc = ::easeInCirc
val outCirc = ::easeOutCirc
val inOutCirc = ::easeInOutCirc

val inQuart = ::easeInQuart
val outQuart = ::easeOutQuart
val inOutQuart = ::easeInOutQuart

val inExpo = ::easeInExpo
val outExpo = ::easeOutExpo
val inOutExpo = ::easeInOutExpo

val inBack = ::easeInBack
val outBack = ::easeOutBack
val inOutBack = ::easeInOutBack