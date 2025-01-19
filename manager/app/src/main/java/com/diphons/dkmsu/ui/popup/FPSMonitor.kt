package com.diphons.dkmsu.ui.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import androidx.core.content.ContextCompat
import com.diphons.dkmsu.ui.data.GlobalStatus
import com.diphons.dkmsu.ui.component.*
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.services.FPSTouchListener
import java.util.*
import kotlin.properties.Delegates

public class FPSMonitor(private val mContext: Context) {
    private var startMonitorTime = 0L
    private var cpuLoadUtils = CpuLoadUtils()
    private var cpuFrequencyUtils = CpuFrequencyUtils()
    private var bola: Boolean = false
    private val prefs = mContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    private var getMode by Delegates.notNull<Int>()

    /**
     * 显示弹出框
     * @param context
     */
    @SuppressLint("SuspiciousIndentation")
    fun showPopupWindow(): Boolean {
        prefs.edit().putBoolean(SpfConfig.MONITOR_MINI, true).apply()
        if (show!!) {
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
                prefs.edit().putBoolean(SpfConfig.MONITOR_MINI, false).apply()
                return false
            }
        }

        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view: View
        bola = false
        getMode = prefs.getInt(SpfConfig.MONITOR_MINI_MODE, 0)
        if (getMode == 2) {
            view = setUpViewOneplus(mContext)
        } else if (getMode == 0) {
            view = setUpViewFPS(mContext)
        } else {
            view = setUpView(mContext)
        }
        val params = LayoutParams()

        if (mContext is AccessibilityService) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = 0
        params.y = 0

        if (bola || getMode == 1) {
            params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            params.flags = LayoutParams.FLAG_NOT_FOCUSABLE
            val gestureDetector = GestureDetector(view.context, simpleOnGestureListener)
            view.setOnTouchListener(FPSTouchListener(params, mWindowManager, gestureDetector))
        }

