package com.diphons.dkmsu.ui.tiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.hasModule

class BypassTile : TileService() {
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
        val newLabel = this.getString(R.string.bypass_chg)
        state = prefs.getBoolean(SpfConfig.BYPASS_CHARGING, false)
        prefs.edit { putBoolean(SpfConfig.BYPASS_CHARGING, !state) }
        val newIcon: Icon = Icon.createWithResource(applicationContext, R.drawable.ic_bypass_charging)
        val newState: Int = if (state)
            Tile.STATE_INACTIVE
        else
            Tile.STATE_ACTIVE
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs = applicationContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        state = prefs.getBoolean(SpfConfig.BYPASS_CHARGING, false)
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        val newLabel = this.getString(R.string.bypass_chg)
        val newIcon: Icon = Icon.createWithResource(applicationContext, R.drawable.ic_bypass_charging)
        val newState: Int = if (isManager && hasModule(Utils.INPUT_SUSPEND)) {
            if (state)
                Tile.STATE_ACTIVE
            else
                Tile.STATE_INACTIVE
        } else
            Tile.STATE_UNAVAILABLE
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}