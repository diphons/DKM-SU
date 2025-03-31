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

class SelinuxTile : TileService() {
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
        val newLabel: String
        val newIcon: Icon

        state = prefs.getBoolean(SpfConfig.SELINUX_MODE, !Utils.isSELinuxActive())
        prefs.edit { putBoolean(SpfConfig.SELINUX_MODE, !state) }
        if (state) {
            newState = Tile.STATE_INACTIVE
            newLabel = "Enforcing"
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_selinux)
            Utils.activateSELinux(true, this)
        } else {
            newState = Tile.STATE_ACTIVE
            newLabel = "Permissive"
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_selinux_permissive)
            Utils.activateSELinux(false, this)
        }
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs = applicationContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        state = prefs.getBoolean(SpfConfig.SELINUX_MODE, !Utils.isSELinuxActive())
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        val newState: Int
        val newLabel: String
        val newIcon: Icon
        if (isManager) {
            if (state) {
                newState = Tile.STATE_ACTIVE
                newLabel = "Permissive"
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_selinux_permissive)
            } else {
                newState = Tile.STATE_INACTIVE
                newLabel = "Enforcing"
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_selinux)
            }
        } else {
            newState = Tile.STATE_UNAVAILABLE
            newLabel = "Selinux"
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_selinux)
        }
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}