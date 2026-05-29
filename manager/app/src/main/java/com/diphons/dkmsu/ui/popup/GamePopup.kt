package com.diphons.dkmsu.ui.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.edit
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.SystemController
import com.diphons.dkmsu.ui.services.GameTouchListener
import com.diphons.dkmsu.ui.store.SpfColors
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.dkmsvcDaemonPath
import com.diphons.dkmsu.ui.util.dpToPx
import com.diphons.dkmsu.ui.util.onFocusedApp
import java.util.*

class GamePopup(private val mContext: Context) {
    private var startMonitorTime = 0L
    private lateinit var globalSPF: SharedPreferences
    private lateinit var prefs: SharedPreferences
    private lateinit var systemController: SystemController
    private var btn_big: ImageView? = null
    private var btn_mini: ImageView? = null
    private var popupWindowView: RelativeLayout? = null
    private var autoBrightness = true
    @SuppressLint("RtlHardcoded")
    fun showPopupWindow(): Boolean {
        if (show!!) {
            return true
        }
        startMonitorTime = System.currentTimeMillis()

        globalSPF = mContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        prefs = mContext.getSharedPreferences(onFocusedApp, Context.MODE_PRIVATE)

        if (mContext !is AccessibilityService) {
            if (!Settings.canDrawOverlays(mContext)) {
                Toast.makeText(mContext, mContext.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
                return false
            }
        }

        show = true
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = setUpView(mContext)

        val params = LayoutParams()

        if (mContext is AccessibilityService) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.TOP or Gravity.LEFT

        if (Utils.getOrientationGameTool(globalSPF, mContext) > 0) {
            params.y = Utils.getOrientationGameTool(globalSPF, mContext)
        } else {
            params.y = dpToPx(mContext, 55f)
        }

        params.flags =
            LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE

        val gestureDetector = GestureDetector(view!!.context, simpleOnGestureListener)

        view!!.setOnTouchListener(GameTouchListener(params, mWindowManager, gestureDetector))
        params.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        mWindowManager!!.addView(mView, params)

        systemController = SystemController(mContext)
        globalSPF.edit { putBoolean("auto_brightness_backup", systemController.isAutoBrightnessEnabled()) }
        val curBrightness = prefs.getInt("brightness", -1)
        autoBrightness = prefs.getBoolean("auto_brightness", true)
        if (!autoBrightness && curBrightness >= 0) {
            systemController.setAutoBrightness(false)
            systemController.setBrightness(curBrightness)
        }
        return true
    }

    private var view: View? = null

    fun hidePopupWindow() {
        if (show!! && null != mView) {
            mWindowManager!!.removeView(mView)
            mView = null
            show = false
        }
        if (GameControl.isShown == true)
            GameControl(this.mContext).close()
    }

    fun popupShape(context: Context, color: Int): Drawable {
        return object : Drawable() {
            private val density = context.resources.displayMetrics.density
            private val cornerRadius = 15f * density
            private val borderWidth = 1f * density
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = btnColor
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            override fun draw(canvas: Canvas) {
                val bounds = bounds
                val left = bounds.left.toFloat()
                val top = bounds.top.toFloat()
                val right = bounds.right.toFloat()
                val bottom = bounds.bottom.toFloat()

                drawBackground(canvas, left, top, right, bottom)
                drawBordersSmooth(canvas, left, top, right, bottom)
            }

            private fun drawBackground(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
                val path = Path()
                val rect = RectF(left, top, right, bottom)
                val radii = floatArrayOf(0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f)
                path.addRoundRect(rect, radii, Path.Direction.CW)
                canvas.drawPath(path, bgPaint)
            }

            private fun drawBordersSmooth(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
                val halfBorder = borderWidth / 2

                canvas.drawLine(
                    left + halfBorder,
                    top + halfBorder,
                    right - cornerRadius + halfBorder,
                    top + halfBorder,
                    borderPaint
                )

                canvas.drawLine(
                    right - halfBorder,
                    top + cornerRadius - halfBorder,
                    right - halfBorder,
                    bottom - cornerRadius + halfBorder,
                    borderPaint
                )

                canvas.drawLine(
                    left + halfBorder,
                    bottom - halfBorder,
                    right - cornerRadius + halfBorder,
                    bottom - halfBorder,
                    borderPaint
                )

                drawSmoothCorners(canvas, left, top, right, bottom)
            }

            private fun drawSmoothCorners(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
                val halfBorder = borderWidth / 2

                val topRightRect = RectF(
                    right - cornerRadius * 2 + halfBorder,
                    top + halfBorder,
                    right - halfBorder,
                    top + cornerRadius * 2 - halfBorder
                )
                canvas.drawArc(topRightRect, 270f, 90f, false, borderPaint)

                val bottomRightRect = RectF(
                    right - cornerRadius * 2 + halfBorder,
                    bottom - cornerRadius * 2 + halfBorder,
                    right - halfBorder,
                    bottom - halfBorder
                )
                canvas.drawArc(bottomRightRect, 0f, 90f, false, borderPaint)
            }

            override fun setAlpha(alpha: Int) {
                bgPaint.alpha = alpha
                borderPaint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                bgPaint.colorFilter = colorFilter
                borderPaint.colorFilter = colorFilter
            }

            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

            override fun getIntrinsicWidth(): Int = -1
            override fun getIntrinsicHeight(): Int = -1
        }
    }

    private var simpleOnGestureListener: GestureDetector.SimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return super.onDoubleTap(e)
        }
    }

    var onBgColor = Color.WHITE
    var bgColor = 0xFF303030.toInt()
    var btnColor = 0xFFFFBB0E.toInt()
    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility", "InflateParams")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.fw_game, null)
        popupWindowView = view!!.findViewById<View>(R.id.game_tool_card) as RelativeLayout
        btn_big = view!!.findViewById<View>(R.id.btn_big) as ImageView
        btn_mini = view!!.findViewById<View>(R.id.btn_mini) as ImageView

        onBgColor = globalSPF.getInt(SpfColors.COLOR_ON_BACKGROUND, Color.WHITE)
        bgColor = globalSPF.getInt(SpfColors.COLOR_SURFACE_VARIANT, 0xFF303030.toInt())
        btnColor = globalSPF.getInt(SpfColors.COLOR_PRIMARY, 0xFFFFBB0E.toInt())
        popupWindowView!!.background = popupShape(context, bgColor)
        btn_big!!.backgroundTintList = ColorStateList.valueOf(btnColor)
        btn_mini!!.backgroundTintList = ColorStateList.valueOf(btnColor)

        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var show: Boolean? = false
        @SuppressLint("StaticFieldLeak")
        var mView: View? = null
    }
}