package com.diphons.dkmsu.ui.popup

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import com.airbnb.lottie.LottieAnimationView
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.data.GlobalStatus
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.CpuFrequencyUtils
import com.diphons.dkmsu.ui.component.CpuLoadUtils
import com.diphons.dkmsu.ui.component.FpsUtils
import com.diphons.dkmsu.ui.component.GpuUtils
import com.diphons.dkmsu.ui.component.TripleCacheValue
import com.diphons.dkmsu.ui.component.CpuCoreInfo
import com.diphons.dkmsu.ui.component.FpsVisualizerView
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.setKernel
import com.google.android.material.tabs.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit
import com.diphons.dkmsu.ui.util.Utils.DYNAMIC_CHARGING
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.isCharging
import androidx.core.view.isGone
import com.diphons.dkmsu.ui.component.BackgroundTaskManager
import com.diphons.dkmsu.ui.component.MaskedGradientView
import com.diphons.dkmsu.ui.component.SystemController
import com.diphons.dkmsu.ui.store.SpfColors
import com.diphons.dkmsu.ui.util.FPSActive
import com.diphons.dkmsu.ui.util.GameActive
import com.diphons.dkmsu.ui.util.Utils.SOUND_CONTROL
import com.diphons.dkmsu.ui.util.dpToPx
import com.diphons.dkmsu.ui.util.getCurrentCharger
import com.diphons.dkmsu.ui.util.onFocusedApp
import com.diphons.dkmsu.ui.util.onGameFps
import com.diphons.dkmsu.ui.util.prefMode
import com.diphons.dkmsu.ui.util.setChgMax
import androidx.core.view.isVisible

/**
 * @author diphons
 * @date 2025/11/13.
 */
class GameControl(context: Context) {
    private val mContext: Context = context.applicationContext
    private lateinit var globalSPF: SharedPreferences
    private lateinit var prefs: SharedPreferences
    private lateinit var systemController: SystemController
    private var mView: View? = null

    private var btn_touchimprove: LinearLayout? = null
    private var btn_touchimprove_icon: ImageView? = null
    private var btn_touchimprove_label: TextView? = null
    private var btn_perfasgame: LinearLayout? = null
    private var btn_perfasgame_icon: ImageView? = null
    private var btn_perfasgame_label: TextView? = null
    private var btn_sound_control: LinearLayout? = null
    private var btn_sound_control_icon: ImageView? = null
    private var btn_sound_control_label: TextView? = null
    private var clock_icon: ImageView? = null
    private var clock_now: TextView? = null
    private var battery_now: TextView? = null
    private var battery_icon: ImageView? = null
    private var temp_now: TextView? = null
    private var temp_now_icon: ImageView? = null
    private var gamer_header: TextView? = null
    private var header_icon: ImageView? = null
    private var content_tool: ScrollView? = null
    private var card_chg_max: LinearLayout? = null
    private var btn_fpsmeter: LinearLayout? = null
    private var btn_fpsmeter_icon: ImageView? = null
    private var btn_fpsmeter_label: TextView? = null
    private var btn_bypass: LinearLayout? = null
    private var btn_bypass_icon: ImageView? = null
    private var btn_bypass_label: TextView? = null
    private var popup_game_control: RelativeLayout? = null
    private var card_info_bg: RelativeLayout? = null
    private var home_fps_chat: FpsVisualizerView? = null
    private var fps_value: TextView? = null
    private var fps_label: TextView? = null
    private var card_fps_calc: LinearLayout? = null
    private var fps_label_min: TextView? = null
    private var fps_value_min: TextView? = null
    private var fps_value_max: TextView? = null
    private var fps_label_max: TextView? = null
    private var fps_value_avg: TextView? = null
    private var fps_label_avg: TextView? = null

    // Brightness and Volume
    private var seek_brightness_icon: ImageButton? = null
    private var seek_brightness: SeekBar? = null
    private var txt_brightness: TextView? = null

    // Perf anim
    private var card_game_perf: LinearLayout? = null
    private var card_info_main: RelativeLayout? = null
    private var perf_lower: Button? = null
    private var perf_balance: Button? = null
    private var perf_pro_game: Button? = null
    private var perf_anim_bg: LottieAnimationView? = null
    private var progress_left_gradient: MaskedGradientView? = null
    private var progress_right_gradient: MaskedGradientView? = null
    private var perf_progress_left: RelativeLayout? = null
    private var perf_progress_right: RelativeLayout? = null
    private var graph_perf_low: MaskedGradientView? = null
    private var graph_perf_bal: MaskedGradientView? = null
    private var graph_perf_pro: MaskedGradientView? = null
    private var perf_heat_label: TextView? = null
    private var perf_battery_label: TextView? = null
    private var perf_touch_label: TextView? = null
    private var perf_smooth_label: TextView? = null
    private var perf_quality_label: TextView? = null

    var hLow: Float? = null
    var hBal: Float? = null
    var hPro: Float? = null
    var hRLow: Float? = null
    var hRBal: Float? = null
    var hRPro: Float? = null

    private var cpuLoadUtils = CpuLoadUtils()
    private var cpu_core_label: TextView? = null
    private var cpu_core_total_load: TextView? = null
    private var gpu_label: TextView? = null
    private var gpu_load_info: TextView? = null
    private var ram_label: TextView? = null
    private var ram_load_info: TextView? = null
    private var activityManager: ActivityManager? = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var home_open_volume: ImageButton? = null
    private var home_memory_clear: ImageButton? = null
    private var batt_label: TextView? = null
    private var batt_disc_info: TextView? = null

    private var card_info_sound: RelativeLayout? = null
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private var popup_switch_headfon: Switch? = null
    private var popup_seek_microfon: SeekBar? = null
    private var popup_txt_mikrofon: TextView? = null
    private var popup_seek_earfon: SeekBar? = null
    private var popup_txt_earfon: TextView? = null
    private var popup_judul_headfon: TextView? = null
    private var popup_judul_headfon2: TextView? = null
    private var popup_card_seek_headfon: LinearLayout? = null
    private var popup_seek_headfon: SeekBar? = null
    private var popup_txt_headfon: TextView? = null
    private var popup_seek_headfon2: SeekBar? = null
    private var popup_txt_headfon2: TextView? = null
    private var btn_reset_sc: LinearLayout? = null
    private var btn_charger_max: LinearLayout? = null
    private var btn_charger_max_icon: ImageView? = null
    private var card_info_sound_label: TextView? = null
    private var popup_seek_microfon_label: TextView? = null
    private var popup_seek_earfon_label: TextView? = null
    private var btn_reset_sc_icon: ImageView? = null

    private var card_charger_max: RelativeLayout? = null
    private var charger_max_label: TextView? = null
    private var txt_current_chg: TextView? = null
    private var txt_watt: TextView? = null
    private var btn_dynamic_charger: LinearLayout? = null
    private var btn_dynamic_charger_icon: ImageView? = null
    private var btn_dynamic_charger_label: TextView? = null
    private var card_chg_max_val0: GridLayout? = null
    private var card_chg_max_val1: GridLayout? = null
    private var txt_cg_p0: TextView? = null
    private var txt_cg_p1: TextView? = null
    private var txt_cg_p2: TextView? = null
    private var txt_cg_p3: TextView? = null
    private var txt_cg_p5: TextView? = null
    private var txt_cg_p6: TextView? = null
    private var txt_dynamic_chg_label: TextView? = null
    private var txt_chg_mode_desc: TextView? = null

    private var card_info_main_2: RelativeLayout? = null
    private var txt_current_chg_max: TextView? = null

    var cResolver: ContentResolver? = null
    @SuppressLint("SimpleDateFormat")
    var sdf = SimpleDateFormat("HH:mm")
    var currentDate = sdf.format(Date())
    private var batteryManager: BatteryManager? = null

