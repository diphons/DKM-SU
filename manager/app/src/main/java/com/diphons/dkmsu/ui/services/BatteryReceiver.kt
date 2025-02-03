package com.diphons.dkmsu.ui.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.MainActivity
import com.diphons.dkmsu.ui.data.GlobalStatus
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.wakelockTimesFormat

class BatteryReceiver : BroadcastReceiver() {
    var judul: String? = null
    var summary: String? = null
    private lateinit var globalConfig: SharedPreferences
    private var batteryManager: BatteryManager? = null
    lateinit var icon: IconCompat
    var show = false
    val NOTIFICATION_CHANNEL_ID = "battservice"
    lateinit var handler: Handler
    lateinit var runnable: Runnable
    lateinit var getStatus: String
    var count = 0

    @SuppressLint("DefaultLocale")
    override fun onReceive(context: Context, intent: Intent) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        if (batteryManager == null) {
            batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }

        val managerA = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (Utils.getScreenState(context)){
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

                    var getTemp = (temp / 10).toFloat()
                    var status = 0
                    var timeFullNow = ""
                    kotlin.runCatching {
                        getStatus = RootUtils.runAndGetOutput("getprop init.dkmsvc.battery.status")
                    }
                    if (getStatus.isNotEmpty()) {
                        status = Utils.strToInt(getStatus.substring(0, getStatus.indexOf(";")))
                        timeFullNow = wakelockTimesFormat(Utils.strToInt(getStatus.replace("$status;", "")).toLong() * 1000)
                    }

                    val batteryPct = batteryManager!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    var batState: String? = null
                    val now = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val nowMA = if (now != null) {
                        (now / globalConfig.getInt(SpfConfig.CURRENT_NOW_UNIT, SpfConfig.CURRENT_NOW_UNIT_DEFAULT))
                    } else {
                        null
                    }
                    var valCur = 0
                    nowMA?.run {
                        if (this > -20000 && this < 20000) {
                            val watt = (5.0 * this) / 1000
                            val getWatt = String.format("%.1f", watt)
                            batState = if (this > 0) (" ⚡ $this mA · $getWatt W") else (" $this mA")
                            valCur = this.toInt()
                        }
                    }
                    if (getTemp.toInt() == 0) {
                        getTemp = GlobalStatus.updateBatteryTemperature().toFloat()
                    }

                    val statusChg = Utils.getStatusString(status, valCur, context)
                    judul = "$batteryPct% · ${if (getTemp.toString().contains(".")) getTemp.toString().substring(0, getTemp.toString().indexOf("."))
                    else getTemp.toString()}" + "ºC · $statusChg $batState"
                    summary = if (timeFullNow.isEmpty() || timeFullNow.contains("-1s") || timeFullNow.contains("0s")) null else "$timeFullNow for a full charge"

                    if (batteryPct <= 25)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_20)
                    else if (batteryPct <= 35)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_30)
                    else if (batteryPct <= 55)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_50)
                    else if (batteryPct <= 65)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_60)
                    else if (batteryPct <= 85)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_80)
                    else if (batteryPct <= 95)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_90)
                    else if (batteryPct <= 100)
                        icon = IconCompat.createWithResource(context, R.drawable.ic_battery_full)

                    val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    Utils.notificationbs =
                        notificationBuilder.setSmallIcon(icon)
                            .setContentTitle(judul)
                            .setContentText(null)
                            .setGroup("DKMSU")
                            .setCategory(Notification.CATEGORY_SERVICE)
                            .setContentIntent(pendingIntent)
                            .setSilent(true)
                            .setStyle(NotificationCompat.BigTextStyle()
                                .bigText(summary))
                            .build()

                    count += 1
                    if (count >= 100)
                        count = 0
                    // Add notification to service when service is running
                    globalConfig.edit().putInt(SpfConfig.BATTERY_NOTIF_STATUS, count).apply()
                    // Schedule the task to run again after 1.5 second
                    if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false)) {
                        managerA.notify(Utils.NOTIF_ID_BS, Utils.notificationbs)
                        handler.postDelayed(this, 1500)
                    } else {
                        managerA.cancel(Utils.NOTIF_ID_BS)
                        handler.removeCallbacks(runnable)
                    }
                }
            }
        }

        if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false)) {
            if (!show) {
                show = true
                startNotif(context)
                handler.post(runnable)
            }
        } else {
            if (show) {
                show = false
            }
        }
    }

    private fun startNotif(context: Context){
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Battery Info Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Battery Info")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_battery_full)
            .setContentIntent(pendingIntent)
            .build()

        val managerA = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        managerA.notify(Utils.NOTIF_ID_BS, notification)
    }
}