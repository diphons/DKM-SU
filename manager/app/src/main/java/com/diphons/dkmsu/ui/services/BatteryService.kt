package com.diphons.dkmsu.ui.services

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.*
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.Utils.bypassChg
import com.diphons.dkmsu.ui.util.getBatteryTemperature
import com.diphons.dkmsu.ui.util.isCharging
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class BatteryService : Service() {
    private lateinit var globalConfig: SharedPreferences
    private lateinit var batteryConfig: SharedPreferences
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var screenOn = true
    private var lastScreenStateChange = 0L
    private var nowTimes = 0L
    private var screenOnTime = 0L
    private var screenOffTime = 0L
    private var deepSleepTime = 0L
    private var awakeTime = 0L
    private var lastBatteryLevel = -1
    private var batteryLevelOnStart = -1
    private var activeDrain = 0
    private var idleDrain = 0
    private var lastUptimeMillis = 0L
    lateinit var icon: IconCompat

    private val lock = Object()

    override fun onBind(intent: Intent?) = null

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()
        isRunning = true

        globalConfig = getSharedPreferences(SpfConfig.SETTINGS, MODE_PRIVATE)
        batteryConfig = getSharedPreferences(SpfConfig.BATTERY_SERVICE, Context.MODE_PRIVATE)
        loadPrefs()

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        startForegroundService()
        startUpdating()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(screenReceiver)
        handler?.removeCallbacks(runnable!!)
        savePrefs()

        val broadcastIntent = Intent("com.diphons.dkmsu.BATTERY_INFO")
        if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
            (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(applicationContext))) {
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isRunning = false

        val restartIntent = Intent(applicationContext, BatteryReceiver::class.java)
        restartIntent.action = "com.diphons.dkmsu.BATTERY_INFO"
        if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
            (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(applicationContext))) {
            sendBroadcast(restartIntent)
        }
    }

    private fun loadPrefs() {
        synchronized(lock) {
            screenOnTime = batteryConfig.getLong(SpfConfig.SCREEN_ON_DURATION, 0L)
            screenOffTime = batteryConfig.getLong(SpfConfig.SCREEN_OFF_DURATION, 0L)
            deepSleepTime = batteryConfig.getLong(SpfConfig.DEEP_SLEEP_MILLIS, 0L)
            lastUptimeMillis = SystemClock.uptimeMillis()
            awakeTime = screenOffTime - deepSleepTime
            activeDrain = batteryConfig.getInt(SpfConfig.BATTERY_LOST_SCREEN_ON, 0)
            idleDrain = batteryConfig.getInt(SpfConfig.BATTERY_LOST_SCREEN_OFF, 0)
            batteryLevelOnStart = getCurrentBatteryLevel()
            lastScreenStateChange = batteryConfig.getLong(SpfConfig.LAST_SCREEN_STATE_CHANGE, System.currentTimeMillis())
        }
    }

    private fun getCurrentBatteryLevel(): Int {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        return (level * 100 / scale)
    }

    private fun savePrefs() {
        synchronized(lock) {
            batteryConfig.edit().apply {
                putLong(SpfConfig.SCREEN_ON_DURATION, screenOnTime)
                putLong(SpfConfig.SCREEN_OFF_DURATION, screenOffTime)
                putLong(SpfConfig.DEEP_SLEEP_MILLIS, deepSleepTime)
                putInt(SpfConfig.BATTERY_LOST_SCREEN_ON, activeDrain)
                putInt(SpfConfig.BATTERY_LOST_SCREEN_OFF, idleDrain)
                putLong(SpfConfig.LAST_SCREEN_STATE_CHANGE, lastScreenStateChange)
                apply()
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = "battery_monitor_channel"
        val channel = NotificationChannel(channelId, getString(R.string.battery_status), NotificationManager.IMPORTANCE_HIGH).apply {
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.battery_status))
            .setContentText("Initializing...")
            .setSmallIcon(R.drawable.ic_battery_full)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("ServiceCast")
    private fun startUpdating() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        screenOn = pm.isInteractive
        lastScreenStateChange = System.currentTimeMillis()

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (Utils.resetStats) {
                    resetAll()
                    Utils.resetStats = false
                } else {
                    if (!isCharging(applicationContext))
                        updateScreenTime()
                    else {
                        synchronized(lock) {
                            lastScreenStateChange = System.currentTimeMillis()
                            savePrefs()
                        }
                    }
                    updateNotification()
                }
                handler?.postDelayed(this, 3000)
            }
        }
        handler?.post(runnable!!)
    }

    private fun readCurrentFromSysfs(): Long {
        val paths = listOf(
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/current_avg",
            "/sys/class/power_supply/bms/current_now",
            "/sys/class/power_supply/usb/current_now",
            "/sys/class/power_supply/main/current_now"
        )

        for (p in paths) {
            try {
                val f = File(p)
                if (f.exists()) {
                    val value = f.readText().trim().toInt()
                    return value.toLong()
                }
            } catch (_: Exception) { }
        }
        return 0L
    }

    private fun updateScreenTime() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastScreenStateChange

            if (elapsed > 0) {
                if (screenOn) {
                    screenOnTime += elapsed
                } else {
                    screenOffTime += elapsed
                    accumulateDeepSleepAndAwake(elapsed)
                }
                lastScreenStateChange = now
                savePrefs()
            }
        }
    }

    private fun accumulateDeepSleepAndAwake(offDuration: Long) {
        synchronized(lock) {
            val currentUptime = SystemClock.uptimeMillis()
            val elapsedUptime = currentUptime - lastUptimeMillis

            val maxAwake = max(0L, elapsedUptime)
            val deepSleep = max(0L, offDuration - maxAwake)

            if (deepSleep > 0) {
                deepSleepTime += deepSleep
            }

            lastUptimeMillis = currentUptime
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            synchronized(lock) {
                if (!isCharging(applicationContext)) {
                    nowTimes = System.currentTimeMillis()
                    val elapsed = nowTimes - lastScreenStateChange

                    if (elapsed > 0) {
                        if (screenOn) {
                            screenOnTime += elapsed
                        } else {
                            screenOffTime += elapsed
                            accumulateDeepSleepAndAwake(elapsed)
                        }
                    }
                }

                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenOn = true
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn = false
                    }
                }

                if (!isCharging(applicationContext)) {
                    lastScreenStateChange = nowTimes
                    savePrefs()
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

            synchronized(lock) {
                if (batteryLevelOnStart == -1 && level >= 0)
                    batteryLevelOnStart = level

                if (lastBatteryLevel != -1 && level != lastBatteryLevel) {
                    val delta = lastBatteryLevel - level
                    if (delta > 0) {
                        if (screenOn) activeDrain += delta else idleDrain += delta
                        savePrefs()
                    }
                }

                lastBatteryLevel = level
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val now = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val temperature = getBatteryTemperature(applicationContext)
        val level = lastBatteryLevel
        val status = getBatteryStatus()

        var batState: String? = null
        val curNow = if (now == 0L) readCurrentFromSysfs() else now
        val nowMA = curNow / globalConfig.getInt(SpfConfig.CURRENT_NOW_UNIT, SpfConfig.CURRENT_NOW_UNIT_DEFAULT)
        var valCur = 0
        nowMA.run {
            if (this > -20000 && this < 20000) {
                val watt = (5.0 * this) / 1000
                val getWatt = String.format("%.1f", watt)
                batState = if (status == 5) "" else if (this > 0) ("⚡ $this mA · $getWatt W") else ("$this mA")
                valCur = this.toInt()
            }
        }

        var statusChg = Utils.getStatusString(status, valCur, applicationContext)
        if (bypassChg)
            statusChg = "Bypass"
        val title = "$level% · ${String.format("%.1f", temperature)}°C · $statusChg $batState"
        var content = ""
        if (isCharging(applicationContext)) {
            if (bypassChg) {
                content = getString(R.string.bypass_chg_content)
                if (level == 100 && globalConfig.getBoolean(SpfConfig.BATTERY_RESET_STATUS, false)) {
                    resetAll()
                }
            } else if (status == 5) {
                content = ""
                if (globalConfig.getBoolean(SpfConfig.BATTERY_RESET_STATUS, false)) {
                    resetAll()
                }
            } else getBatteryChargeEstimate(level)
        } else {
            content = "Active drain: ${formatDrainRate(calculateActiveDrainRate())} /hr · idle drain: ${formatDrainRate(calculateIdleDrainRate())} /hr"
        }

        awakeTime = screenOffTime - deepSleepTime
        val sb = StringBuilder()
        if (status == 2)
            sb.appendLine("${getBatteryChargeEstimate(level)}\n")
        else if (bypassChg)
            sb.appendLine("${getString(R.string.bypass_chg_content)}\n")
        sb.appendLine("Active drain: ${formatDrainRate(calculateActiveDrainRate())} /hr · idle drain: ${formatDrainRate(calculateIdleDrainRate())} /hr")
        sb.appendLine("Screen on: ${formatDuration(screenOnTime)} (${"%d".format(activeDrain)}%)")
        sb.appendLine("Screen off: ${formatDuration(screenOffTime)} (${"%d".format(idleDrain)}%)")
        sb.appendLine("Deep sleep: ${formatDuration(deepSleepTime)} (${calculatePercentage(deepSleepTime.toFloat(), (deepSleepTime + awakeTime).toFloat())}%)")
        sb.appendLine("Awake: ${formatDuration(awakeTime)} (${calculatePercentage(awakeTime.toFloat(), (deepSleepTime + awakeTime).toFloat())}%)")
        val bigText = sb.toString().trim()

        if (bypassChg)
            icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_bypass_charging)
        else {
            if (level <= 25)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_20)
            else if (level <= 35)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_30)
            else if (level <= 55)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_50)
            else if (level <= 65)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_60)
            else if (level <= 85)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_80)
            else if (level <= 95)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_90)
            else if (level <= 100)
                icon = IconCompat.createWithResource(applicationContext, R.drawable.ic_battery_full)
        }

        val channelId = "battery_monitor_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOnlyAlertOnce(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    private fun getBatteryStatus(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    }

    @SuppressLint("DefaultLocale")
    private fun getBatteryChargeEstimate(level: Int): String? {
        return try {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val chargeCurrentMicroA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val currentMa = abs(chargeCurrentMicroA) / 1000.0
            if (currentMa <= 0) return null

            val fullCapacityMah = try {
                val pathList = listOf(
                    "/sys/class/power_supply/battery/charge_full",
                    "/sys/class/power_supply/battery/charge_full_design",
                    "/sys/class/power_supply/bms/charge_full",
                    "/sys/class/power_supply/max77729_fg/charge_full"
                )

                var cap = 0.0
                for (path in pathList) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val content = file.readText().trim()
                        val value = content.toDoubleOrNull()
                        if (value != null && value > 1000) {
                            cap = value / 1000.0
                            break
                        }
                    }
                }
                if (cap == 0.0) 4000.0 else cap
            } catch (_: Exception) {
                4000.0
            }

            val remainingMah = fullCapacityMah * (level / 100.0)
            val neededMah = fullCapacityMah - remainingMah

            val hours = neededMah / currentMa
            val totalMinutes = (hours * 60).toInt()
            val h = totalMinutes / 60
            val m = totalMinutes % 60

            if (h > 0)
                String.format("%d hr %d mnt ${getString(R.string.battery_status_estimated)}", h, m)
            else
                String.format("%d mnt ${getString(R.string.battery_status_estimated)}", m)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateActiveDrainRate(): Float {
        return try {
            val lostDuringOn = getBatteryLostWhileScreenOn()
            val screenOnDuration = getScreenOnDuration()
            if (lostDuringOn != 0 && screenOnDuration >= TimeUnit.MINUTES.toMillis(1)) {
                max(0.0f, (lostDuringOn / (screenOnDuration / 3600000.0)).toFloat())
            } else 0.0f
        } catch (_: Exception) {
            0.0f
        }
    }

    private fun calculateIdleDrainRate(): Float {
        return try {
            val lost = getBatteryLostWhileScreenOff()
            val duration = getScreenOffDuration()
            if (lost != 0 && duration >= TimeUnit.MINUTES.toMillis(1)) {
                val rate = (lost / (duration / 3600000.0)).toFloat()
                rate
            } else 0.0f
        } catch (_: Exception) {
            0.0f
        }
    }

    private fun getBatteryLostWhileScreenOff(): Int {
        return batteryConfig.getInt(SpfConfig.BATTERY_LOST_SCREEN_OFF, 0)
    }

    private fun getBatteryLostWhileScreenOn(): Int {
        return batteryConfig.getInt(SpfConfig.BATTERY_LOST_SCREEN_ON, 0)
    }

    private fun getScreenOffDuration(): Long {
        return batteryConfig.getLong(SpfConfig.SCREEN_OFF_DURATION, 0L)
    }

    private fun getScreenOnDuration(): Long {
        return batteryConfig.getLong(SpfConfig.SCREEN_ON_DURATION, 0L)
    }

    private fun calculatePercentage(currentValue: Float, maxValue: Float): Float {
        var result = 0.0f

        if (currentValue != 0.0f && maxValue != 0.0f) {
            result = max(0.0f, (currentValue * 100.0f) / maxValue)
        }

        try {
            val decimalFormat = DecimalFormat()
            decimalFormat.setMaximumFractionDigits(2)
            var formatted = decimalFormat.format(result)
            formatted = formatted.replace(",", ".")
            val finalResult = formatted.toFloat()
            return min(finalResult, 100.0f)
        } catch (_: java.lang.Exception) {
            return -1.0f
        }
    }

    private fun formatDrainRate(value: Float): String {
        val locale = Locale.getDefault()
        return if (value == 0.0f) {
            "0%"
        } else {
            val formatted = String.format(locale, "%.2f", value)
                .replace('.', ',')
            "$formatted%"
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    private fun resetAll() {
        synchronized(lock) {
            screenOnTime = 0
            screenOffTime = 0
            deepSleepTime = 0
            awakeTime = 0
            activeDrain = 0
            idleDrain = 0
            lastScreenStateChange = System.currentTimeMillis()
            lastUptimeMillis = SystemClock.uptimeMillis()
            batteryLevelOnStart = getCurrentBatteryLevel()
            savePrefs()
            updateNotification()
        }
    }

    companion object {
        @Volatile var isRunning = false
    }
}


