package com.diphons.dkmsu.ui.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.CpuFrequencyUtils
import com.diphons.dkmsu.ui.component.CpuLoadUtils
import com.diphons.dkmsu.ui.component.FpsUtils
import com.diphons.dkmsu.ui.component.GpuUtils
import com.diphons.dkmsu.ui.data.GlobalStatus
import com.diphons.dkmsu.ui.services.FPSTouchListener
import com.diphons.dkmsu.ui.store.SpfColors
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.FPSActive
import com.diphons.dkmsu.ui.util.GameActive
import com.diphons.dkmsu.ui.util.dpToPx
import com.diphons.dkmsu.ui.util.getBatteryTemperature
import com.diphons.dkmsu.ui.util.onFocusedApp
import com.diphons.dkmsu.ui.util.onGameFps
import com.diphons.dkmsu.ui.util.rootAvailable
import kotlinx.coroutines.*
import kotlin.properties.Delegates

/**
 * @author diphons
 * @date 2024/12/30.
 */
public class FPSMonitor(private val mContext: Context) {

    // View ID constants
    companion object {
        private const val ID_FW_CPU_LOAD = 1
        private const val ID_FW_GPU_LOAD = 2
        private const val ID_FW_BATTERY_TEMP = 3
        private const val ID_FPS_METER = 4
        private const val ID_BG_STAGE_HT = 5
        private const val ID_NORMAL_VIEW_HT = 6
        private const val ID_SMALL_VIEW_HT = 7
        private const val ID_FPS_GAME_INFO_T = 8
        private const val ID_FPS_GAME_INFO = 9
        private const val ID_GPU_GAME_INFO_T = 10
        private const val ID_GPU_GAME_INFO = 11
        private const val ID_CPU_GAME_INFO_T = 12
        private const val ID_CPU_GAME_INFO = 13
        private const val ID_RAM_GAME_INFO_T = 14
        private const val ID_RAM_GAME_INFO = 15
        private const val ID_BAT_PERCENT_INFO_T = 16
        private const val ID_BAT_PERCENT_INFO = 17
        private const val ID_BAT_GAME_INFO_T = 18
        private const val ID_BAT_GAME_INFO = 19
        private const val ID_RAM_INFO = 20
        private const val ID_BATT_INFO = 21
        private const val ID_TEMP_INFO = 22
        private const val ID_FPS_GAME_INFO_T_SMALL = 23
        private const val ID_FPS_GAME_INFO_SMALL = 24
        private const val ID_GPU_GAME_INFO_T_SMALL = 25
        private const val ID_GPU_GAME_INFO_SMALL = 26
        private const val ID_CPU_GAME_INFO_T_SMALL = 27
        private const val ID_CPU_GAME_INFO_SMALL = 28
        private const val ID_RAM_GAME_INFO_T_SMALL = 29
        private const val ID_RAM_GAME_INFO_SMALL = 30
        private const val ID_BAT_PERCENT_INFO_T_SMALL = 31
        private const val ID_BAT_PERCENT_INFO_SMALL = 32
        private const val ID_BAT_GAME_INFO_T_SMALL = 33
        private const val ID_BAT_GAME_INFO_SMALL = 34
        private const val ID_RAM_INFO_SMALL = 35
        private const val ID_BATT_INFO_SMALL = 36
        private const val ID_TEMP_INFO_SMALL = 37

        private var mWindowManager: WindowManager? = null
        public var show: Boolean? = false

        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
    }

    // Background Task Management
    private val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null

    private var startMonitorTime = 0L
    private var cpuLoadUtils = CpuLoadUtils()
    private var cpuFrequencyUtils = CpuFrequencyUtils()
    private var bola: Boolean = false
    private var globalSpf = mContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    private lateinit var prefs: SharedPreferences
    private var getMode by Delegates.notNull<Int>()

