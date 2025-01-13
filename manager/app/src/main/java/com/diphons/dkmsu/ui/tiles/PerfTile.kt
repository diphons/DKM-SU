package com.diphons.dkmsu.ui.tiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.GAME_AI
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.setProfile

class PerfTile : TileService() {
    private var id = 0
    lateinit var prefs: SharedPreferences

    override fun onClick() {
        super.onClick()
            updateTile()
    }

    private fun updateTile() {
        val tile = this.qsTile
        val newIcon: Icon
        val newLabel: String
        val newState: Int = Tile.STATE_ACTIVE

        id = prefs.getInt(SpfConfig.PROFILE_MODE, 0)
        if (id == 0) {
            id = 2
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_game)
            newLabel = getString(R.string.game)
        } else if (id == 2) {
            id = 1
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_performance)
            newLabel = getString(R.string.performance)
        } else if (id == 1) {
            id = 4
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_battery)
            newLabel = getString(R.string.battery)
        } else {
            id = 0
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_balance)
            newLabel = getString(R.string.balance)
        }

        // Set Profile
        setProfile(applicationContext, id)

        // Change the UI of the tile.
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs = applicationContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        var newIcon: Icon
        var newLabel: String
        var newState: Int
        id = prefs.getInt(SpfConfig.PROFILE_MODE, 0)

        newState = Tile.STATE_ACTIVE
        newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_balance)
        newLabel = getString(R.string.balance)

        fun perfMode(){
            if (prefs.getBoolean(SpfConfig.PERF_MODE, true)) {
                newState = Tile.STATE_ACTIVE
                if (id == 1) {
                    newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_performance)
                    newLabel = getString(R.string.performance)
                } else if (id == 2) {
                    newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_game)
                    newLabel = getString(R.string.game)
                } else if (id == 4) {
                    newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_battery)
                    newLabel = getString(R.string.battery)
                } else {
                    newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_balance)
                    newLabel = getString(R.string.balance)
                }
            } else {
                newState = Tile.STATE_UNAVAILABLE
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_none)
                newLabel = getString(R.string.performance_mode)
            }
        }
        if (hasModule(GAME_AI)) {
            if (prefs.getBoolean(SpfConfig.GAME_AI, true)) {
                newState = Tile.STATE_UNAVAILABLE
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_ai)
                newLabel = getString(R.string.performance_mode)
            } else {
                perfMode()
            }
        } else {
            perfMode()
        }

        // Change the UI of the tile.
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}