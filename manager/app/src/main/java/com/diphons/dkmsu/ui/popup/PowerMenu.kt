package com.diphons.dkmsu.ui.popup

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.SpfColors
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.dpToPx
import com.diphons.dkmsu.ui.util.reboot
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PowerMenu(context: Context) {
    private val mContext: Context = context.applicationContext
    private var mView: View? = null

    // UI Components
    private lateinit var rebootUserspace: ImageButton
    private lateinit var cardRebootUserspace: LinearLayout
    private lateinit var powerShutdown: ImageButton
    private lateinit var powerReboot: ImageButton
    private lateinit var powerDownload: ImageButton
    private lateinit var powerRecovery: ImageButton
    private lateinit var powerFastboot: ImageButton
    private lateinit var powerEmergency: ImageButton
    private lateinit var mainContainer: LinearLayout
    private lateinit var popupWindowView: LinearLayout
    private val prefs = mContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    fun open() {
        Utils.checkDrawOverlayPermission(mContext)
        if (isShown!!) {
            return
        }

        isShown = true
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.mView = setUpView(mContext)

        val params = LayoutParams()

        if (Settings.canDrawOverlays(this.mContext)) {
            //6.0+
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        val flags = LayoutParams.FLAG_ALT_FOCUSABLE_IM
        flags.and(LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        flags.and(LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        params.flags = flags
        params.format = PixelFormat.TRANSLUCENT

        params.flags = LayoutParams.FLAG_BLUR_BEHIND
        params.width = LayoutParams.MATCH_PARENT
        params.height = LayoutParams.MATCH_PARENT

        params.gravity = Gravity.CENTER
        params.windowAnimations = R.style.windowAnim

        mWindowManager!!.addView(mView, params)
    }

    fun close() {
        if (!isShown!! || mView == null) return

        isShown = false

        try {
            mWindowManager!!.removeView(mView)
            mView = null
        } catch (_: Exception) { }
    }

    @SuppressLint("CutPasteId", "InflateParams")
    private fun setUpView(context: Context): View {
        mainContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            alpha = 0.95f
            fitsSystemWindows = true
            keepScreenOn = true
        }

        popupWindowView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(context, 6), dpToPx(context, 6), dpToPx(context, 6), dpToPx(context, 6))
            }
            setPadding(dpToPx(context, 25), dpToPx(context, 25), dpToPx(context, 25), dpToPx(context, 25))
            orientation = LinearLayout.VERTICAL

            val color = prefs.getInt(SpfColors.COLOR_SURFACE_VARIANT, 0xFF191C18.toInt())
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(color)
            }
        }

        val firstRow = createButtonRow(context, listOf(
            ButtonConfig(context.getString(R.string.shutdown), context.getDrawable(R.drawable.ic_power_off)) { powerShutdown = it },
            ButtonConfig(context.getString(R.string.restart_app), context.getDrawable(R.drawable.ic_restart)) { powerReboot = it },
            ButtonConfig(context.getString(R.string.reboot_recovery), context.getDrawable(R.drawable.ic_restart_recovery)) { powerRecovery = it }
        ))

        val secondRow = createButtonRow(context, listOf(
            ButtonConfig(context.getString(R.string.reboot_bootloader), context.getDrawable(R.drawable.ic_restart_bootloader)) { powerFastboot = it },
            ButtonConfig(context.getString(R.string.reboot_download), context.getDrawable(R.drawable.ic_restart_download)) { powerDownload = it },
            ButtonConfig(context.getString(R.string.reboot_edl), context.getDrawable(R.drawable.ic_emergency)) { powerEmergency = it }
        )).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(context, 25)
        }

        val userspaceSection = createUserspaceSection(context)

        popupWindowView.addView(firstRow)
        popupWindowView.addView(secondRow)
        popupWindowView.addView(userspaceSection)

        mainContainer.addView(popupWindowView)

        setupButtonListeners()
        setDialogState(mainContainer)

        return mainContainer
    }

    private data class ButtonConfig(
        val text: String,
        val icon: Drawable?,
        val buttonAssign: (ImageButton) -> Unit
    )

    private fun createButtonRow(context: Context, buttons: List<ButtonConfig>): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }.apply {
            buttons.forEachIndexed { index, config ->
                if (index > 0) {
                    addView(createSpacer(context, dpToPx(context, 25)))
                }
                addView(createButtonGroup(context, config))
            }
        }
    }

    private fun createButtonGroup(context: Context, config: ButtonConfig): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 70), LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
        }.apply {
            val button = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(context, 70), dpToPx(context, 70)).apply {
                    setPadding(dpToPx(context, 14), dpToPx(context, 14), dpToPx(context, 14), dpToPx(context, 14))
                }
				val color = prefs.getInt(SpfColors.COLOR_SURFACE_CONTAINER, 0xFF303030.toInt())
				val alpha = 0.75f
				val colorWithAlpha = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
                val iconColor = prefs.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(mContext, R.color.white))
                imageTintList = ColorStateList.valueOf(iconColor)
                setImageDrawable(config.icon)
				background = GradientDrawable().apply {
					cornerRadius = 100f
					setColor(colorWithAlpha)
				}
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            val textView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextColor(prefs.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(mContext, R.color.white)))
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                text = config.text
                setPadding(0, dpToPx(context, 5), 0, 0)
            }

            config.buttonAssign(button)

            addView(button)
            addView(textView)
        }
    }

    private fun createUserspaceSection(context: Context): LinearLayout {
        cardRebootUserspace = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(context, 25)
            }
            orientation = LinearLayout.HORIZONTAL
        }

        val userspaceContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        rebootUserspace = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 70), dpToPx(context, 70)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dpToPx(context, 14), dpToPx(context, 14), dpToPx(context, 14), dpToPx(context, 14))
            }
			val color = prefs.getInt(SpfColors.COLOR_SURFACE_CONTAINER, 0xFF303030.toInt())
			val alpha = 0.75f
			val colorWithAlpha = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            val iconColor = prefs.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(mContext, R.color.white))
            imageTintList = ColorStateList.valueOf(iconColor)
            setImageDrawable(context.getDrawable(R.drawable.ic_restart_ui))
			background = GradientDrawable().apply {
				cornerRadius = 100f
				setColor(colorWithAlpha)
			}
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val userspaceText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(prefs.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(mContext, R.color.white)))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            text = context.getString(R.string.reboot_userspace)
            setPadding(0, dpToPx(context, 5), 0, 0)
        }

        userspaceContainer.addView(rebootUserspace)
        userspaceContainer.addView(userspaceText)
        cardRebootUserspace.addView(userspaceContainer)

        return cardRebootUserspace
    }

    private fun createSpacer(context: Context, width: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(width, 0)
        }
    }

    private fun setupButtonListeners() {
        val pm = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
            cardRebootUserspace.visibility = View.VISIBLE
        } else {
            cardRebootUserspace.visibility = View.GONE
        }

        powerShutdown.setOnClickListener {
            hapticFeedback.run()
            reboot("shutdown")
            close()
        }
        powerReboot.setOnClickListener {
            hapticFeedback.run()
            reboot("")
            close()
        }
        powerDownload.setOnClickListener {
            hapticFeedback.run()
            reboot("download")
            close()
        }
        powerRecovery.setOnClickListener {
            hapticFeedback.run()
            reboot("recovery")
            close()
        }
        powerFastboot.setOnClickListener {
            hapticFeedback.run()
            reboot("bootloader")
            close()
        }
        powerEmergency.setOnClickListener {
            hapticFeedback.run()
            reboot("edl")
            close()
        }
        rebootUserspace.setOnClickListener {
            hapticFeedback.run()
            reboot("userspace")
            close()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val hapticFeedback = Runnable {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                mView!!.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("ClickableViewAccessibility", "GestureBackNavigation")
    private fun setDialogState(view: View) {
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                val rect = Rect()
                popupWindowView.getGlobalVisibleRect(rect)

                if (!rect.contains(x, y)) {
                    close()
                    return@setOnTouchListener true
                }
            }
            false
        }

        view.isFocusableInTouchMode = true
        view.requestFocus()
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}
