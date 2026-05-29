package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.isCharging

class BatteryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private lateinit var globalConfig: SharedPreferences

    override fun doWork(): Result {
        val context = applicationContext
        globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

        if (!BatteryService.isRunning) {
            val intent = Intent(context, BatteryService::class.java)
            if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
                (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(context)))
                context.startForegroundService(intent)
        }
        return Result.success()
    }
}