    var onBgColor = Color.WHITE
    var bgColor = context.getColor(R.color.toolbox_default_bg)
    var cardColor = context.getColor(R.color.color_game_mode_fg)
    var btnColor = context.getColor(R.color.color_game_mode_on)
    var seekColor = context.getColor(R.color.color_game_popup)

    // Background Task Management
    private val backgroundTaskManager = BackgroundTaskManager()
    private var systemInfoJob: Job? = null
    private var fpsUpdateJob: Job? = null

    // Data classes for better organization
    private data class SystemInfo(
        val currentTime: String,
        val batteryPercent: Int,
        val temperature: Int,
        val cpuLoad: Int,
        val gpuLoad: String,
        val ramLoad: Int,
        val batteryCurrent: Int,
        val isCharging: Boolean,
        val cores: List<CpuCoreInfo>
    )

    private data class FpsData(
        val currentFps: Float,
        val minFps: Float,
        val maxFps: Float,
        val avgFps: Float,
        val hasValidData: Boolean
    )

    @SuppressLint("RtlHardcoded")
    fun open() {
        if (FPSMonitor.show == true || FPSActive) {
            FPSMonitor(mContext).hidePopupWindow()
        }
        GameActive = true
        if (isShown!!) {
            return
        }
        isShown = true
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (batteryManager == null) {
            batteryManager = mContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }
        this.mView = setUpView(mContext)
        val params = LayoutParams()
        if (Settings.canDrawOverlays(this.mContext)) {
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }
        val flags = LayoutParams.FLAG_ALT_FOCUSABLE_IM
        flags.and(LayoutParams.FLAG_FULLSCREEN)
        params.flags = flags
        params.format = PixelFormat.TRANSLUCENT
        params.flags = LayoutParams.FLAG_BLUR_BEHIND
        params.width = LayoutParams.MATCH_PARENT
        params.height = LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.y = dpToPx(mContext, 30f)
        params.x = dpToPx(mContext, 30f)
        params.windowAnimations = R.style.windowAnim
        mWindowManager!!.addView(mView, params)
    }

    fun close() {
        stopSystemInfoUpdates()
        stopFpsUpdates()
        backgroundTaskManager.cancelAll()

        if (isShown!! && null != this.mView) {
            mWindowManager!!.removeView(mView)
            isShown = false
            GameActive = false
        }
        home_fps_chat!!.saveState(mContext, onFocusedApp)
        if (prefs.getBoolean(SpfConfig.MONITOR_MINI, false) || FPSActive)
            FPSMonitor(mContext).showPopupWindow(prefs)
    }

