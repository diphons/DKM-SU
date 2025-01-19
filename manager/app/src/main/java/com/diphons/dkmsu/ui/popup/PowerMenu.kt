package com.diphons.dkmsu.ui.popup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.reboot

class PowerMenu(context: Context) {
    private val mContext: Context = context.applicationContext
    private var mView: View? = null

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
        if (isShown!! &&
                null != this.mView) {
            mWindowManager!!.removeView(mView)
            isShown = false
        }
    }

    @SuppressLint("CutPasteId", "InflateParams")
    private fun setUpView(context: Context): View {

        val view = LayoutInflater.from(context).inflate(R.layout.power_menu, null)
        val reboot_userspace = view.findViewById<View>(R.id.reboot_userspace) as ImageButton
        val card_reboot_userspace = view.findViewById<View>(R.id.card_reboot_userspace) as LinearLayout
        val power_shutdown = view.findViewById<View>(R.id.reboot_power_off) as ImageButton
        val power_reboot = view.findViewById<View>(R.id.reboot_system) as ImageButton
        val power_download = view.findViewById<View>(R.id.reboot_download) as ImageButton
        val power_recovery = view.findViewById<View>(R.id.reboot_recovery) as ImageButton
        val power_fastboot = view.findViewById<View>(R.id.reboot_bootloader) as ImageButton
        val power_emergency = view.findViewById<View>(R.id.reboot_edl) as ImageButton

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
            card_reboot_userspace.visibility = View.VISIBLE
        } else
            card_reboot_userspace.visibility = View.GONE

        power_shutdown.setOnClickListener {
            reboot("shutdown")
            close()
        }
        power_reboot.setOnClickListener {
            reboot("")
            close()
        }
        power_download.setOnClickListener {
            reboot("download")
            close()
        }

        power_recovery.setOnClickListener {
            reboot("recovery")
            close()
        }
        power_fastboot.setOnClickListener {
            reboot("bootloader")
            close()
        }
        power_emergency.setOnClickListener {
            reboot("edl")
            close()
        }
        reboot_userspace.setOnClickListener {
            reboot("userspace")
            close()
        }
        setDialogState(view)

        return view
    }

    @SuppressLint("ClickableViewAccessibility", "GestureBackNavigation")
    private fun setDialogState (view: View) {
        val popupWindowView = view.findViewById<View>(R.id.popup_power)// 非透明的内容区域

        view.setOnTouchListener { _, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()
            val rect = Rect()
            popupWindowView.getGlobalVisibleRect(rect)
            if (!rect.contains(x, y)) {
                close()
            }
            false
        }

        view.setOnKeyListener { v, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    close()
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}