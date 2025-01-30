package com.diphons.dkmsu.ui.tiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.popup.FPSMonitor
import com.diphons.dkmsu.ui.util.FPSActive

class FPSTile : TileService() {
    lateinit var prefs: SharedPreferences
    var state: Boolean = false
    val isManager = Natives.becomeManager(ksuApp.packageName)

    override fun onClick() {
        super.onClick()
        if (isManager)
            updateTile()
    }

    private fun updateTile() {
        val tile = this.qsTile
        val newState: Int

        state = prefs.getBoolean(SpfConfig.MONITOR_MINI, false)
        prefs.edit().putBoolean(SpfConfig.MONITOR_MINI, !state).apply()
        if (state || FPSActive) {
            newState = Tile.STATE_INACTIVE
            FPSMonitor(applicationContext).hidePopupWindow()
        } else {
            FPSActive = false
            newState = Tile.STATE_ACTIVE
            FPSMonitor(applicationContext).showPopupWindow()
        }

        val newIcon: Icon = Icon.createWithResource(applicationContext, R.drawable.ic_fps_monitor)
        // Change the UI of the tile.
        tile.label = "FPS Meter"
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs = applicationContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        state = prefs.getBoolean(SpfConfig.MONITOR_MINI, false)
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        val newState: Int
        if (isManager) {
            if (state || FPSActive) {
                newState = Tile.STATE_ACTIVE
                FPSMonitor(applicationContext).showPopupWindow()
            } else {
                newState = Tile.STATE_INACTIVE
                FPSMonitor(applicationContext).hidePopupWindow()
            }
        } else {
            newState = Tile.STATE_UNAVAILABLE
        }
        val newIcon: Icon = Icon.createWithResource(applicationContext, R.drawable.ic_fps_monitor)
        // Change the UI of the tile.
        tile.label = "FPS Meter"
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}