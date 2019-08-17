package ru.melodin.fast.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange


object ColorUtil {

    private const val MIN_ALPHA_SEARCH_MAX_ITERATIONS = 10
    private const val MIN_ALPHA_SEARCH_PRECISION = 1

    @JvmOverloads
    fun darkenColor(color: Int, darkFactor: Float = 0.75f): Int {
        var newColor = color
        val hsv = FloatArray(3)
        Color.colorToHSV(newColor, hsv)
        hsv[2] *= darkFactor
        newColor = Color.HSVToColor(hsv)
        return newColor
    }

    @JvmOverloads
    fun lightenColor(color: Int, lightFactor: Float = 1.1f): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= lightFactor
        return Color.HSVToColor(hsv)
    }

    @JvmOverloads
    fun saturateColor(color: Int, factor: Float = 2f): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] *= factor
        return Color.HSVToColor(hsv)
    }

    @JvmOverloads
    fun alphaColor(color: Int, alphaFactor: Float = 0.85f): Int {
        val alpha = Color.alpha(color)

        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return Color.argb((alpha * alphaFactor).toInt(), red, green, blue)
    }

    private fun compositeColors(@ColorInt foreground: Int, @ColorInt background: Int): Int {
        val bgAlpha = Color.alpha(background)
        val fgAlpha = Color.alpha(foreground)
        val a = compositeAlpha(fgAlpha, bgAlpha)

        val r = compositeComponent(
            Color.red(foreground), fgAlpha,
            Color.red(background), bgAlpha, a
        )
        val g = compositeComponent(
            Color.green(foreground), fgAlpha,
            Color.green(background), bgAlpha, a
        )
        val b = compositeComponent(
            Color.blue(foreground), fgAlpha,
            Color.blue(background), bgAlpha, a
        )

        return Color.argb(a, r, g, b)
    }

    private fun compositeAlpha(foregroundAlpha: Int, backgroundAlpha: Int): Int {
        return 0xFF - (0xFF - backgroundAlpha) * (0xFF - foregroundAlpha) / 0xFF
    }

    private fun compositeComponent(fgC: Int, fgA: Int, bgC: Int, bgA: Int, a: Int): Int {
        return if (a == 0) 0 else (0xFF * fgC * fgA + bgC * bgA * (0xFF - fgA)) / (a * 0xFF)
    }

    @FloatRange(from = 0.0, to = 1.0)
    fun calculateLuminance(@ColorInt color: Int): Double {
        var red = Color.red(color) / 255.0
        red = if (red < 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)

        var green = Color.green(color) / 255.0
        green = if (green < 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)

        var blue = Color.blue(color) / 255.0
        blue = if (blue < 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)

        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }


    private fun calculateContrast(@ColorInt foreground: Int, @ColorInt background: Int): Double {
        var color = foreground
        if (Color.alpha(background) != 255) {
            throw IllegalArgumentException(
                "BACKGROUND can not be translucent: #" + Integer.toHexString(
                    background
                )
            )
        }
        if (Color.alpha(color) < 255) {
            color = compositeColors(color, background)
        }

        val luminance1 = calculateLuminance(color) + 0.05
        val luminance2 = calculateLuminance(background) + 0.05

        return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2)
    }

    fun calculateMinimumAlpha(
        @ColorInt foreground: Int, @ColorInt background: Int,
        minContrastRatio: Float
    ): Int {
        if (Color.alpha(background) != 255) {
            throw IllegalArgumentException(
                "BACKGROUND can not be translucent: #" + Integer.toHexString(
                    background
                )
            )
        }

        // First lets check that a fully opaque foreground has sufficient contrast
        var testForeground = setAlphaComponent(foreground, 255)
        var testRatio = calculateContrast(testForeground, background)
        if (testRatio < minContrastRatio) {
            // Fully opaque foreground does not have sufficient contrast, return error
            return -1
        }

        // Binary search to find a value with the minimum value which provides sufficient contrast
        var numIterations = 0
        var minAlpha = 0
        var maxAlpha = 255

        while (numIterations <= MIN_ALPHA_SEARCH_MAX_ITERATIONS && maxAlpha - minAlpha > MIN_ALPHA_SEARCH_PRECISION) {
            val testAlpha = (minAlpha + maxAlpha) / 2

            testForeground = setAlphaComponent(foreground, testAlpha)
            testRatio = calculateContrast(testForeground, background)

            if (testRatio < minContrastRatio) {
                minAlpha = testAlpha
            } else {
                maxAlpha = testAlpha
            }

            numIterations++
        }

        return maxAlpha
    }

    private fun rgbToHsl(
        @IntRange(from = 0x0, to = 0xFF) r: Int,
        @IntRange(from = 0x0, to = 0xFF) g: Int, @IntRange(from = 0x0, to = 0xFF) b: Int,
        hsl: FloatArray
    ) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = Math.max(rf, Math.max(gf, bf))
        val min = Math.min(rf, Math.min(gf, bf))
        val deltaMaxMin = max - min

        var h: Float
        val s: Float
        val l = (max + min) / 2f

        if (max == min) {
            s = 0f
            h = s
        } else {
            h = when (max) {
                rf -> (gf - bf) / deltaMaxMin % 6f
                gf -> (bf - rf) / deltaMaxMin + 2f
                else -> (rf - gf) / deltaMaxMin + 4f
            }

            s = deltaMaxMin / (1f - Math.abs(2f * l - 1f))
        }

        h = h * 60f % 360f
        if (h < 0) {
            h += 360f
        }

        hsl[0] = constrain(h, 0f, 360f)
        hsl[1] = constrain(s, 0f, 1f)
        hsl[2] = constrain(l, 0f, 1f)
    }

    fun colorToHSL(@ColorInt color: Int, hsl: FloatArray) {
        rgbToHsl(Color.red(color), Color.green(color), Color.blue(color), hsl)
    }

    @ColorInt
    fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        val c = (1f - Math.abs(2 * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - Math.abs(h / 60f % 2f - 1f))

        val hueSegment = h.toInt() / 60

        var r = 0
        var g = 0
        var b = 0

        when (hueSegment) {
            0 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * (x + m))
                b = Math.round(255 * m)
            }
            1 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * (c + m))
                b = Math.round(255 * m)
            }
            2 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (c + m))
                b = Math.round(255 * (x + m))
            }
            3 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (x + m))
                b = Math.round(255 * (c + m))
            }
            4 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (c + m))
            }
            5, 6 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (x + m))
            }
        }

        r = constrain(r, 0, 255)
        g = constrain(g, 0, 255)
        b = constrain(b, 0, 255)

        return Color.rgb(r, g, b)
    }

    @ColorInt
    fun setAlphaComponent(
        @ColorInt color: Int,
        @IntRange(from = 0x0, to = 0xFF) alpha: Int
    ): Int {
        if (alpha < 0 || alpha > 255) {
            throw IllegalArgumentException("alpha must be between 0 and 255.")
        }
        return color and 0x00ffffff or (alpha shl 24)
    }

    private fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else if (amount > high) high else amount
    }

    private fun constrain(amount: Int, low: Int, high: Int): Int {
        return if (amount < low) low else if (amount > high) high else amount
    }

    internal fun isLight(color: Int): Boolean {
        return calculateLuminance(color) >= 0.5
    }

    @ColorInt
    fun getAttrColor(context: Context, @AttrRes resId: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }
}
