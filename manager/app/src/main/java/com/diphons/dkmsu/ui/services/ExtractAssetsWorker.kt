package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.component.ExtractAssets
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.BOEFFLA_WL_BLOCKER
import com.diphons.dkmsu.ui.util.Utils.getChargeType
import com.diphons.dkmsu.ui.util.batteryWorker
import com.diphons.dkmsu.ui.util.getStartedSVC
import com.diphons.dkmsu.ui.util.getXMPath
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.hasXiaomiDevice
import com.diphons.dkmsu.ui.util.isCharging
import com.diphons.dkmsu.ui.util.restartDkmSVC
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.diphons.dkmsu.ui.util.setWLBlocker

class ExtractAssetsWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences

    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

        restartDkmSVC()

        if (hasXiaomiDevice() && !hasModule(getXMPath(applicationContext))) {
            ExtractAssets(applicationContext).extractResources("xiaomi-touch")
            RootUtils.runCommand("find /data/data/${applicationContext.packageName}/files/xioami-touch -type f chmod 755")
        }

        if (hasModule(BOEFFLA_WL_BLOCKER)) {
            setWLBlocker(applicationContext)
        }

        if (!getStartedSVC())
            runSVCWorker(applicationContext, "")

        if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false) &&
            (globalConfig.getInt(SpfConfig.BATTERY_NOTIF_MODE, 1) == 0 || isCharging(applicationContext))) {
            val intent = Intent(applicationContext, BatteryService::class.java)
            ContextCompat.startForegroundService(applicationContext, intent)
            batteryWorker(applicationContext)
        }

        return Result.success()
    }
}