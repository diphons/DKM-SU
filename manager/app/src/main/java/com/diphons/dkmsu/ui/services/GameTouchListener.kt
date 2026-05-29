package com.diphons.dkmsu.ui.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Rect
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.popup.GameControl
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class GameTouchListener(
    private val params: WindowManager.LayoutParams,
    private val windowManager: WindowManager?,
    private val gestureDetector: GestureDetector
) : View.OnTouchListener {

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0

    private var btn_big: ImageView? = null
    private var btn_mini: ImageView? = null
    private var game_tool_card: RelativeLayout? = null
    private var context: Context? = null
    private var bsweep = false
    private var startTime: Long = 0
    private var isDragging = false

    // Spring animations for smooth movement
    private var springY: SpringAnimation? = null
    private var springX: SpringAnimation? = null

    // Velocity tracking for fling
    private var velocityTracker: android.view.VelocityTracker? = null
    private var lastTouchTime = 0L

    // Counter untuk melacak touch di luar btn_big
    private var touchOutsideBigButtonCount = 0
    private val maxTouchOutsideCount = 5

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        context = v.context
        gestureDetector.onTouchEvent(event)
        game_tool_card = v.findViewById(R.id.game_tool_card)
        btn_big = v.findViewById(R.id.btn_big)
        btn_mini = v.findViewById(R.id.btn_mini)
        val globalSPF = context!!.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

        // Initialize velocity tracker
        if (velocityTracker == null) {
            velocityTracker = android.view.VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        @OptIn(DelicateCoroutinesApi::class)
        val hapticFeedback = Runnable {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                } catch (_: Exception) {}
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Handle touch location detection - HANYA jika btn_big visible
                if (btn_big?.visibility == View.VISIBLE) {
                    handleTouchLocationDetection(event)
                }

                // Cancel any ongoing animations
                springY?.cancel()
                springX?.cancel()

                Utils.gameInitialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                initialX = params.x
                initialY = params.y
                startTime = System.currentTimeMillis()
                lastTouchTime = System.currentTimeMillis()
                isDragging = false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleTouchUp(v, event, globalSPF, hapticFeedback)
                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(v, event)
            }
        }
        return false
    }

    /**
     * Mendeteksi apakah touch berada di dalam atau di luar area btn_big
     * Hanya dihitung ketika btn_big visible
     */
    private fun handleTouchLocationDetection(event: MotionEvent) {
        btn_big?.let { bigButton ->
            if (bigButton.visibility != View.VISIBLE) return@let

            // Dapatkan koordinat global btn_big
            val bigButtonLocation = IntArray(2)
            bigButton.getLocationOnScreen(bigButtonLocation)

            // Buat rect untuk area btn_big dengan sedikit toleransi
            val bigButtonRect = Rect(
                bigButtonLocation[0] - 20, // Toleransi 20px di kiri
                bigButtonLocation[1] - 20, // Toleransi 20px di atas
                bigButtonLocation[0] + bigButton.width + 20, // Toleransi 20px di kanan
                bigButtonLocation[1] + bigButton.height + 20 // Toleransi 20px di bawah
            )

            // Cek apakah touch point berada di dalam area btn_big
            val touchX = event.rawX.toInt()
            val touchY = event.rawY.toInt()

            if (bigButtonRect.contains(touchX, touchY)) {
                // Touch di dalam btn_big (atau area toleransi), reset counter
                touchOutsideBigButtonCount = 0
            } else {
                // Touch di luar btn_big, increment counter
                touchOutsideBigButtonCount++
                println("Touch outside big button: $touchOutsideBigButtonCount")

                // Jika sudah mencapai batas maksimal, hide btn_big dan show btn_mini
                if (touchOutsideBigButtonCount >= maxTouchOutsideCount) {
                    println("Hiding big button after $maxTouchOutsideCount touches outside")
                    animateButtonVisibility(btn_big, View.GONE)
                    animateButtonVisibility(btn_mini, View.VISIBLE)
                    touchOutsideBigButtonCount = 0 // Reset counter setelah action
                }
            }
        }
    }

    private fun handleTouchUp(v: View, event: MotionEvent, globalSPF: SharedPreferences, hapticFeedback: Runnable) {
        val upX = event.rawX
        val deltaX = initialTouchX - upX
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - startTime

        // Calculate velocity for fling effect
        velocityTracker?.computeCurrentVelocity(1000) // pixels per second
        val yVelocity = velocityTracker?.yVelocity ?: 0f

        if (abs(deltaX) > 100) {
            if (deltaX < 0) {
                // Swipe right - show btn_big
                hapticFeedback.run()
                animateButtonVisibility(btn_mini, View.GONE)
                animateButtonVisibility(btn_big, View.VISIBLE)
                bsweep = true
                touchOutsideBigButtonCount = 0 // Reset counter saat swipe right
            }
        }

        // Handle button visibility based on current state
        if (btn_big?.visibility == View.VISIBLE) {
            if (deltaX > 0) {
                // Swipe left - hide btn_big, show btn_mini
                animateButtonVisibility(btn_big, View.GONE)
                animateButtonVisibility(btn_mini, View.VISIBLE)
                touchOutsideBigButtonCount = 0 // Reset counter saat swipe left
            } else {
                if (duration <= 150 && !isDragging) {
                    // Quick tap - open game control
                    hapticFeedback.run()
                    GameControl(context!!).open()
                    animateButtonVisibility(btn_big, View.GONE)
                    animateButtonVisibility(btn_mini, View.VISIBLE)
                    touchOutsideBigButtonCount = 0 // Reset counter saat tap
                }
            }

            if (!bsweep && !isDragging) {
                // Jika tidak ada sweep dan tidak dragging, kembali ke btn_mini
                animateButtonVisibility(btn_big, View.GONE)
                animateButtonVisibility(btn_mini, View.VISIBLE)
                touchOutsideBigButtonCount = 0 // Reset counter
            } else {
                bsweep = false
            }
        } else {
            // Jika btn_mini visible dan di-sentuh atau di-swipe, show btn_big
            if (duration <= 150 && !isDragging) {
                // Tap pada btn_mini - show btn_big
                animateButtonVisibility(btn_mini, View.GONE)
                animateButtonVisibility(btn_big, View.VISIBLE)
                touchOutsideBigButtonCount = 0 // Reset counter saat btn_mini di-tap
            }
        }

        // Apply spring animation for smooth settling
        applySpringAnimation(v, yVelocity)

        // Save position
        savePosition(globalSPF)
    }

    private fun handleTouchMove(v: View, event: MotionEvent) {
        val moveX = event.rawX
        val moveY = event.rawY

        val deltaX = moveX - initialTouchX
        val deltaY = moveY - initialTouchY

        // Check if we're actually dragging (minimum movement threshold)
        if (!isDragging && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
            isDragging = true
            // Saat mulai dragging, show btn_big dan hide btn_mini
            btn_mini?.visibility = View.GONE
            btn_big?.visibility = View.VISIBLE
            touchOutsideBigButtonCount = 0 // Reset counter saat mulai drag
        }

        if (isDragging) {
            if (abs(deltaY) > 100) {
                if (deltaY < 0 || deltaX > 0) {
                    bsweep = false
                }
            } else if (abs(deltaX) > 100) {
                if (deltaX < 0) {
                    bsweep = true
                }
            }

            // Smooth interpolation for movement
            val newY = if (bsweep) {
                Utils.gameInitialY
            } else {
                Utils.gameInitialY + deltaY.toInt()
            }

            // Apply easing to the movement for smoother feel
            val easedY = applyEasing(newY.toFloat(), initialY.toFloat())

            params.y = easedY.toInt()
            params.x = 0 // Keep x position fixed for now

            // Use post to ensure smooth updates
            v.post {
                windowManager?.updateViewLayout(v, params)
            }

            startTime = 0
            lastTouchTime = System.currentTimeMillis()
        }
    }

    private fun applyEasing(current: Float, start: Float): Float {
        // Simple ease-out interpolation
        val distance = current - start
        val easedDistance = distance * 0.7f // Reduce movement for smoother feel
        return start + easedDistance
    }

    private fun applySpringAnimation(v: View, yVelocity: Float) {
        // Create spring animation for Y position
        springY = SpringAnimation(v, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce().apply {
                finalPosition = 0f // Return to natural position
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            // Set initial velocity for fling effect
            setStartVelocity(yVelocity * 0.5f) // Reduce velocity for smoother stop
            start()
        }

        // Also spring for X position if needed
        springX = SpringAnimation(v, DynamicAnimation.TRANSLATION_X, 0f).apply {
            spring = SpringForce().apply {
                finalPosition = 0f
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            start()
        }
    }

    private fun animateButtonVisibility(button: ImageView?, visibility: Int) {
        button?.let { btn ->
            when (visibility) {
                View.VISIBLE -> {
                    btn.alpha = 0f
                    btn.visibility = View.VISIBLE
                    btn.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .start()
                }
                View.GONE -> {
                    btn.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.AccelerateInterpolator())
                        .withEndAction {
                            btn.visibility = View.GONE
                            btn.alpha = 1f
                        }
                        .start()
                }
                else -> {
                    btn.visibility = visibility
                }
            }
        }
    }

    private fun savePosition(globalSPF: SharedPreferences) {
        val orientation = context?.resources?.configuration?.orientation ?: Configuration.ORIENTATION_PORTRAIT

        // Use apply() instead of commit() for async saving
        globalSPF.edit().apply {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                putString(SpfConfig.GAME_TOOL_PORTRAIT, params.y.toString())
            } else {
                putString(SpfConfig.GAME_TOOL_LANDSCAPE, params.y.toString())
            }
        }.apply()
    }
}