        params.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        return try {
            mWindowManager!!.addView(view, params)
            mView = view
            show = true

            startTimer()
            true
        } catch (ex: Exception) {
            Toast.makeText(mContext, "FPS Monitor Error\n" + ex.message, Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private var view: View? = null
    private var cpuLoadTextView: TextView? = null
    private var gpuLoadTextView: TextView? = null
    private var cpuLoadTextView_small: TextView? = null
    private var gpuLoadTextView_small: TextView? = null
    private var gpuPanel: View? = null
    private var temperaturePanel: View? = null
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
    private var myHandler = Handler(Looper.getMainLooper())
    private var myHandler2 = Handler(Looper.getMainLooper())
    private val info = ActivityManager.MemoryInfo()
    private var coreCount = -1
    private var clusters = ArrayList<Array<String>>()

    private val fpsUtils = FpsUtils()
    private var batteryManager: BatteryManager? = null

    private var pollingPhase = 0

    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        pollingPhase += 1
        pollingPhase %= 4

        if (coreCount < 1) {
            coreCount = cpuFrequencyUtils.coreCount
            clusters = cpuFrequencyUtils.clusterInfo
        }
        val gpuLoad = GpuUtils.getGpuLoad()

        activityManager!!.getMemoryInfo(info)

        var cpuLoad = cpuLoadUtils.cpuLoadSum
        val loads = cpuLoadUtils.cpuLoad
        val centerIndex = coreCount / 2
        var bigCoreLoadMax = 0.0
        if (centerIndex >= 2) {
            try {
                for (i in centerIndex until coreCount) {
                    val coreLoad = loads[i]!!
                    if (coreLoad > bigCoreLoadMax) {
                        bigCoreLoadMax = coreLoad
                    }
                }
                if (bigCoreLoadMax > 70 && bigCoreLoadMax > cpuLoad) {
                    cpuLoad = bigCoreLoadMax
                }
            } catch (ex: java.lang.Exception) {
                Log.e("", "" + ex.message)
            }
        }

        if (cpuLoad < 0) {
            cpuLoad = 0.toDouble()
        }

        val fps = fpsUtils.currentFps
        var batState: String? = null

        if (pollingPhase != 0) {
            val now = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val nowMA = if (now != null) {
                (now / prefs.getInt(SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT, SpfConfig.MONITOR_MINI_CURRENT_NOW_UNIT_DEFAULT))
            } else {
                null
            }
            nowMA?.run {
                if (this > -20000 && this < 20000) {
                    batState = "" + (if (this > 0) ("+" + this) else this) + "mA"
                }
            }
        }
        if (batState == null) {
            batState = GlobalStatus.updateBatteryTemperature().toString() + "°C"
        }

        val info = ActivityManager.MemoryInfo().apply {
            activityManager!!.getMemoryInfo(this)
        }
        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()
        var color_profile: Int
        var batteryCapacity: Int

        myHandler2.post {
            cpuLoadTextView?.text = cpuLoad.toInt().toString() + "%"
            if (gpuLoad > -1) {
                gpuLoadTextView?.text = "$gpuLoad %"
            } else {
                gpuLoadTextView?.text = "--"
            }
            batteryCapacity = batteryManager!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            if (!bola) {
                if (getMode == 2) {
                    bg_stage_ht!!.backgroundTintList = mContext.getColorStateList(R.color.wallbgdark)
                    color_profile = ContextCompat.getColor(mContext, R.color.color_ht1)
                    fpsText!!.setTextColor(color_profile)
                    gpuLoadTextView!!.setTextColor(color_profile)
                    cpuLoadTextView!!.setTextColor(color_profile)
                    ramText!!.setTextColor(color_profile)
                    battpercentTextView!!.setTextColor(color_profile)
                    battTextView!!.setTextColor(color_profile)
                    fpsText_small!!.setTextColor(color_profile)
                    gpuLoadTextView_small!!.setTextColor(color_profile)
                    cpuLoadTextView_small!!.setTextColor(color_profile)
                    ramText_small!!.setTextColor(color_profile)
                    battpercentTextView_small!!.setTextColor(color_profile)
                    battTextView_small!!.setTextColor(color_profile)
                    if (prefs.getBoolean(SpfConfig.MONITOR_MINI_SMALL, false)){
                        fps_ht_normal!!.visibility = View.GONE
                        fps_ht_small!!.visibility = View.VISIBLE
                        if (prefs.getBoolean(SpfConfig.MONITOR_MINI_RAM, true)){
                            ram_ht!!.visibility = View.GONE
                            ram_ht_small!!.visibility = View.VISIBLE
                        } else {
                            ram_ht!!.visibility = View.GONE
                            ram_ht_small!!.visibility = View.GONE
                        }
                        if (prefs.getBoolean(SpfConfig.MONITOR_MINI_BATT, true)){
                            batt_ht!!.visibility = View.GONE
                            batt_ht_small!!.visibility = View.VISIBLE
                        } else {
                            batt_ht!!.visibility = View.GONE
                            batt_ht_small!!.visibility = View.GONE
                        }
                        if (prefs.getBoolean(SpfConfig.MONITOR_MINI_TEMP, true)){
                            temp_ht!!.visibility = View.GONE
                            temp_ht_small!!.visibility = View.VISIBLE
                        } else {
                            temp_ht!!.visibility = View.GONE
                            temp_ht_small!!.visibility = View.GONE
                        }
                    } else {
                        fps_ht_normal!!.visibility = View.VISIBLE
                        fps_ht_small!!.visibility = View.GONE
                        ram_ht!!.visibility = View.GONE
                        ram_ht_small!!.visibility = View.GONE
                        batt_ht!!.visibility = View.GONE
                        batt_ht_small!!.visibility = View.GONE
                        temp_ht!!.visibility = View.GONE
                        temp_ht_small!!.visibility = View.GONE
                    }
                    cpuLoadTextView_small?.text = cpuLoad.toInt().toString() + "%"
                    if (gpuLoad > -1) {
                        gpuLoadTextView_small?.text = gpuLoad.toString() + "%"
                    } else {
                        gpuLoadTextView_small?.text = "--"
                    }

                    ramText!!.text = "${((totalMem - availMem) * 100 / totalMem)}%"
                    ramText_small!!.text = "${((totalMem - availMem) * 100 / totalMem)}%"
                    battpercentTextView!!.text = GlobalStatus.batteryCapacity.toString() + "%"
                    battTextView!!.text = GlobalStatus.updateBatteryTemperature().toString() + "°C"
                    battpercentTextView_small!!.text = "$batteryCapacity%"
                    battTextView_small!!.text = GlobalStatus.updateBatteryTemperature().toString() + "°C"
                    if (prefs.getBoolean(SpfConfig.MONITOR_MINI_EXPAND, false)) {
                        //Misc.HTEx = 1
                        fpsText_t!!.text = " "
                        gpuText_t!!.text = "  ·  "
                        cpuText_t!!.text = "  ·  "
                        ramText_t!!.text = "  ·  "
                        percText_t!!.text = "  ·  "
                        batText_t!!.text = "  ·  "
                        fpsText_t_small!!.text = " "
                        gpuText_t_small!!.text = "  ·  "
                        cpuText_t_small!!.text = "  ·  "
                        ramText_t_small!!.text = "  ·  "
                        percText_t_small!!.text = "  ·  "
                        batText_t_small!!.text = "  ·  "
                    } else {
                        //Misc.HTEx = 0
                        fpsText_t!!.text = " FPS "
                        gpuText_t!!.text = "   GPU "
                        cpuText_t!!.text = "   CPU "
                        ramText_t!!.text = "  RAM "
                        percText_t!!.text = "  BAT "
                        batText_t!!.text = "   T "
                        fpsText_t_small!!.text = " FPS "
                        gpuText_t_small!!.text = "   GPU "
                        cpuText_t_small!!.text = "   CPU "
                        ramText_t_small!!.text = "  RAM "
                        percText_t_small!!.text = "  BAT "
                        batText_t_small!!.text = "   T "
                    }
                }
            }
            temperatureText!!.text = batState!!
            if (fps != null) {
                if (getMode == 0) {
                    if (bola) {
                        fpsText?.text = fps.toString()
                    } else {
                        fpsText?.text = "fps $fps"
                    }
                } else {
                    fpsText?.text = fps.toString()
                    if (getMode == 2) {
                        fpsText_small?.text = fps.toString()
                    }
                }
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                myHandler.post {
                    updateInfo()
                }
            }
        }, 0, 1500)
    }

