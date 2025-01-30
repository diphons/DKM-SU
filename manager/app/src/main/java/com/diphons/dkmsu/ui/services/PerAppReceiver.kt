package com.diphons.dkmsu.ui.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.diphons.dkmsu.ui.popup.FPSMonitor
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.onFocusedApp
import com.diphons.dkmsu.ui.util.runProfileWorker

class PerAppReceiver : BroadcastReceiver() {
    private lateinit var globalSPF: SharedPreferences
    private lateinit var prefs: SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {
        globalSPF = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        onFocusedApp = RootUtils.runAndGetOutput("getprop init.dkmsvc.perapp")
        prefs = context.getSharedPreferences(onFocusedApp, Context.MODE_PRIVATE)

        if (prefs.getBoolean(SpfConfig.MONITOR_MINI, false))
            FPSMonitor(context).showPopupWindow()
        else if (globalSPF.getBoolean(SpfConfig.MONITOR_MINI, false))
            FPSMonitor(context).showPopupWindow()
        else
            FPSMonitor(context).hidePopupWindow()

        runProfileWorker(context)
    }
}