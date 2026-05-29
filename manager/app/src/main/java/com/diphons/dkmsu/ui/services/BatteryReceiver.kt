package com.diphons.dkmsu.ui.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.isCharging

class BatteryReceiver : BroadcastReceiver() {
    private lateinit var globalConfig: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

        when (intent.action) {
            "com.diphons.dkmsu.BATTERY_INFO",
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_REBOOT,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val serviceIntent = Intent(context, BatteryService::class.java)
                if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
                    (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(context)))
                    context.startForegroundService(serviceIntent)
                else
                    context.stopService(serviceIntent)
            }
        }
    }
    /*
    override fun onReceive(context: Context?, intent: Intent?) {
        globalConfig = context!!.getSharedPreferences("settings", Context.MODE_PRIVATE)

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "com.diphons.dkmsu.BATTERY_INFO") {
            context.let {
                val serviceIntent = Intent(it, BatteryService::class.java)
                if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
                    (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(it)))
                    it.startForegroundService(serviceIntent)
                else {
                    it.stopService(serviceIntent)
                }
            }
        }
    }*/
}