    // Data class for system info
    private data class SystemMonitorInfo(
        val cpuLoad: Double,
        val gpuLoad: String,
        val fps: String?,
        val batteryCapacity: Int,
        val batteryTemp: String,
        val batteryCurrent: String?,
        val ramUsage: Int,
        val totalMem: Int,
        val availMem: Int
    )

    fun showPopupWindow(spf: SharedPreferences) {
        FPSActive = onFocusedApp.isNotEmpty()
        if (FPSActive)
            prefs = spf
        showPopupWindow()
    }

    @SuppressLint("SuspiciousIndentation")
    fun showPopupWindow(): Boolean {
        if (show == true || GameActive) {
            return true
        }

        startMonitorTime = System.currentTimeMillis()
        if (batteryManager == null) {
            batteryManager = mContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }

        if (mContext !is AccessibilityService) {
            if (!Settings.canDrawOverlays(mContext)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.fromParts("package", mContext.packageName, null)
                intent.data = uri
                Toast.makeText(mContext, mContext.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
                mContext.startActivity(intent)
                FPSActive = false
                return false
            }
        }

        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        bola = false
        getMode = globalSpf.getInt(SpfConfig.MONITOR_MINI_MODE, 2)

        val view = try {
            when (getMode) {
                2 -> setUpViewOneplus(mContext)
                1 -> setUpView(mContext)
                else -> setUpViewFPS(mContext)
            }
        } catch (_: Exception) {
            setUpViewFPS(mContext)
        }

        val params = LayoutParams().apply {
            if (mContext is AccessibilityService) {
                type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                type = LayoutParams.TYPE_APPLICATION_OVERLAY
            }
            format = PixelFormat.TRANSLUCENT
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val gestureDetector = GestureDetector(view.context, simpleOnGestureListener)
        view.setOnTouchListener(FPSTouchListener(params, mWindowManager, gestureDetector))

        return try {
            mWindowManager!!.addView(view, params)
            mView = view
            show = true
            startMonitorUpdates()
            true
        } catch (ex: Exception) {
            Toast.makeText(mContext, "FPS Monitor Error\n" + ex.message, Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun stopMonitorUpdates() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun startMonitorUpdates() {
        stopMonitorUpdates()

        monitorJob = backgroundScope.launch {
            var pollingPhase = 0

            while (isActive) {
                try {
                    val systemInfo = collectSystemInfo(pollingPhase)
                    withContext(Dispatchers.Main) {
                        updateUI(systemInfo)
                    }

                    pollingPhase = (pollingPhase + 1) % 4
                    delay(1500) // Update every 1.5 seconds
                } catch (_: Exception) {
                    delay(1500)
                }
            }
        }
    }

    private suspend fun collectSystemInfo(pollingPhase: Int): SystemMonitorInfo = withContext(Dispatchers.IO) {
        if (coreCount < 1) {
            coreCount = cpuFrequencyUtils.coreCount
        }
        val gpuLoad = GpuUtils.getGpuLoad()

        activityManager?.getMemoryInfo(ActivityManager.MemoryInfo())

        var cpuLoad = cpuLoadUtils.cpuLoadSum
        val loads = cpuLoadUtils.cpuLoad
        val centerIndex = coreCount / 2
        var bigCoreLoadMax = 0.0
        if (centerIndex >= 2) {
            try {
                for (i in centerIndex until coreCount) {
                    val coreLoad = loads[i] ?: 0.0
                    if (coreLoad > bigCoreLoadMax) {
                        bigCoreLoadMax = coreLoad
                    }
                }
                if (bigCoreLoadMax > 70 && bigCoreLoadMax > cpuLoad) {
                    cpuLoad = bigCoreLoadMax
                }
            } catch (_: java.lang.Exception) { }
        }

        if (cpuLoad < 0) {
            cpuLoad = 0.0
        }

        val fps = fpsUtils.currentFps
        if (fps?.isEmpty() == false)
            onGameFps = fps
        var batState: String? = null

        if (FPSActive) {
            saveFpsHistory(fpsUtils.currentFps?.toFloatOrNull() ?: 0f)
        }

        if (pollingPhase != 0) {
            try {
                val now = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val nowMA = if (now != null) {
                    (now / globalSpf.getInt(SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT, SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT_DEFAULT))
                } else {
                    null
                }
                nowMA?.run {
                    if (this > -20000 && this < 20000) {
                        batState = "" + (if (this > 0) ("+" + this) else this) + "mA"
                    }
                }
            } catch (_: Exception) { }
        }
        if (batState == null) {
            batState = try {
                (if (rootAvailable()) GlobalStatus.updateBatteryTemperature().toString() else getBatteryTemperature(mContext).toString()) + "°C"
            } catch (_: Exception) {
                "N/A°C"
            }
        }

        val info = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(info)
        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()
        val ramUsage = ((totalMem - availMem) * 100 / totalMem)

        var batteryCapacity = 0
        try {
            batteryCapacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        } catch (_: Exception) { }

        SystemMonitorInfo(
            cpuLoad = cpuLoad,
            gpuLoad = if (gpuLoad > -1) "$gpuLoad%" else "0%",
            fps = fps,
            batteryCapacity = batteryCapacity,
            batteryTemp = batState!!,
            batteryCurrent = batState,
            ramUsage = ramUsage,
            totalMem = totalMem,
            availMem = availMem
        )
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(info: SystemMonitorInfo) {
        try {
            cpuLoadTextView?.text = "${info.cpuLoad.toInt()}%"
            gpuLoadTextView?.text = info.gpuLoad
            cpuLoadTextView_small?.text = "${info.cpuLoad.toInt()}%"
            gpuLoadTextView_small?.text = if (info.gpuLoad != "0%") info.gpuLoad.replace("%", "") else "--"

            if (!bola) {
                if (getMode == 2) {
                    val color_profile = globalSpf.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(mContext, R.color.color_ht1))
                    val color_default = globalSpf.getInt(SpfColors.COLOR_ON_BACKGROUND, ContextCompat.getColor(mContext, R.color.white))

                    listOf(fpsText, gpuLoadTextView, cpuLoadTextView, ramText, battpercentTextView, battTextView,
                        fpsText_small, gpuLoadTextView_small, cpuLoadTextView_small, ramText_small,
                        battpercentTextView_small, battTextView_small).forEach { textView ->
                        textView?.setTextColor(color_profile)
                    }

                    val isSmallMode = globalSpf.getBoolean(SpfConfig.MONITOR_MINI_SMALL, false)
                    fps_ht_normal?.visibility = if (isSmallMode) View.GONE else View.VISIBLE
                    fps_ht_small?.visibility = if (isSmallMode) View.VISIBLE else View.GONE

                    val showRam = globalSpf.getBoolean(SpfConfig.MONITOR_MINI_RAM, true)
                    val showBatt = globalSpf.getBoolean(SpfConfig.MONITOR_MINI_BATT, true)
                    val showTemp = globalSpf.getBoolean(SpfConfig.MONITOR_MINI_TEMP, true)

                    ram_ht?.visibility = View.GONE
                    ram_ht_small?.visibility = if (isSmallMode && showRam) View.VISIBLE else View.GONE
                    batt_ht?.visibility = View.GONE
                    batt_ht_small?.visibility = if (isSmallMode && showBatt) View.VISIBLE else View.GONE
                    temp_ht?.visibility = View.GONE
                    temp_ht_small?.visibility = if (isSmallMode && showTemp) View.VISIBLE else View.GONE

                    ramText?.text = "${info.ramUsage}%"
                    ramText_small?.text = "${info.ramUsage}%"

                    val batteryTemp = try {
                        if (rootAvailable()) GlobalStatus.updateBatteryTemperature().toString() else getBatteryTemperature(mContext).toString()
                    } catch (_: Exception) {
                        "N/A"
                    }

                    battpercentTextView?.text = "${GlobalStatus.batteryCapacity}%"
                    battTextView?.text = "$batteryTemp°C"
                    battpercentTextView_small?.text = "${info.batteryCapacity}%"
                    battTextView_small?.text = "$batteryTemp°C"

                    val isExpand = globalSpf.getBoolean(SpfConfig.MONITOR_MINI_EXPAND, false)
                    val titleTexts = if (isExpand) {
                        listOf(" ", "  ·  ", "  ·  ", "  ·  ", "  ·  ", "  ·  ")
                    } else {
                        listOf(" FPS ", "   GPU ", "   CPU ", "  RAM ", "  BAT ", "   T ")
                    }

                    listOf(fpsText_t, gpuText_t, cpuText_t, ramText_t, percText_t, batText_t).forEachIndexed { index, textView ->
                        textView?.text = titleTexts[index]
                        textView?.setTextColor(if (isExpand) color_profile else color_default)
                    }

                    listOf(fpsText_t_small, gpuText_t_small, cpuText_t_small, ramText_t_small, percText_t_small, batText_t_small).forEachIndexed { index, textView ->
                        textView?.text = titleTexts[index]
                        textView?.setTextColor(if (isExpand) color_profile else color_default)
                    }
                }
            }

            temperatureText?.text = info.batteryCurrent
            if (fpsText?.text?.isEmpty() == true)
                fpsText?.text = onGameFps

            if (info.fps != null) {
                when (getMode) {
                    0 -> fpsText?.text = if (bola) info.fps else "fps ${info.fps}"
                    else -> {
                        fpsText?.text = info.fps
                        if (getMode == 2) {
                            fpsText_small?.text = info.fps
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun hidePopupWindow() {
        stopMonitorUpdates()
        backgroundScope.coroutineContext.cancelChildren()

        if (show == true && mView != null) {
            mWindowManager?.removeViewImmediate(mView)
            mView = null
        }
        if (!GameActive)
            FPSActive = false
        show = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpView(context: Context): View {
        val mainLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_monitor_mini)
            setPadding(dpToPx(context, 8), dpToPx(context, 3), dpToPx(context, 8), dpToPx(context, 4))
        }

        fun createMonitorItem(label: String, valueId: Int, valueText: String = "--"): Pair<TextView, TextView> {
            val labelView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                text = label
                setTextColor(0xFFDDDDDD.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                setTypeface(typeface, Typeface.BOLD)
            }

            val valueView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(context, 2)
                    gravity = Gravity.CENTER
                }
                text = valueText
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                minWidth = dpToPx(context, when (label) {
                    "FPS:" -> 22
                    else -> 20
                })
                id = valueId
            }

            return Pair(labelView, valueView)
        }

        val cpuItem = createMonitorItem("CPU:", ID_FW_CPU_LOAD)
        val gpuItem = createMonitorItem("GPU:", ID_FW_GPU_LOAD)
        val fpsItem = createMonitorItem("FPS:", ID_FW_BATTERY_TEMP)

        cpuLoadTextView = cpuItem.second
        gpuLoadTextView = gpuItem.second
        fpsText = fpsItem.second

        temperatureText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(context, 5)
                gravity = Gravity.CENTER
            }
            text = "--"
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            minWidth = dpToPx(context, 34)
            id = ID_FW_BATTERY_TEMP
        }

        mainLayout.addView(cpuItem.first)
        mainLayout.addView(cpuItem.second)
        mainLayout.addView(gpuItem.first)
        mainLayout.addView(gpuItem.second)
        mainLayout.addView(fpsItem.first)
        mainLayout.addView(fpsItem.second)
        mainLayout.addView(temperatureText)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        view = mainLayout
        return mainLayout
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun setUpViewFPS(context: Context): View {
        val mainLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fpsText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "fps"
            setTextColor(globalSpf.getInt(SpfColors.COLOR_PRIMARY, 0xFFFFFFFF.toInt()))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val color = globalSpf.getInt(SpfColors.COLOR_SURFACE_VARIANT, 0xFF191b24.toInt())
            val alpha = 0.75f
            val colorWithAlpha = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(colorWithAlpha)
            }
            val paddingHorizontal = (12 * context.resources.displayMetrics.density).toInt()
            val paddingVertical = (6 * context.resources.displayMetrics.density).toInt()
            setPaddingRelative(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
            id = ID_FPS_METER
        }

        cpuLoadTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            id = ID_FW_CPU_LOAD
        }

        gpuLoadTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            id = ID_FW_GPU_LOAD
        }

        temperatureText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            id = ID_FW_BATTERY_TEMP
        }

        mainLayout.addView(fpsText)
        mainLayout.addView(cpuLoadTextView)
        mainLayout.addView(gpuLoadTextView)
        mainLayout.addView(temperatureText)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        view = mainLayout
        return mainLayout
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpViewOneplus(context: Context): View {
        val mainLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val color = globalSpf.getInt(SpfColors.COLOR_SURFACE_VARIANT, 0xFF191b24.toInt())
            val alpha = 0.75f
            val colorWithAlpha = ColorUtils.setAlphaComponent(color, (255 * alpha).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(colorWithAlpha)
            }
            val paddingHorizontal = (12 * context.resources.displayMetrics.density).toInt()
            setPaddingRelative(paddingHorizontal, 0, paddingHorizontal, 0)
            id = ID_BG_STAGE_HT
        }

        fps_ht_normal = createNormalView(context)
        fps_ht_small = createSmallView(context)

        temperatureText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            id = ID_FW_BATTERY_TEMP
        }

        mainLayout.addView(fps_ht_normal)
        mainLayout.addView(fps_ht_small)
        mainLayout.addView(temperatureText)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        view = mainLayout
        bg_stage_ht = mainLayout

        return mainLayout
    }

    private fun createNormalView(context: Context): LinearLayout {
        val layout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            id = ID_NORMAL_VIEW_HT
        }

        val padding7dp = dpToPx(context, 7)
        val padding0dp = dpToPx(context, 0)

        fun createTextViewPair(
            labelText: String,
            labelId: Int,
            valueId: Int,
            isBold: Boolean = false
        ): Pair<TextView, TextView> {
            val labelView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(padding0dp, padding7dp, padding0dp, padding7dp)
                }
                text = labelText
                setTextColor(0xFFFFFFFF.toInt())
                if (isBold) setTypeface(typeface, Typeface.BOLD)
                id = labelId
            }

            val valueView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(padding0dp, padding7dp, padding0dp, padding7dp)
                }
                text = ""
                setTextColor(ContextCompat.getColor(context, R.color.color_ht))
                id = valueId
            }

            return Pair(labelView, valueView)
        }

        val fpsPair = createTextViewPair(" FPS ", ID_FPS_GAME_INFO_T, ID_FPS_GAME_INFO, true)
        val gpuPair = createTextViewPair("   GPU ", ID_GPU_GAME_INFO_T, ID_GPU_GAME_INFO, true)
        val cpuPair = createTextViewPair("   CPU ", ID_CPU_GAME_INFO_T, ID_CPU_GAME_INFO, true)

        fpsText_t = fpsPair.first
        fpsText = fpsPair.second
        gpuText_t = gpuPair.first
        gpuLoadTextView = gpuPair.second
        cpuText_t = cpuPair.first
        cpuLoadTextView = cpuPair.second

        layout.addView(fpsText_t)
        layout.addView(fpsText)
        layout.addView(gpuText_t)
        layout.addView(gpuLoadTextView)
        layout.addView(cpuText_t)
        layout.addView(cpuLoadTextView)

        ram_ht = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            id = ID_RAM_INFO
        }

        val ramPair = createTextViewPair("  RAM ", ID_RAM_GAME_INFO_T, ID_RAM_GAME_INFO, true)
        ramText_t = ramPair.first
        ramText = ramPair.second
        ram_ht?.addView(ramText_t)
        ram_ht?.addView(ramText)
        layout.addView(ram_ht)

        batt_ht = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            id = ID_BATT_INFO
        }

        val battPair = createTextViewPair("  BAT ", ID_BAT_PERCENT_INFO_T, ID_BAT_PERCENT_INFO, true)
        percText_t = battPair.first
        battpercentTextView = battPair.second
        batt_ht?.addView(percText_t)
        batt_ht?.addView(battpercentTextView)
        layout.addView(batt_ht)

        temp_ht = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            id = ID_TEMP_INFO
        }

        val tempPair = createTextViewPair("   T ", ID_BAT_GAME_INFO_T, ID_BAT_GAME_INFO, true)
        batText_t = tempPair.first
        battTextView = tempPair.second
        temp_ht?.addView(batText_t)
        temp_ht?.addView(battTextView)
        layout.addView(temp_ht)

        TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setPadding(padding0dp, padding7dp, padding7dp, padding7dp)
            }
            text = ""
            setTextColor(ContextCompat.getColor(context, R.color.color_ht))
            layout.addView(this)
        }

        return layout
    }

    private fun createSmallView(context: Context): LinearLayout {
        val layout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            id = ID_SMALL_VIEW_HT
        }

        val padding5dp = dpToPx(context, 5)
        val padding0dp = dpToPx(context, 0)

        fun createSmallTextViewPair(
            labelText: String,
            labelId: Int,
            valueId: Int,
            isBold: Boolean = false
        ): Pair<TextView, TextView> {
            val labelView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(padding0dp, padding5dp, padding0dp, padding5dp)
                }
                text = labelText
                setTextColor(0xFFFFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                if (isBold) setTypeface(typeface, Typeface.BOLD)
                id = labelId
            }

            val valueView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(padding0dp, padding5dp, padding0dp, padding5dp)
                }
                text = ""
                setTextColor(ContextCompat.getColor(context, R.color.color_ht))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                id = valueId
            }

            return Pair(labelView, valueView)
        }

        val fpsPair = createSmallTextViewPair(" FPS ", ID_FPS_GAME_INFO_T_SMALL, ID_FPS_GAME_INFO_SMALL, true)
        val gpuPair = createSmallTextViewPair("   GPU ", ID_GPU_GAME_INFO_T_SMALL, ID_GPU_GAME_INFO_SMALL, true)
        val cpuPair = createSmallTextViewPair("   CPU ", ID_CPU_GAME_INFO_T_SMALL, ID_CPU_GAME_INFO_SMALL, true)

        fpsText_t_small = fpsPair.first
        fpsText_small = fpsPair.second
        gpuText_t_small = gpuPair.first
        gpuLoadTextView_small = gpuPair.second
        cpuText_t_small = cpuPair.first
        cpuLoadTextView_small = cpuPair.second

        layout.addView(fpsText_t_small)
        layout.addView(fpsText_small)
        layout.addView(gpuText_t_small)
        layout.addView(gpuLoadTextView_small)
        layout.addView(cpuText_t_small)
        layout.addView(cpuLoadTextView_small)

        ram_ht_small = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.VISIBLE
            id = ID_RAM_INFO_SMALL
        }

        val ramPair = createSmallTextViewPair("  RAM ", ID_RAM_GAME_INFO_T_SMALL, ID_RAM_GAME_INFO_SMALL, true)
        ramText_t_small = ramPair.first
        ramText_small = ramPair.second
        ram_ht_small?.addView(ramText_t_small)
        ram_ht_small?.addView(ramText_small)
        layout.addView(ram_ht_small)

        batt_ht_small = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.VISIBLE
            id = ID_BATT_INFO_SMALL
        }

        val battPair = createSmallTextViewPair("  BAT ", ID_BAT_PERCENT_INFO_T_SMALL, ID_BAT_PERCENT_INFO_SMALL, true)
        percText_t_small = battPair.first
        battpercentTextView_small = battPair.second.apply {
            setTextColor(0xFFFFFFFF.toInt())
        }
        batt_ht_small?.addView(percText_t_small)
        batt_ht_small?.addView(battpercentTextView_small)
        layout.addView(batt_ht_small)

        temp_ht_small = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            visibility = View.VISIBLE
            id = ID_TEMP_INFO_SMALL
        }

        val tempPair = createSmallTextViewPair("   T ", ID_BAT_GAME_INFO_T_SMALL, ID_BAT_GAME_INFO_SMALL, true)
        batText_t_small = tempPair.first
        battTextView_small = tempPair.second
        temp_ht_small?.addView(batText_t_small)
        temp_ht_small?.addView(battTextView_small)
        layout.addView(temp_ht_small)

        TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setPadding(padding0dp, padding5dp, padding5dp, padding5dp)
            }
            text = ""
            setTextColor(globalSpf.getInt(SpfColors.COLOR_PRIMARY, ContextCompat.getColor(context, R.color.color_ht)))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
            layout.addView(this)
        }

        return layout
    }

    private var simpleOnGestureListener: GestureDetector.SimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return super.onDoubleTap(e)
        }
    }

    private fun saveFpsHistory(fps: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = loadFpsHistory().toMutableList()

                list.add(fps)
                if (list.size > 2000) list.removeAt(0)   // batasi max 2000 bar

                val joined = list.joinToString(",")
                prefs.edit().putString("fps_values", joined).apply()
            } catch (_: Exception) { }
        }
    }

    private fun loadFpsHistory(): List<Float> {
        val str = prefs.getString("fps_values", null) ?: return emptyList()
        return str.split(",").mapNotNull { it.toFloatOrNull() }
    }

    // View references
    private var view: View? = null
    private var cpuLoadTextView: TextView? = null
    private var gpuLoadTextView: TextView? = null
    private var cpuLoadTextView_small: TextView? = null
    private var gpuLoadTextView_small: TextView? = null
    private var temperatureText: TextView? = null
    private var battpercentTextView: TextView? = null
    private var battTextView: TextView? = null
    private var battpercentTextView_small: TextView? = null
    private var battTextView_small: TextView? = null
    private var fpsText: TextView? = null
    private var fpsText_small: TextView? = null
    private var ramText: TextView? = null
    private var ramText_small: TextView? = null
    private var fpsText_t: TextView? = null
    private var batText_t: TextView? = null
    private var percText_t: TextView? = null
    private var fpsText_t_small: TextView? = null
    private var batText_t_small: TextView? = null
    private var percText_t_small: TextView? = null
    private var gpuText_t: TextView? = null
    private var gpuText_t_small: TextView? = null
    private var cpuText_t: TextView? = null
    private var cpuText_t_small: TextView? = null
    private var ramText_t: TextView? = null
    private var ramText_t_small: TextView? = null
    private var fps_ht_normal: LinearLayout? = null
    private var fps_ht_small: LinearLayout? = null
    private var ram_ht: LinearLayout? = null
    private var batt_ht: LinearLayout? = null
    private var temp_ht: LinearLayout? = null
    private var ram_ht_small: LinearLayout? = null
    private var batt_ht_small: LinearLayout? = null
    private var temp_ht_small: LinearLayout? = null
    private var bg_stage_ht: LinearLayout? = null
    private var activityManager: ActivityManager? = null
    private var coreCount = -1

    private val fpsUtils = FpsUtils()
    private var batteryManager: BatteryManager? = null
}
