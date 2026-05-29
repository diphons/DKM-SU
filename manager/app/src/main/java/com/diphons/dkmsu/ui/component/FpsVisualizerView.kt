package com.diphons.dkmsu.ui.component

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * @author diphons
 * @date 2025/11/13.
 */
class FpsVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maxFps = 120f
    private val fpsValues = mutableListOf<Float>()
    private val barWidth = 10f
    private val spacing = 2f

    private var currentFps = 0f
    private var currentPercent = 100f
    private var colorsValue = 0
    private var printFps = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 180
    }

    private val fpsTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        textAlign = Paint.Align.RIGHT
        textSize = 80f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(40f, 0f, 0f, Color.GREEN)
    }

    private val fpsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        textAlign = Paint.Align.LEFT
        textSize = 38f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        setShadowLayer(20f, 0f, 0f, Color.GREEN)
    }

    private val green by lazy { 0xFF4CAF50.toInt() }
    private val orange by lazy { 0xFFFFD93B.toInt() }
    private val red by lazy { 0xFFF44336.toInt() }

    fun setMaxFps(max: Float) {
        maxFps = max
        invalidate()
    }

    fun setColors(value: Int) {
        colorsValue = value
        invalidate()
    }

    fun setPrintFps(value: Boolean) {
        printFps = value
    }

    fun setFps(value: String) {
        val parsed = value.toFloatOrNull() ?: 0f
        setFps(parsed)
    }

    fun setFps(value: Int) {
        val parsed = value.toFloat()
        setFps(parsed)
    }

    fun setFps(fps: Float) {
        currentFps = fps.coerceAtMost(maxFps)
        currentPercent = (currentFps * 100f / maxFps).coerceIn(0f, 100f)
        fpsValues.add(currentFps)
        val maxBars = (width / (barWidth + spacing)).toInt().coerceAtLeast(1)
        if (fpsValues.size > maxBars) fpsValues.removeAt(0)
        invalidate()
    }

    fun getCurrentFps(): Int = currentFps.toInt()

    private fun getColorForCurrent(): Int {
        return when {
            currentPercent >= 85 -> green
            currentPercent >= 55 -> orange
            else -> red
        }
    }

    fun saveState(context: Context, pkg: String) {
        val prefs = context.getSharedPreferences(pkg, Context.MODE_PRIVATE)
        val data = fpsValues.joinToString(",")

        prefs.edit()
            .putString("fps_values", data)
            .putFloat("current_fps", currentFps)
            .apply()
    }

    fun restoreState(context: Context, pkg: String) {
        val prefs = context.getSharedPreferences(pkg, Context.MODE_PRIVATE)
        val saved = prefs.getString("fps_values", null) ?: return

        fpsValues.clear()
        fpsValues.addAll(
            saved.split(",")
                .mapNotNull { it.toFloatOrNull() }
        )

        post {
            val maxBars = (width / (barWidth + spacing)).toInt().coerceAtLeast(1)
            if (fpsValues.size > maxBars) {
                val takeLast = fpsValues.takeLast(maxBars)
                fpsValues.clear()
                fpsValues.addAll(takeLast)
            }

            currentFps = prefs.getFloat("current_fps", 0f)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        var x = w - (fpsValues.size * (barWidth + spacing))

        paint.color = if (colorsValue != 0) colorsValue else getColorForCurrent()
        fpsValues.forEachIndexed { index, fps ->
            val percent = (fps * 100f / maxFps).coerceIn(0f, 100f)
            val barHeight = h * (percent / 100f)
            val top = h - barHeight
            val fadeFactor = (index.toFloat() / fpsValues.size.toFloat()).coerceIn(0f, 1f)

            paint.alpha = (10 + (120 * fadeFactor)).toInt()
            canvas.drawRoundRect(RectF(x, top, x + barWidth, h), 6f, 6f, paint)
            x += (barWidth + spacing)
        }

        if (printFps) {
            val fpsString = currentFps.toInt().toString()
            val fpsWidth = fpsTextPaint.measureText(fpsString)
            val centerX = w / 2f
            val centerY = h / 2f
            val baselineY = centerY + fpsTextPaint.textSize / 3
            val shadowColor = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnBackground,
                Color.BLACK
            )
            val color = if (colorsValue > 0) colorsValue else getColorForCurrent()
            fpsTextPaint.color = color
            fpsLabelPaint.color = color
            fpsTextPaint.setShadowLayer(20f, 0f, 0f, shadowColor)
            fpsLabelPaint.setShadowLayer(10f, 0f, 0f, shadowColor)
            canvas.drawText(fpsString, centerX + fpsWidth / 2f, baselineY, fpsTextPaint)
            canvas.drawText("fps", centerX + fpsWidth / 2f + 25f, baselineY, fpsLabelPaint)
        }
    }
}
