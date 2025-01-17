package com.diphons.dkmsu.ui.tiles

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.popup.PowerMenu
import com.diphons.dkmsu.ui.util.RootUtils

class PowerTile : TileService() {
    var state: Boolean = false
    val isManager = Natives.becomeManager(ksuApp.packageName)

    override fun onClick() {
        super.onClick()
        if (isManager) {
            RootUtils.runCommand("service call statusbar 2")
            PowerMenu(applicationContext).open()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        val newState: Int = if (isManager)
            Tile.STATE_ACTIVE
        else
            Tile.STATE_UNAVAILABLE
        val newIcon: Icon = Icon.createWithResource(applicationContext, R.drawable.ic_power_off)
        // Change the UI of the tile.
        tile.label = getString(R.string.power_menu)
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}