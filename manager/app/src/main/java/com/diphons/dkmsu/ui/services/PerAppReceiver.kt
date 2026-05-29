package com.diphons.dkmsu.ui.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.SystemController
import com.diphons.dkmsu.ui.popup.FPSMonitor
import com.diphons.dkmsu.ui.popup.GamePopup
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.dkmsvcDaemonPath
import com.diphons.dkmsu.ui.util.onFocusedApp
import com.diphons.dkmsu.ui.util.prefMode
import com.diphons.dkmsu.ui.util.runProfileWorker

class PerAppReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Utils.getScreenState(context)) {
            val globalSPF = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
            val perAppList = globalSPF.getString(SpfConfig.PER_APP_LIST, "")
            onFocusedApp = KeepShellPublic.doCmdSync("${dkmsvcDaemonPath()} status perapp")
            if (onFocusedApp.isBlank() || (onFocusedApp.isNotEmpty() && perAppList!!.isNotEmpty() && !perAppList.contains(onFocusedApp))) {
                hideAllPopups(context)
                return
            }

            val prefs = context.getSharedPreferences(onFocusedApp, Context.MODE_PRIVATE)
            handleFPSMonitor(context, globalSPF, prefs)
        }
    }

    private fun handleFPSMonitor(
        context: Context,
        globalSPF: SharedPreferences,
        prefs: SharedPreferences
    ) {
        val gameTool = prefs.getBoolean(SpfConfig.GAME_TOOL, false)
        if (gameTool)
            GamePopup(context).showPopupWindow()
        else if (GamePopup.show == true)
            GamePopup(context).hidePopupWindow()

        if (prefs.getBoolean(SpfConfig.MONITOR_MINI, false))
            if (gameTool)
                FPSMonitor(context).showPopupWindow(prefs)
            else
                FPSMonitor(context).showPopupWindow()
        else if (globalSPF.getBoolean(SpfConfig.MONITOR_MINI, false))
            FPSMonitor(context).showPopupWindow()
        else
            FPSMonitor(context).hidePopupWindow()

        if (gameTool) {
            val profileMode = prefs.getInt(SpfConfig.GAME_TOOL_PERF, 1)
            prefMode(context, profileMode, prefs, globalSPF)
        } else
            runProfileWorker(context)
    }

    private fun hideAllPopups(context: Context) {
        if (GamePopup.show == true)
            GamePopup(context).hidePopupWindow()
        if (FPSMonitor.show == true)
            FPSMonitor(context).hidePopupWindow()
        runProfileWorker(context)
    }
}