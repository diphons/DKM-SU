package com.diphons.dkmsu.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.diphons.dkmsu.R

/**
 * @author diphons
 * @date 2025/11/15.
 */
class MaskedGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var gradient: LinearGradient? = null
    private var maskBitmap: Bitmap? = null

    private var pendingColors: IntArray? = null
    private var isGradient: Boolean = false
    private var isHorizontal: Boolean = false

    init {
        setMaskResource(R.drawable.img_perf_settings_perf_scale_progress_left)
    }

    fun setMaskResource(maskResId: Int) {
        maskBitmap?.recycle()
        maskBitmap = BitmapFactory.decodeResource(resources, maskResId)
        invalidate()
    }

    fun setMaskBitmap(bitmap: Bitmap) {
        maskBitmap?.recycle()
        maskBitmap = bitmap
        invalidate()
    }

    fun setColors(vararg colors: Int) {
        pendingColors = colors
        isGradient = colors.size > 1
        createGradientIfPossible()
        invalidate()
    }

    fun setColor(color: Int) {
        pendingColors = intArrayOf(color)
        isGradient = false
        createGradientIfPossible()
        invalidate()
    }

    fun setGradientColors(startColor: Int, endColor: Int) {
        pendingColors = intArrayOf(startColor, endColor)
        isGradient = true
        createGradientIfPossible()
        invalidate()
    }

    fun setGradientHorizontal(value: Boolean) {
        isHorizontal = value
        invalidate()
    }

    private fun createGradientIfPossible() {
        val colors = pendingColors ?: return
        if (width == 0 || height == 0) return

        gradient = if (isGradient && colors.size >= 2) {
            LinearGradient(
                0f, 0f,
                if (isHorizontal) width.toFloat() else 0f,
                if (isHorizontal) 0f else height.toFloat(),
                colors,
                null,
                Shader.TileMode.CLAMP
            )
        } else {
            null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createGradientIfPossible()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mask = maskBitmap ?: return
        val save = canvas.saveLayer(null, null)

        if (isGradient && gradient != null) {
            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            val colors = pendingColors
            if (colors != null && colors.isNotEmpty()) {
                paint.shader = null
                paint.color = colors[0]
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        paint.shader = null
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, null, Rect(0, 0, width, height), paint)

        paint.xfermode = null
        canvas.restoreToCount(save)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        maskBitmap?.recycle()
        maskBitmap = null
    }
}