    /**
     * 隐藏弹出框
     */
    fun hidePopupWindow() {
        stopTimer()
        if (show!! && null != mView) {
            try {
                mWindowManager?.removeViewImmediate(mView)
            } catch (_: Exception) {}
            mView = null
        }
       // if (!Utils.GameActive)
        prefs.edit().putBoolean(SpfConfig.MONITOR_MINI, false).apply()
        show = false
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility", "InflateParams")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.monitor_mini, null)
        cpuLoadTextView = view!!.findViewById(R.id.fw_cpu_load)
        gpuLoadTextView = view!!.findViewById(R.id.fw_gpu_load)
        temperatureText = view!!.findViewById(R.id.fw_battery_temp)
        fpsText = view!!.findViewById(R.id.fw_fps)


        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return view!!
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility", "InflateParams")
    private fun setUpViewFPS(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.monitor_fps, null)
        cpuLoadTextView = view!!.findViewById(R.id.fw_cpu_load)
        gpuLoadTextView = view!!.findViewById(R.id.fw_gpu_load)
        temperatureText = view!!.findViewById(R.id.fw_battery_temp)
        fpsText = view!!.findViewById(R.id.fps_meter)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return view!!
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility", "InflateParams")
    private fun setUpViewOneplus(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.monitor_oneplus, null)

        gpuPanel = view!!.findViewById(R.id.fw_gpu_load)

        bg_stage_ht = view!!.findViewById(R.id.bg_stage_ht)
        fps_ht_normal = view!!.findViewById(R.id.normal_view_ht)
        fps_ht_small = view!!.findViewById(R.id.small_view_ht)
        fpsText_t = view!!.findViewById(R.id.fps_game_info_t)
        gpuText_t = view!!.findViewById(R.id.gpu_game_info_t)
        cpuText_t = view!!.findViewById(R.id.cpu_game_info_t)
        ramText_t = view!!.findViewById(R.id.ram_game_info_t)
        batText_t = view!!.findViewById(R.id.bat_game_info_t)
        percText_t = view!!.findViewById(R.id.bat_percent_info_t)
        cpuLoadTextView = view!!.findViewById(R.id.cpu_game_info)
        gpuLoadTextView = view!!.findViewById(R.id.gpu_game_info)
        ramText = view!!.findViewById(R.id.ram_game_info)
        battpercentTextView = view!!.findViewById(R.id.bat_percent_info)
        battTextView = view!!.findViewById(R.id.bat_game_info)
        fpsText = view!!.findViewById(R.id.fps_game_info)
        ram_ht = view!!.findViewById(R.id.ram_info)
        batt_ht = view!!.findViewById(R.id.batt_info)
        temp_ht = view!!.findViewById(R.id.temp_info)

        fpsText_t_small = view!!.findViewById(R.id.fps_game_info_t_small)
        gpuText_t_small = view!!.findViewById(R.id.gpu_game_info_t_small)
        cpuText_t_small = view!!.findViewById(R.id.cpu_game_info_t_small)
        ramText_t_small = view!!.findViewById(R.id.ram_game_info_t_small)
        batText_t_small = view!!.findViewById(R.id.bat_game_info_t_small)
        percText_t_small = view!!.findViewById(R.id.bat_percent_info_t_small)
        cpuLoadTextView_small = view!!.findViewById(R.id.cpu_game_info_small)
        gpuLoadTextView_small = view!!.findViewById(R.id.gpu_game_info_small)
        ramText_small = view!!.findViewById(R.id.ram_game_info_small)
        battpercentTextView_small = view!!.findViewById(R.id.bat_percent_info_small)
        battTextView_small = view!!.findViewById(R.id.bat_game_info_small)
        fpsText_small = view!!.findViewById(R.id.fps_game_info_small)
        ram_ht_small = view!!.findViewById(R.id.ram_info_small)
        batt_ht_small = view!!.findViewById(R.id.batt_info_small)
        temp_ht_small = view!!.findViewById(R.id.temp_info_small)

        temperatureText = view!!.findViewById(R.id.fw_battery_temp)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return view!!
    }
    // detect double tap so we can hide tinyDancer
    private var simpleOnGestureListener: GestureDetector.SimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // hide but don't remove view
            return super.onDoubleTap(e)
        }
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var show: Boolean? = false

        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
        private var timer: Timer? = null
    }
}