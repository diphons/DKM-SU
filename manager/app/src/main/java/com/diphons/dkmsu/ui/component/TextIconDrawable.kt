package com.diphons.dkmsu.ui.component

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import kotlin.properties.Delegates

class TextIconDrawable: Drawable() {
    private var alpha = 255
    private var textPaint = TextPaint().apply {
        textAlign = Paint.Align.CENTER
    }
    var text by Delegates.observable("") { _, _, _ -> invalidateSelf() }
    var textColor by Delegates.observable(Color.BLACK) { _, _, _ -> invalidateSelf() }

    private fun fitText(width: Int) {
        textPaint.textSize = 48f
        val widthAt48 = textPaint.measureText(text)
        textPaint.textSize = 48f / widthAt48 * width.toFloat()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        fitText(width)
        textPaint.color = ColorUtils.setAlphaComponent(textColor, alpha)
        canvas.drawText(text, width / 2f, height / 2f, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

}