    fun bgShape(context: Context, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12.0f.dpToPx(context)
            setColor(color)
        }
    }

    fun infoShape(context: Context, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6.0f.dpToPx(context)
            setColor(color)
        }
    }

    fun cardShape(context: Context, color: Int, stroke: Int): GradientDrawable {
        val borderWidth = 0.8f.dpToPx(context)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(borderWidth.toInt(), stroke)
            setColor(color)
            cornerRadius = 6.0f.dpToPx(context)
        }
    }

    fun ovalShape(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    fun setComposeColors(context: Context){
        popup_game_control!!.background = bgShape(context, bgColor)
        gamer_header!!.setTextColor(onBgColor)
        header_icon!!.backgroundTintList = ColorStateList.valueOf(onBgColor)
        clock_icon!!.setColorFilter(onBgColor, PorterDuff.Mode.SRC_IN)
        clock_now!!.setTextColor(onBgColor)
        temp_now!!.setTextColor(onBgColor)
        temp_now_icon!!.backgroundTintList = ColorStateList.valueOf(onBgColor)
        battery_now!!.setTextColor(onBgColor)
        battery_icon!!.setColorFilter(onBgColor, PorterDuff.Mode.SRC_IN)
        cpu_core_label!!.setTextColor(onBgColor)
        cpu_core_total_load!!.setTextColor(onBgColor)
        gpu_label!!.setTextColor(onBgColor)
        gpu_load_info!!.setTextColor(onBgColor)
        ram_label!!.setTextColor(onBgColor)
        ram_load_info!!.setTextColor(onBgColor)
        batt_label!!.setTextColor(onBgColor)
        batt_disc_info!!.setTextColor(onBgColor)
        home_open_volume!!.background = ovalShape(onBgColor)
        home_open_volume!!.setColorFilter(cardColor)
        home_memory_clear!!.background = ovalShape(onBgColor)
        home_memory_clear!!.setColorFilter(cardColor)
        seek_brightness_icon!!.background = ovalShape(onBgColor)
        seek_brightness_icon!!.setColorFilter(cardColor)
        card_info_bg!!.background = infoShape(context, cardColor)

        home_fps_chat!!.post {
            home_fps_chat!!.setColors(btnColor)
            home_fps_chat!!.restoreState(context, onFocusedApp)
        }
        fps_value!!.setTextColor(onBgColor)
        fps_label!!.setTextColor(onBgColor)
        fps_value_min!!.setTextColor(onBgColor)
        fps_label_min!!.setTextColor(onBgColor)
        fps_value_max!!.setTextColor(onBgColor)
        fps_label_max!!.setTextColor(onBgColor)
        fps_value_avg!!.setTextColor(onBgColor)
        fps_label_avg!!.setTextColor(onBgColor)

        perf_heat_label!!.setTextColor(onBgColor)
        perf_battery_label!!.setTextColor(onBgColor)
        perf_touch_label!!.setTextColor(onBgColor)
        perf_smooth_label!!.setTextColor(onBgColor)
        perf_quality_label!!.setTextColor(onBgColor)

        progress_left_gradient?.setColors(bgColor, btnColor)
        progress_right_gradient?.setMaskResource(R.drawable.img_perf_settings_perf_scale_progress_right)
        progress_right_gradient?.setColors(bgColor, btnColor)
        graph_perf_low?.setMaskResource(R.drawable.low)
        graph_perf_low?.setGradientHorizontal(true)
        graph_perf_low?.setColors(btnColor, bgColor)
        graph_perf_bal?.setMaskResource(R.drawable.img_perf_settings_perf_radar_triangle)
        graph_perf_bal?.setGradientHorizontal(true)
        graph_perf_bal?.setColors(btnColor, bgColor)
        graph_perf_pro?.setMaskResource(R.drawable.bal)
        graph_perf_pro?.setGradientHorizontal(true)
        graph_perf_pro?.setColors(btnColor, bgColor)

        seekBarColors(seek_brightness!!, btnColor, seekColor, onBgColor)

        card_info_sound_label!!.setTextColor(onBgColor)
        popup_seek_microfon_label!!.setTextColor(onBgColor)
        popup_seek_earfon_label!!.setTextColor(onBgColor)
        popup_txt_mikrofon!!.setTextColor(onBgColor)
        popup_txt_earfon!!.setTextColor(onBgColor)
        popup_judul_headfon!!.setTextColor(onBgColor)
        popup_txt_headfon!!.setTextColor(onBgColor)
        popup_judul_headfon2!!.setTextColor(onBgColor)
        popup_txt_headfon2!!.setTextColor(onBgColor)
        btn_reset_sc!!.background = ovalShape(onBgColor)
        btn_reset_sc_icon!!.backgroundTintList = ColorStateList.valueOf(cardColor)
        seekBarColors(popup_seek_microfon!!, btnColor, seekColor, onBgColor)
        seekBarColors(popup_seek_earfon!!, btnColor, seekColor, onBgColor)
        seekBarColors(popup_seek_headfon!!, btnColor, seekColor, onBgColor)
        seekBarColors(popup_seek_headfon2!!, btnColor, seekColor, onBgColor)
        switchColors(popup_switch_headfon!!, btnColor,seekColor,
            bgColor, bgColor, onBgColor)

        txt_current_chg!!.setTextColor(onBgColor)
        charger_max_label!!.setTextColor(onBgColor)
        txt_watt!!.setTextColor(onBgColor)
        txt_dynamic_chg_label!!.setTextColor(onBgColor)
        txt_chg_mode_desc!!.setTextColor(onBgColor)
    }

    fun setCardBtn(
        context: Context,
        container: LinearLayout,
        iconView: ImageView,
        labelView: TextView,
        isActive: Boolean
    ) {
        if (isActive) {
            container.background = cardShape(context, btnColor, btnColor)
            iconView.backgroundTintList = ColorStateList.valueOf(cardColor)
            labelView.setTextColor(cardColor)
        } else {
            container.background = cardShape(context, cardColor, onBgColor)
            iconView.backgroundTintList = ColorStateList.valueOf(onBgColor)
            labelView.setTextColor(onBgColor)
        }
    }

    fun seekBarColors(
        seekBar: SeekBar,
        progressColor: Int,
        backgroundColor: Int,
        borderColor: Int
    ) {
        val density = seekBar.context.resources.displayMetrics.density
        val borderWidthPx = (2 * density).toInt()
        val cornerRadiusPx = (8 * density).toInt()
        val trackDrawable = createTrackDrawable(backgroundColor, borderColor, borderWidthPx, cornerRadiusPx)
        val progressDrawable = createProgressDrawable(progressColor, cornerRadiusPx - borderWidthPx)
        val clipDrawable = ClipDrawable(progressDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
        val layers = arrayOf<Drawable>(
            trackDrawable,
            clipDrawable
        )
        val layerDrawable = LayerDrawable(layers)

        layerDrawable.setId(0, android.R.id.background)
        layerDrawable.setId(1, android.R.id.progress)
        seekBar.progressDrawable = layerDrawable
        seekBar.thumb = null
    }

    private fun createTrackDrawable(
        backgroundColor: Int,
        borderColor: Int,
        borderWidth: Int,
        cornerRadius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            setStroke(borderWidth, borderColor)
            this.cornerRadius = cornerRadius.toFloat()
        }
    }

    private fun createProgressDrawable(
        progressColor: Int,
        cornerRadius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(progressColor)
            this.cornerRadius = cornerRadius.toFloat()
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchColors(
        switch: Switch,
        thumbColorOn: Int,
        thumbColorOff: Int,
        trackColorOn: Int,
        trackColorOff: Int,
        textColor: Int? = null
    ) {
        switch.thumbTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(thumbColorOn, thumbColorOff)
        )
        switch.trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(trackColorOn, trackColorOff)
        )
        textColor?.let {
            switch.setTextColor(it)
        }
    }

    fun perfButton(context: Context, button: Button, isActive: Boolean) {
        val bgCol = if (isActive) btnColor else cardColor
        val bgColStroke = if (isActive) btnColor else onBgColor
        val iconCol = if (isActive) cardColor else onBgColor
        button.background = cardShape(context, bgCol, bgColStroke)

        val drawables = button.compoundDrawables
        for (drawable in drawables) {
            drawable?.setTint(iconCol)
        }

        button.setTextColor(iconCol)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("ResourceAsColor", "SetTextI18n", "ResourceType", "InflateParams")
    private fun setUpView(context: Context): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_game_control, null)

        // Initialize all views
        initializeViews(view)

        globalSPF = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        prefs = context.getSharedPreferences(onFocusedApp.ifEmpty { SpfConfig.SETTINGS }, Context.MODE_PRIVATE)

        onBgColor = globalSPF.getInt(SpfColors.COLOR_ON_BACKGROUND, Color.WHITE)
        bgColor = globalSPF.getInt(SpfColors.COLOR_SURFACE_VARIANT, context.getColor(R.color.toolbox_default_bg))
        cardColor = globalSPF.getInt(SpfColors.COLOR_SURFACE_CONTAINER, context.getColor(R.color.color_game_mode_fg))
        btnColor = globalSPF.getInt(SpfColors.COLOR_PRIMARY, context.getColor(R.color.color_game_mode_on))
        seekColor = globalSPF.getInt(SpfColors.COLOR_SECONDARY, context.getColor(R.color.color_game_popup))

        card_info_main_2!!.visibility = View.VISIBLE
        hLow = 69f
        hBal = 49f
        hPro = 35f

        hRLow = 35f
        hRBal = 40f
        hRPro = 69f

        cResolver = context.contentResolver
        setComposeColors(context)
        clock_now!!.text = currentDate
        battery_now!!.text = "${batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0}%"
        temp_now!!.text = GlobalStatus.updateBatteryTemperature().toString() + "°C"

        setupButtonListeners(context)
        setupPerformanceControls(context)
        setupSoundControls(context)
        setupChargerControls(context)
        setupBrightness(context)

        setDialogState(view)

        // Start background tasks
        startSystemInfoUpdates()
        startFpsUpdates()

        return view
    }

    @SuppressLint("CutPasteId")
    private fun initializeViews(view: View) {
        gamer_header = view.findViewById(R.id.gamer_header)
        header_icon = view.findViewById(R.id.header_icon)
        btn_touchimprove = view.findViewById(R.id.btn_touchimprove)
        btn_touchimprove_icon = view.findViewById(R.id.btn_touchimprove_icon)
        btn_touchimprove_label = view.findViewById(R.id.btn_touchimprove_label)
        btn_perfasgame = view.findViewById(R.id.btn_perfasgame)
        btn_perfasgame_icon = view.findViewById(R.id.btn_perfasgame_icon)
        btn_perfasgame_label = view.findViewById(R.id.btn_perfasgame_label)
        btn_sound_control = view.findViewById(R.id.btn_sound_control)
        clock_icon = view.findViewById(R.id.clock_icon)
        clock_now = view.findViewById(R.id.clock_now)
        battery_now = view.findViewById(R.id.battery_now)
        battery_icon = view.findViewById(R.id.battery_icon)
        temp_now = view.findViewById(R.id.temp_now)
        temp_now_icon = view.findViewById(R.id.temp_now_icon)
        btn_sound_control_icon = view.findViewById(R.id.btn_sound_control_icon)
        btn_sound_control_label = view.findViewById(R.id.btn_sound_control_label)
        content_tool = view.findViewById(R.id.content_tool)
        card_chg_max = view.findViewById(R.id.card_chg_max)
        btn_fpsmeter = view.findViewById(R.id.btn_fpsmeter)
        btn_fpsmeter_icon = view.findViewById(R.id.btn_fpsmeter_icon)
        btn_fpsmeter_label = view.findViewById(R.id.btn_fpsmeter_label)
        btn_bypass = view.findViewById(R.id.btn_bypass)
        btn_bypass_icon = view.findViewById(R.id.btn_bypass_icon)
        btn_bypass_label = view.findViewById(R.id.btn_bypass_label)
        card_info_bg = view.findViewById(R.id.card_info_bg)
        popup_game_control = view.findViewById(R.id.popup_game_control)
        home_fps_chat = view.findViewById(R.id.home_fps_chat)
        fps_value = view.findViewById(R.id.fps_value)
        fps_label = view.findViewById(R.id.fps_label)
        card_fps_calc = view.findViewById(R.id.card_fps_calc)
        fps_value_min = view.findViewById(R.id.fps_value_min)
        fps_label_min = view.findViewById(R.id.fps_label_min)
        fps_value_max = view.findViewById(R.id.fps_value_max)
        fps_label_max = view.findViewById(R.id.fps_label_max)
        fps_value_avg = view.findViewById(R.id.fps_value_avg)
        fps_label_avg = view.findViewById(R.id.fps_label_avg)
        seek_brightness_icon = view.findViewById(R.id.seek_brightness_icon)
        seek_brightness = view.findViewById(R.id.seek_brightness)
        txt_brightness = view.findViewById(R.id.txt_brightness)

        card_game_perf = view.findViewById(R.id.card_game_perf)
        card_info_main = view.findViewById(R.id.card_info_main)
        perf_lower = view.findViewById(R.id.perf_lower)
        perf_balance = view.findViewById(R.id.perf_balance)
        perf_pro_game = view.findViewById(R.id.perf_pro_game)
        perf_anim_bg = view.findViewById(R.id.perf_anim_bg)
        progress_left_gradient = view.findViewById(R.id.progress_left_gradient)
        progress_right_gradient = view.findViewById(R.id.progress_right_gradient)
        perf_progress_left = view.findViewById(R.id.perf_progress_left)
        perf_progress_right = view.findViewById(R.id.perf_progress_right)
        graph_perf_low = view.findViewById(R.id.graph_perf_low)
        graph_perf_bal = view.findViewById(R.id.graph_perf_bal)
        graph_perf_pro = view.findViewById(R.id.graph_perf_pro)
        perf_heat_label = view.findViewById(R.id.perf_heat_label)
        perf_battery_label = view.findViewById(R.id.perf_battery_label)
        perf_touch_label = view.findViewById(R.id.perf_touch_label)
        perf_smooth_label = view.findViewById(R.id.perf_smooth_label)
        perf_quality_label = view.findViewById(R.id.perf_quality_label)

        cpu_core_label = view.findViewById(R.id.cpu_core_label)
        cpu_core_total_load = view.findViewById(R.id.cpu_core_total_load)
        gpu_label = view.findViewById(R.id.gpu_label)
        gpu_load_info = view.findViewById(R.id.gpu_load_info)
        ram_label = view.findViewById(R.id.ram_label)
        ram_load_info = view.findViewById(R.id.ram_load_info)
        home_open_volume = view.findViewById(R.id.home_open_volume)
        home_memory_clear = view.findViewById(R.id.home_memory_clear)
        batt_label = view.findViewById(R.id.batt_label)
        batt_disc_info = view.findViewById(R.id.batt_disc_info)

        card_info_sound = view.findViewById(R.id.card_info_sound)
        card_info_sound_label = view.findViewById(R.id.card_info_sound_label)
        popup_seek_microfon = view.findViewById(R.id.popup_seek_microfon)
        popup_seek_microfon_label = view.findViewById(R.id.popup_seek_microfon_label)
        popup_txt_mikrofon = view.findViewById(R.id.popup_txt_mikrofon)
        popup_seek_earfon = view.findViewById(R.id.popup_seek_earfon)
        popup_seek_earfon_label = view.findViewById(R.id.popup_seek_earfon_label)
        popup_txt_earfon = view.findViewById(R.id.popup_txt_earfon)
        popup_switch_headfon = view.findViewById(R.id.popup_switch_headfon)

        popup_seek_headfon = view.findViewById(R.id.popup_seek_headfon)
        popup_txt_headfon = view.findViewById(R.id.popup_txt_headfon)
        popup_judul_headfon = view.findViewById(R.id.popup_judul_headfon)
        popup_judul_headfon2 = view.findViewById(R.id.popup_judul_headfon2)
        popup_card_seek_headfon = view.findViewById(R.id.popup_card_seek_headfon)
        popup_seek_headfon2 = view.findViewById(R.id.popup_seek_headfon2)
        popup_txt_headfon2 = view.findViewById(R.id.popup_txt_headfon2)
        btn_reset_sc = view.findViewById(R.id.btn_reset_sc)
        btn_reset_sc_icon = view.findViewById(R.id.btn_reset_sc_icon)
        btn_charger_max = view.findViewById(R.id.btn_charger_max)
        btn_charger_max_icon = view.findViewById(R.id.btn_charger_max_icon)

        card_charger_max = view.findViewById(R.id.card_charger_max)
        txt_current_chg = view.findViewById(R.id.txt_current_chg)
        btn_dynamic_charger = view.findViewById(R.id.btn_dynamic_charger)
        charger_max_label = view.findViewById(R.id.charger_max_label)
        txt_watt = view.findViewById(R.id.txt_watt)
        btn_dynamic_charger_icon = view.findViewById(R.id.btn_dynamic_charger_icon)
        btn_dynamic_charger_label = view.findViewById(R.id.btn_dynamic_charger_label)
        card_chg_max_val0 = view.findViewById(R.id.card_chg_max_val0)
        card_chg_max_val1 = view.findViewById(R.id.card_chg_max_val1)
        txt_cg_p0 = view.findViewById(R.id.btn_chg0)
        txt_cg_p1 = view.findViewById(R.id.btn_chg1)
        txt_cg_p2 = view.findViewById(R.id.btn_chg2)
        txt_cg_p3 = view.findViewById(R.id.btn_chg3)
        txt_cg_p5 = view.findViewById(R.id.btn_chg5)
        txt_cg_p6 = view.findViewById(R.id.btn_chg6)
        txt_dynamic_chg_label = view.findViewById(R.id.txt_dynamic_chg_label)
        txt_chg_mode_desc = view.findViewById(R.id.txt_chg_mode_desc)

        card_info_main_2 = view.findViewById(R.id.card_info_main_2)
        txt_current_chg_max = view.findViewById(R.id.txt_current_chg_max)
    }

    private fun setupButtonListeners(context: Context) {
        setCardBtn(context, btn_touchimprove!!, btn_touchimprove_icon!!, btn_touchimprove_label!!,
            globalSPF.getBoolean(SpfConfig.TOUCH_SAMPLE, false))
        btn_touchimprove!!.setOnClickListener {
            hapticFeedback.run()
            val touchVal = globalSPF.getBoolean(SpfConfig.TOUCH_SAMPLE, false)
            val vSet = if (touchVal) 0 else 1
            setCardBtn(context, btn_touchimprove!!, btn_touchimprove_icon!!, btn_touchimprove_label!!,
                !touchVal)
            globalSPF.edit { putBoolean(SpfConfig.TOUCH_SAMPLE, !touchVal) }
            if (hasModule(Utils.TOUCH_SAMPLE))
                setKernel("$vSet", Utils.TOUCH_SAMPLE)
            if (hasModule(Utils.TOUCH_SAMPLE_GOODIX))
                setKernel("$vSet", Utils.TOUCH_SAMPLE_GOODIX)
        }

        setCardBtn(context, btn_fpsmeter!!, btn_fpsmeter_icon!!, btn_fpsmeter_label!!,
            prefs.getBoolean(SpfConfig.MONITOR_MINI, false) || FPSActive)
        btn_fpsmeter!!.setOnClickListener {
            hapticFeedback.run()
            FPSActive = !(prefs.getBoolean(SpfConfig.MONITOR_MINI, false) || FPSActive)
            setCardBtn(context, btn_fpsmeter!!, btn_fpsmeter_icon!!, btn_fpsmeter_label!!,
                FPSActive)
            prefs.edit { putBoolean(SpfConfig.MONITOR_MINI, FPSActive) }
        }

        var bypassVal = prefs.getBoolean(SpfConfig.BYPASS_CHARGING, false)
        setCardBtn(context, btn_bypass!!, btn_bypass_icon!!, btn_bypass_label!!,bypassVal)
        btn_bypass!!.setOnClickListener {
            hapticFeedback.run()
            bypassVal = !bypassVal
            setCardBtn(context, btn_bypass!!, btn_bypass_icon!!, btn_bypass_label!!,
                bypassVal)
            prefs.edit { putBoolean(SpfConfig.BYPASS_CHARGING, bypassVal) }
        }

        home_open_volume!!.setOnClickListener {
            hapticFeedback.run()
            openVolumeSettings(context)
        }

        home_memory_clear!!.setOnClickListener {
            hapticFeedback.run()
            cleanupMemory()
        }
    }

    private fun openVolumeSettings(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun setupPerformanceControls(context: Context) {
        if (globalSPF.getBoolean(SpfConfig.GAME_TOOL_MODE, false)) {
            card_game_perf!!.visibility = View.VISIBLE
            card_info_main!!.visibility = View.GONE
            card_info_sound!!.visibility = View.GONE
            card_charger_max!!.visibility = View.GONE
        } else {
            card_game_perf!!.visibility = View.GONE
            card_info_main!!.visibility = View.VISIBLE
            card_info_sound!!.visibility = View.GONE
            card_charger_max!!.visibility = View.GONE
        }

        setCardBtn(context, btn_perfasgame!!, btn_perfasgame_icon!!, btn_perfasgame_label!!,
            globalSPF.getBoolean(SpfConfig.GAME_TOOL_MODE, false))
        btn_perfasgame!!.setOnClickListener {
            hapticFeedback.run()
            val perfVal = globalSPF.getBoolean(SpfConfig.GAME_TOOL_MODE, false)
            if (perfVal) {
                card_game_perf!!.visibility = View.GONE
                card_info_main!!.visibility = View.VISIBLE
                card_info_sound!!.visibility = View.GONE
                card_charger_max!!.visibility = View.GONE
            } else {
                card_game_perf!!.visibility = View.VISIBLE
                card_info_main!!.visibility = View.GONE
                card_info_sound!!.visibility = View.GONE
                card_charger_max!!.visibility = View.GONE
                perfAnim(context)
            }
            setCardBtn(context, btn_sound_control!!, btn_sound_control_icon!!, btn_sound_control_label!!,
                false)
            setCardBtn(context, btn_charger_max!!, btn_charger_max_icon!!, txt_chg_mode_desc!!,
                false)
            setCardBtn(context, btn_perfasgame!!, btn_perfasgame_icon!!, btn_perfasgame_label!!,
                !perfVal)
            globalSPF.edit { putBoolean(SpfConfig.GAME_TOOL_MODE, !perfVal) }
        }

        perfAnim(context)
        perf_anim_bg!!.playAnimation()

        perf_lower!!.setOnClickListener {
            hapticFeedback.run()
            perf_anim_bg!!.playAnimation()
            perfButton(context, perf_lower!!, true)
            perfButton(context, perf_balance!!, false)
            perfButton(context, perf_pro_game!!, false)
            Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hLow!!))
            Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRLow!!))

            graph_perf_pro!!.visibility = View.GONE
            graph_perf_bal!!.visibility = View.GONE
            graph_perf_low!!.alpha = 0.1f
            graph_perf_low!!.visibility = View.VISIBLE
            graph_perf_low!!.animate()
                .alpha(0.9f)
                .duration = 500
            prefMode(context, 0, prefs, globalSPF)
        }

        perf_balance!!.setOnClickListener {
            hapticFeedback.run()
            perf_anim_bg!!.playAnimation()
            perfButton(context, perf_lower!!, false)
            perfButton(context, perf_balance!!, true)
            perfButton(context, perf_pro_game!!, false)
            Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hBal!!))
            Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRBal!!))

            graph_perf_pro!!.visibility = View.GONE
            graph_perf_low!!.visibility = View.GONE
            graph_perf_bal!!.alpha = 0.1f
            graph_perf_bal!!.visibility = View.VISIBLE
            graph_perf_bal!!.animate()
                .alpha(0.9f)
                .duration = 500
            prefMode(context, 1, prefs, globalSPF)
        }

        perf_pro_game!!.setOnClickListener {
            hapticFeedback.run()
            perf_anim_bg!!.playAnimation()
            perfButton(context, perf_lower!!, false)
            perfButton(context, perf_balance!!, false)
            perfButton(context, perf_pro_game!!, true)
            Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hPro!!))
            Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRPro!!))
            graph_perf_bal!!.visibility = View.GONE
            graph_perf_low!!.visibility = View.GONE
            graph_perf_pro!!.alpha = 0.1f
            graph_perf_pro!!.visibility = View.VISIBLE
            graph_perf_pro!!.animate()
                .alpha(0.9f)
                .duration = 500
            prefMode(context, 2, prefs, globalSPF)
        }
    }

    private fun setBrightness(mode: Boolean) {
        if (mode) {
            seek_brightness!!.isEnabled = false
            seek_brightness!!.alpha = 0.5f
            seek_brightness_icon!!.setImageResource(R.drawable.toolbox_brightness_mode_auto)
        } else {
            seek_brightness!!.isEnabled = true
            seek_brightness!!.alpha = 1.0f
            seek_brightness_icon!!.setImageResource(R.drawable.toolbox_brightness_mode_manual)
        }
        seek_brightness_icon!!.setColorFilter(cardColor)
    }

    private fun setupBrightness(context: Context) {
        systemController = SystemController(context)
        systemController.setupBrightnessSeekBar(seek_brightness!!)
        seek_brightness!!.progress = prefs.getInt("brightness", systemController.getCurrentBrightness())
        var autoBright = prefs.getBoolean("auto_brightness", systemController.isAutoBrightnessEnabled())
        setBrightness(autoBright)
        seek_brightness_icon!!.setOnClickListener {
            hapticFeedback.run()
            autoBright = !autoBright
            setBrightness(autoBright)
            prefs.edit { putBoolean("auto_brightness", autoBright) }
            systemController.setAutoBrightness(autoBright)
            if (!autoBright)
                systemController.setBrightness(seek_brightness!!.progress)
        }
        txt_brightness!!.text = "${seek_brightness!!.progress}"
        seek_brightness!!.setOnSeekBarChangeListener(OnSeekBarChangeListenerBright({
            prefs.edit { putInt("brightness", seek_brightness!!.progress) }
            systemController.setBrightness(seek_brightness!!.progress)
        }, txt_brightness!!))
    }

    private fun setupSoundControls(context: Context) {
        btn_sound_control!!.isEnabled = hasModule(SOUND_CONTROL)
        setCardBtn(context, btn_sound_control!!, btn_sound_control_icon!!, btn_sound_control_label!!,
            false)
        btn_sound_control!!.setOnClickListener {
            hapticFeedback.run()
            if (card_info_sound!!.isGone) {
                card_info_sound!!.visibility = View.VISIBLE
                card_info_main!!.visibility = View.GONE
                card_game_perf!!.visibility = View.GONE
                card_charger_max!!.visibility = View.GONE
            } else {
                card_info_sound!!.visibility = View.GONE
                card_charger_max!!.visibility = View.GONE
                if (globalSPF.getBoolean(SpfConfig.GAME_TOOL_MODE, false)) {
                    card_info_main!!.visibility = View.GONE
                    card_game_perf!!.visibility = View.VISIBLE
                } else {
                    card_info_main!!.visibility = View.VISIBLE
                    card_game_perf!!.visibility = View.GONE
                }
            }
            setCardBtn(context, btn_charger_max!!, btn_charger_max_icon!!, txt_chg_mode_desc!!,
                false)
            setCardBtn(context, btn_sound_control!!, btn_sound_control_icon!!, btn_sound_control_label!!,
                card_info_sound!!.isVisible)
        }

        // Sound control setup
        popup_seek_headfon!!.progress = (prefs.getFloat(SpfConfig.SC_HEADFON, 0f) * 20).toInt()
        popup_txt_headfon!!.text = "${popup_seek_headfon!!.progress}"
        popup_seek_headfon!!.setOnSeekBarChangeListener(OnSeekBarChangeListener({
            val switchHead = prefs.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false)
            prefs.edit { putFloat(SpfConfig.SC_HEADFON, popup_seek_headfon!!.progress.toFloat() / 20) }
            if (!switchHead) {
                popup_seek_headfon2!!.progress = popup_seek_headfon!!.progress
                popup_txt_headfon2!!.text = popup_txt_headfon!!.text
                prefs.edit { putFloat(SpfConfig.SC_HEADFON2, popup_seek_headfon!!.progress.toFloat() / 20) }
            }
            setKernel("${popup_txt_headfon!!.text} ${
                if (!switchHead)
                    popup_txt_headfon!!.text
                else
                    popup_txt_headfon2!!.text
            }", Utils.HEADPHONE_GAIN)
        }, popup_txt_headfon!!))

        popup_seek_headfon2!!.progress = (prefs.getFloat(SpfConfig.SC_HEADFON2, 0f) * 20).toInt()
        popup_txt_headfon2!!.text = "${popup_seek_headfon2!!.progress}"
        popup_seek_headfon2!!.setOnSeekBarChangeListener(OnSeekBarChangeListener2({
            prefs.edit { putFloat(SpfConfig.SC_HEADFON2, popup_seek_headfon2!!.progress.toFloat() / 20) }
            setKernel("${popup_txt_headfon!!.text} ${popup_txt_headfon2!!.text}", Utils.HEADPHONE_GAIN)
        }, popup_txt_headfon2!!))

        popup_seek_microfon!!.progress = (prefs.getFloat(SpfConfig.SC_MICROFON, 0f) * 20).toInt()
        popup_txt_mikrofon!!.text = "${popup_seek_microfon!!.progress}"
        popup_seek_microfon!!.setOnSeekBarChangeListener(OnSeekBarChangeListenerMic({
            prefs.edit { putFloat(SpfConfig.SC_MICROFON, popup_seek_microfon!!.progress.toFloat() / 20) }
            setKernel("${popup_txt_mikrofon!!.text}", Utils.MICROPHONE_GAIN)
        }, popup_txt_mikrofon!!))

        popup_seek_earfon!!.progress = (prefs.getFloat(SpfConfig.SC_EARFON, 0f) * 20).toInt()
        popup_txt_earfon!!.text = "${popup_seek_earfon!!.progress}"
        popup_seek_earfon!!.setOnSeekBarChangeListener(OnSeekBarChangeListenerEar({
            prefs.edit { putFloat(SpfConfig.SC_EARFON, popup_seek_earfon!!.progress.toFloat() / 20) }
            setKernel("${popup_txt_earfon!!.text}", Utils.EARPIECE_GAIN)
        }, popup_txt_earfon!!))

        setChecked(mContext, prefs.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false))
        popup_switch_headfon!!.run {
            isChecked = prefs.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false)
            setOnClickListener {
                hapticFeedback.run()
                prefs.edit { putBoolean(SpfConfig.SC_SWITCH_HEADFON, isChecked) }
                setChecked(mContext, isChecked)
            }
        }

        btn_reset_sc!!.setOnClickListener{
            hapticFeedback.run()
            resetValueSc()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDynamicCharger(context: Context, mode: Boolean){
        if (mode){
            btn_dynamic_charger!!.background = cardShape(context, btnColor, btnColor)
            btn_dynamic_charger_icon!!.backgroundTintList = ColorStateList.valueOf(cardColor)
            btn_dynamic_charger_label!!.setTextColor(cardColor)
            txt_dynamic_chg_label!!.visibility = View.VISIBLE
            card_chg_max!!.visibility = View.GONE
            txt_chg_mode_desc!!.text = "Dynamic Charger"
            txt_current_chg_max!!.visibility = View.GONE
        } else {
            btn_dynamic_charger!!.background = cardShape(context, cardColor, onBgColor)
            btn_dynamic_charger_icon!!.backgroundTintList = ColorStateList.valueOf(onBgColor)
            btn_dynamic_charger_label!!.setTextColor(onBgColor)
            txt_dynamic_chg_label!!.visibility = View.GONE
            card_chg_max!!.visibility = View.VISIBLE
            txt_chg_mode_desc!!.text = "Charger Max"
            txt_current_chg_max!!.visibility = View.VISIBLE
        }
    }

    private fun setupChargerControls(context: Context) {
        setCardBtn(context, btn_charger_max!!, btn_charger_max_icon!!, txt_chg_mode_desc!!,
            false)
        txt_current_chg_max!!.setTextColor(onBgColor)
        btn_charger_max!!.setOnClickListener {
            hapticFeedback.run()
            if (card_charger_max!!.isGone) {
                card_info_sound!!.visibility = View.GONE
                card_info_main!!.visibility = View.GONE
                card_game_perf!!.visibility = View.GONE
                card_charger_max!!.visibility = View.VISIBLE
                txt_current_chg_max!!.setTextColor(cardColor)
            } else {
                card_info_sound!!.visibility = View.GONE
                card_charger_max!!.visibility = View.GONE
                txt_current_chg_max!!.setTextColor(onBgColor)
                if (globalSPF.getBoolean(SpfConfig.GAME_TOOL_MODE, false)) {
                    card_info_main!!.visibility = View.GONE
                    card_game_perf!!.visibility = View.VISIBLE
                } else {
                    card_info_main!!.visibility = View.VISIBLE
                    card_game_perf!!.visibility = View.GONE
                }
            }
            setCardBtn(context, btn_sound_control!!, btn_sound_control_icon!!, btn_sound_control_label!!,
                false)
            setCardBtn(context, btn_charger_max!!, btn_charger_max_icon!!, txt_chg_mode_desc!!,
                card_charger_max!!.isVisible)
        }

        chgPotition = globalSPF.getInt(SpfConfig.CHG_MAX, 0)
        getChgMax()
        setChgActive(chgPotition, context)
        setTxtChgActive(chgPotition)

        if (hasModule(DYNAMIC_CHARGING)) {
            btn_dynamic_charger!!.visibility = View.VISIBLE
            var dynamicCharger = globalSPF.getBoolean(SpfConfig.DYNAMIC_CHARGING, true)
            setDynamicCharger(context, dynamicCharger)
            btn_dynamic_charger!!.setOnClickListener {
                hapticFeedback.run()
                dynamicCharger = !dynamicCharger
                setDynamicCharger(context, dynamicCharger)
                globalSPF.edit { putBoolean(SpfConfig.DYNAMIC_CHARGING, dynamicCharger) }
                setKernel(if (dynamicCharger) "1" else "0", DYNAMIC_CHARGING)
            }
        } else {
            btn_dynamic_charger!!.visibility = View.GONE
            txt_dynamic_chg_label!!.visibility = View.GONE
        }

        txt_cg_p0!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_0, 0)
        }
        txt_cg_p1!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_1, 1)
        }
        txt_cg_p2!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_2, 2)
        }
        txt_cg_p3!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_3, 3)
        }
        txt_cg_p5!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_5, 0)
        }
        txt_cg_p6!!.setOnClickListener{
            hapticFeedback.run()
            setChgMax(mContext, chgProf_6, 1)
        }
    }

    // Background Task Management
    private fun startSystemInfoUpdates() {
        stopSystemInfoUpdates()

        systemInfoJob = backgroundTaskManager.execute(
            block = {
                while (GameActive) {
                    val systemInfo = collectSystemInfo()
                    withContext(Dispatchers.Main) {
                        updateSystemInfoUI(systemInfo)
                    }
                    delay(1500) // Update every 1.5 seconds
                }
            },
            onError = { _ -> }
        )
    }

    private fun startFpsUpdates() {
        stopFpsUpdates()

        fpsUpdateJob = backgroundTaskManager.execute(
            block = {
                while (GameActive) {
                    val fpsData = collectFpsData()
                    withContext(Dispatchers.Main) {
                        updateFpsUI(fpsData)
                    }
                    delay(1000) // Update every 1000ms for smooth FPS display
                }
            },
            onError = { _ -> }
        )
    }

    private fun stopSystemInfoUpdates() {
        systemInfoJob?.cancel()
        systemInfoJob = null
    }

    private fun stopFpsUpdates() {
        fpsUpdateJob?.cancel()
        fpsUpdateJob = null
    }

    private suspend fun collectSystemInfo(): SystemInfo {
        return withContext(Dispatchers.IO) {
            val cores = ArrayList<CpuCoreInfo>()
            val loads = cpuLoadUtils.cpuLoad
            val gpuLoad = GpuUtils.getGpuLoad()

            val info = ActivityManager.MemoryInfo().apply {
                activityManager!!.getMemoryInfo(this)
            }
            val totalMem = (info.totalMem / 1024 / 1024f).toInt()
            val availMem = (info.availMem / 1024 / 1024f).toInt()
            val ramLoad = ((totalMem - availMem) * 100 / totalMem)

            val now = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val nowMA = now?.let {
                it / globalSPF.getInt(SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT,
                    SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT_DEFAULT)
            }

            // CPU core information
            for (coreIndex in 0 until coreCount) {
                val core = CpuCoreInfo(coreIndex)
                core.currentFreq = cpuFrequencyUtil.getCurrentFrequency("cpu$coreIndex")

                if (!maxFreqList.containsKey(coreIndex) ||
                    (core.currentFreq != "" && maxFreqList[coreIndex].isNullOrEmpty())) {
                    maxFreqList[coreIndex] = cpuFrequencyUtil.getCurrentMaxFrequency("cpu$coreIndex")
                }
                core.maxFreq = maxFreqList[coreIndex]

                if (!minFreqList.containsKey(coreIndex) ||
                    (core.currentFreq != "" && minFreqList[coreIndex].isNullOrEmpty())) {
                    minFreqList[coreIndex] = cpuFrequencyUtil.getCurrentMinFrequency("cpu$coreIndex")
                }
                core.minFreq = minFreqList[coreIndex]

                if (loads.containsKey(core.coreIndex)) {
                    core.loadRatio = loads[core.coreIndex]!!
                }
                cores.add(core)
            }

            SystemInfo(
                currentTime = sdf.format(Date()),
                batteryPercent = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0,
                temperature = GlobalStatus.updateBatteryTemperature().toInt(),
                cpuLoad = loads[-1]?.toInt() ?: 0,
                gpuLoad = "$gpuLoad",
                ramLoad = ramLoad,
                batteryCurrent = nowMA?.toInt() ?: 0,
                isCharging = isCharging(mContext),
                cores = cores
            )
        }
    }

    private suspend fun collectFpsData(): FpsData {
        return withContext(Dispatchers.IO) {
            val currentFps = fpsUtils.currentFps?.toFloatOrNull() ?: 0f
            val fpsList = loadMetricList("fps_values")
            val fpsCalc = calcSummary(fpsList)

            val hasValidData = fpsList.isNotEmpty() &&
                    fpsCalc.first > 0f &&
                    fpsCalc.second > 0f &&
                    fpsCalc.third > 0f &&
                    fpsCalc.first >= fpsCalc.second &&
                    fpsList.size >= 5

            FpsData(
                currentFps = currentFps,
                minFps = fpsCalc.second,
                maxFps = fpsCalc.first,
                avgFps = fpsCalc.third,
                hasValidData = hasValidData
            )
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateSystemInfoUI(info: SystemInfo) {
        clock_now!!.text = info.currentTime
        battery_now!!.text = "${info.batteryPercent}%"
        temp_now!!.text = "${info.temperature}°C"

        cpu_core_total_load!!.text = "${info.cpuLoad}%"
        gpu_load_info!!.text = "${info.gpuLoad}%"
        ram_load_info!!.text = "${info.ramLoad}%"

        val status = getBatteryStatus(mContext)
        val batteryIO = "${info.batteryCurrent}mA"
        batt_disc_info!!.text = batteryIO

        if (status == 2 || status == 5) {
            val watt = (5.0 * info.batteryCurrent) / 1000
            txt_current_chg!!.text = String.format("%.1f", watt)
        } else {
            txt_current_chg!!.text = "~"
        }

        // Close if screen is off or GamePopup is not showing
        if (GamePopup.show == false || !Utils.getScreenState(mContext)) {
            close()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFpsUI(data: FpsData) {
        home_fps_chat!!.setFps(data.currentFps.toString())
        if (data.currentFps == 0f)
            fps_value!!.text = onGameFps
        else
            fps_value!!.text = "${data.currentFps}"
        onGameFps = "${data.currentFps}"

        if (data.hasValidData) {
            if (card_fps_calc!!.isGone) {
                card_fps_calc!!.visibility = View.VISIBLE
            }
            fps_value_min!!.text = "${data.minFps}"
            fps_value_max!!.text = "${data.maxFps}"
            fps_value_avg!!.text = "${data.avgFps}"
        } else if (card_fps_calc!!.isVisible) {
            card_fps_calc!!.visibility = View.GONE
        }
    }

    private fun getBatteryStatus(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    }

    // Existing methods that remain mostly the same
    private val fpsUtils = FpsUtils()
    private var cpuFrequencyUtil = CpuFrequencyUtils()
    private var minFreqList = HashMap<Int, String>()
    private var maxFreqList = HashMap<Int, String>()
    private val coreCount = object : TripleCacheValue(mContext, "CoreCount") {
        override fun initValue(): String {
            return "" + cpuFrequencyUtil.coreCount
        }
    }.toInt()

    private val cpuVili: Boolean = Utils.getBoard().contains("lahaina")

    @OptIn(DelicateCoroutinesApi::class)
    val hapticFeedback = Runnable {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                mView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Exception) {}
        }
    }

    fun perfAnim(context: Context){
        val profileMode = prefs.getInt(SpfConfig.GAME_TOOL_PERF, 1)
        when (profileMode) {
            2 -> {
                Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hPro!!))
                Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRPro!!))
                perfButton(context, perf_lower!!, false)
                perfButton(context, perf_balance!!, false)
                perfButton(context, perf_pro_game!!, true)
                graph_perf_low!!.visibility = View.GONE
                graph_perf_bal!!.visibility = View.GONE
                graph_perf_pro!!.visibility = View.VISIBLE
                graph_perf_pro!!.animate()
                    .alpha(0.9f)
                    .duration = 1000
            }
            0 -> {
                Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hLow!!))
                Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRLow!!))
                perfButton(context, perf_lower!!, true)
                perfButton(context, perf_balance!!, false)
                perfButton(context, perf_pro_game!!, false)
                graph_perf_pro!!.visibility = View.GONE
                graph_perf_bal!!.visibility = View.GONE
                graph_perf_low!!.visibility = View.VISIBLE
                graph_perf_low!!.animate()
                    .alpha(0.9f)
                    .duration = 1000
            }
            else -> {
                Utils.animLayout(perf_progress_left!!, dpToPx(mContext, hBal!!))
                Utils.animLayout(perf_progress_right!!, dpToPx(mContext, hRBal!!))
                perfButton(context, perf_lower!!, false)
                perfButton(context, perf_balance!!, true)
                perfButton(context, perf_pro_game!!, false)
                graph_perf_low!!.visibility = View.GONE
                graph_perf_pro!!.visibility = View.GONE
                graph_perf_bal!!.visibility = View.VISIBLE
                graph_perf_bal!!.animate()
                    .alpha(0.9f)
                    .duration = 1000
            }
        }
    }

    private var chgPotition = 0
    private var chgProf_0 = 0
    private var chgProf_1 = 0
    private var chgProf_2 = 0
    private var chgProf_3 = 0
    private var chgProf_5 = 0
    private var chgProf_6 = 0

    @SuppressLint("SetTextI18n")
    fun getChgMax() {
        val chgProfileName = globalSPF.getString(SpfConfig.CHG_MAX_NAME, "Default")
        var getCurrentMax = globalSPF.getInt(SpfConfig.CHG_CUR_MAX, 0)
        if (chgProfileName!!.contains("Default") || chgProfileName.isEmpty() || getCurrentMax == 0) {
            getCurrentMax = getCurrentCharger()
            globalSPF.edit { putInt(SpfConfig.CHG_CUR_MAX, getCurrentMax) }
        }

        if (cpuVili) {
            chgProf_5 = getCurrentMax
            chgProf_6 = getCurrentMax / 2
            txt_cg_p5!!.text = "${chgProf_5 * 5.5 / 1000000} Watt"
            txt_cg_p6!!.text = "${chgProf_6 * 5.5 / 1000000} Watt"
        } else {
            chgProf_0 = getCurrentMax
            chgProf_3 = getCurrentMax / 4
            chgProf_2 = getCurrentMax / 2
            chgProf_1 = chgProf_2 + chgProf_3
            txt_cg_p0!!.text = "${chgProf_0 * 5.5 / 1000000} Watt"
            txt_cg_p3!!.text = "${chgProf_3 * 5.5 / 1000000} Watt"
            txt_cg_p2!!.text = "${chgProf_2 * 5.5 / 1000000} Watt"
            txt_cg_p1!!.text = "${chgProf_1 * 5.5 / 1000000} Watt"
        }
    }

    fun setChgMax(context: Context, value: Int, position: Int) {
        chgPotition = position
        setChgMax(prefs, value, chgPotition)
        setChgActive(chgPotition, context)
        setTxtChgActive(chgPotition)
    }

    fun setTxtChgActive(value: Int){
        if (cpuVili) {
            if (value == 1) {
                txt_current_chg_max!!.text = txt_cg_p6!!.text
            } else {
                txt_current_chg_max!!.text = txt_cg_p5!!.text
            }
        } else {
            when (value) {
                1 -> {
                    txt_current_chg_max!!.text = txt_cg_p1!!.text
                }
                2 -> {
                    txt_current_chg_max!!.text = txt_cg_p2!!.text
                }
                3 -> {
                    txt_current_chg_max!!.text = txt_cg_p3!!.text
                }
                else -> {
                    txt_current_chg_max!!.text = txt_cg_p0!!.text
                }
            }
        }
    }

    fun setChgActive(value: Int, context: Context){
        when (value) {
            1 -> {
                txt_cg_p0!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p1!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p2!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p3!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p5!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p6!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p0!!.setTextColor(onBgColor)
                txt_cg_p1!!.setTextColor(cardColor)
                txt_cg_p2!!.setTextColor(onBgColor)
                txt_cg_p3!!.setTextColor(onBgColor)
                txt_cg_p5!!.setTextColor(onBgColor)
                txt_cg_p6!!.setTextColor(cardColor)
            }
            2 -> {
                txt_cg_p0!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p1!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p2!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p3!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p5!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p6!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p0!!.setTextColor(onBgColor)
                txt_cg_p1!!.setTextColor(onBgColor)
                txt_cg_p2!!.setTextColor(cardColor)
                txt_cg_p3!!.setTextColor(onBgColor)
                txt_cg_p5!!.setTextColor(onBgColor)
                txt_cg_p6!!.setTextColor(onBgColor)
            }
            3 -> {
                txt_cg_p0!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p1!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p2!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p3!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p5!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p6!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p0!!.setTextColor(onBgColor)
                txt_cg_p1!!.setTextColor(onBgColor)
                txt_cg_p2!!.setTextColor(onBgColor)
                txt_cg_p3!!.setTextColor(cardColor)
                txt_cg_p5!!.setTextColor(onBgColor)
                txt_cg_p6!!.setTextColor(onBgColor)
            }
            else -> {
                txt_cg_p0!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p1!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p2!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p3!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p5!!.background = cardShape(context, btnColor, btnColor)
                txt_cg_p6!!.background = cardShape(context, seekColor, onBgColor)
                txt_cg_p0!!.setTextColor(cardColor)
                txt_cg_p1!!.setTextColor(onBgColor)
                txt_cg_p2!!.setTextColor(onBgColor)
                txt_cg_p3!!.setTextColor(onBgColor)
                txt_cg_p5!!.setTextColor(cardColor)
                txt_cg_p6!!.setTextColor(onBgColor)
            }
        }
    }

    fun loadMetricList(key: String): List<Float> {
        return prefs.getString(key, "")!!
            .split(",")
            .mapNotNull { it.toFloatOrNull() }
    }

    fun calcSummary(list: List<Float>): Triple<Float, Float, Float> {
        if (list.isEmpty()) return Triple(0f, 0f, 0f)

        val max = list.maxOrNull() ?: 0f
        val min = list.minOrNull() ?: 0f
        val avg = list.average().toFloat()
        return Triple(max, min, avg)
    }

    @SuppressLint("SetTextI18n")
    fun setChecked(context: Context, boolean: Boolean) {
        if (boolean) {
            popup_judul_headfon2!!.text = context.getString(R.string.headphone_gain_right)
            popup_judul_headfon!!.text = context.getString(R.string.headphone_gain_left)
            popup_judul_headfon2!!.visibility = View.VISIBLE
            popup_seek_headfon2!!.visibility = View.VISIBLE
            popup_txt_headfon2!!.visibility = View.VISIBLE
        } else {
            popup_judul_headfon!!.text = context.getString(R.string.headphone_gain)
            popup_judul_headfon2!!.visibility = View.GONE
            popup_seek_headfon2!!.visibility = View.GONE
            popup_txt_headfon2!!.visibility = View.GONE
            popup_seek_headfon2!!.progress = popup_seek_headfon!!.progress
            popup_txt_headfon2!!.text = popup_txt_headfon!!.text
            prefs.edit { putFloat(SpfConfig.SC_HEADFON2, popup_seek_headfon2!!.progress.toFloat() / 20) }
            setKernel("${popup_txt_headfon!!.text} ${popup_txt_headfon2!!.text}", Utils.HEADPHONE_GAIN)
        }
    }

    @SuppressLint("SetTextI18n")
    fun resetValueSc(){
        popup_seek_headfon!!.progress = 0
        popup_seek_headfon2!!.progress = 0
        popup_seek_microfon!!.progress = 0
        popup_seek_earfon!!.progress = 0
        popup_txt_headfon!!.text = "0"
        popup_txt_headfon2!!.text = "0"
        popup_txt_earfon!!.text = "0"
        popup_txt_mikrofon!!.text = "0"

        prefs.edit {
            putFloat(SpfConfig.SC_HEADFON, 0f)
                .putFloat(SpfConfig.SC_HEADFON2, 0f)
                .putFloat(SpfConfig.SC_MICROFON, 0f)
                .putFloat(SpfConfig.SC_EARFON, 0f)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun cleanupMemory() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withContext(Dispatchers.Main) {
                    ram_load_info?.text = "Cleaning"
                }
                dropCaches()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    ram_load_info?.text = "Error"
                }
            }
        }
    }

    private suspend fun dropCaches() {
        return withContext(Dispatchers.IO) {
            KeepShellPublic.doCmdSync(
                "sync\n" +
                        "echo 3 > /proc/sys/vm/drop_caches\n" +
                        "echo 1 > /proc/sys/vm/compact_memory"
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setDialogState (view: View) {
        view.setOnTouchListener { _, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()
            val rect = Rect()
            popup_game_control!!.getGlobalVisibleRect(rect)
            if (!rect.contains(x, y)) {
                close()
            }
            false
        }
    }

    // Inner classes for SeekBar listeners
    class OnSeekBarChangeListenerBright(private var next: Runnable, private var txtBright: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            txtBright.text = progress.toString()
        }
    }

    class OnSeekBarChangeListener(private var next: Runnable, private var txtHeadphone: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            txtHeadphone.text = progress.toString()
        }
    }

    class OnSeekBarChangeListener2(private var next: Runnable, private var txtHeadphone2: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            txtHeadphone2.text = progress.toString()
        }
    }

    class OnSeekBarChangeListenerMic(private var next: Runnable, private var txtMicrophone: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            txtMicrophone.text = progress.toString()
        }
    }

    class OnSeekBarChangeListenerEar(private var next: Runnable, private var txtEarphone: TextView) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            next.run()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            txtEarphone.text = progress.toString()
        }
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}
