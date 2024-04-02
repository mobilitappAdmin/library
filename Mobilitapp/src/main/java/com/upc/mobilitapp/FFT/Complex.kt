package com.upc.mobilitapp.FFT

import java.util.*

/**
 * Represents a complex number with real and imaginary parts.
 *
 * @author Adrian Catalin
 * @property re The real part of the complex number.
 * @property im The imaginary part of the complex number.
*/
class Complex(// the real part
    private val re: Double, // the imaginary part
    private var im: Double
) {
    // return a string representation of the invoking Complex object
    override fun toString(): String {
        if (im == 0.0) return re.toString() + ""
        if (re == 0.0) return im.toString() + "i"
        return if (im < 0) re.toString() + " - " + -im + "i" else re.toString() + " + " + im + "i"
    }

    // return abs/modulus/magnitude
    fun abs(): Double {
        return Math.hypot(re, im)
    }

    // return angle/phase/argument, normalized to be between -pi and pi
    fun phase(): Double {
        return Math.atan2(im, re)
    }

    // return a new Complex object whose value is (this + b)
    operator fun plus(b: Complex): Complex {
        val a = this // invoking object
        val real = a.re + b.re
        val imag = a.im + b.im
        return Complex(real, imag)
    }

    // return a new Complex object whose value is (this - b)
    operator fun minus(b: Complex): Complex {
        val a = this
        val real = a.re - b.re
        val imag = a.im - b.im
        return Complex(real, imag)
    }

    // return a new Complex object whose value is (this * b)
    operator fun times(b: Complex): Complex {
        val a = this
        val real = a.re * b.re - a.im * b.im
        val imag = a.re * b.im + a.im * b.re
        return Complex(real, imag)
    }

    // return a new object whose value is (this * alpha)
    fun scale(alpha: Double): Complex {
        return Complex(alpha * re, alpha * im)
    }

    // return a new Complex object whose value is the conjugate of this
    fun conjugate(): Complex {
        return Complex(re, -im)
    }

    // return a new Complex object whose value is the reciprocal of this
    fun reciprocal(): Complex {
        val scale = re * re + im * im
        return Complex(re / scale, -im / scale)
    }

    // return the real or imaginary part
    fun re(): Double {
        return re
    }

    fun im(): Double {
        return im
    }

    // return a / b
    fun divides(b: Complex): Complex {
        val a = this
        return a.times(b.reciprocal())
    }

    // return a new Complex object whose value is the complex exponential of this
    fun exp(): Complex {
        return Complex(Math.exp(re) * Math.cos(im), Math.exp(re) * Math.sin(im))
    }

    // return a new Complex object whose value is the complex sine of this
    fun sin(): Complex {
        return Complex(Math.sin(re) * Math.cosh(im), Math.cos(re) * Math.sinh(im))
    }

    // return a new Complex object whose value is the complex cosine of this
    fun cos(): Complex {
        return Complex(Math.cos(re) * Math.cosh(im), -Math.sin(re) * Math.sinh(im))
    }

    // return a new Complex object whose value is the complex tangent of this
    fun tan(): Complex {
        return sin().divides(cos())
    }

    // See Section 3.3.
    override fun equals(x: Any?): Boolean {
        if (x == null) return false
        if (this.javaClass != x.javaClass) return false
        val that = x as Complex
        return (re == that.re) && (im == that.im)
    }

    // See Section 3.3.
    override fun hashCode(): Int {
        return Objects.hash(re, im)
    }

    companion object {
        // a static version of plus
        fun plus(a: Complex, b: Complex): Complex {
            val real = a.re + b.re
            val imag = a.im + b.im
            return Complex(real, imag)
        }
    }
}