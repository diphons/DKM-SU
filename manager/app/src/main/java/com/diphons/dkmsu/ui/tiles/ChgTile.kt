package com.diphons.dkmsu.ui.tiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.DYNAMIC_CHARGING
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.setProfile

class ChgTile : TileService() {
    private var id = 0
    private var cur_chg = 0
    private var cur_chg0 = 0
    private var cur_chg1 = 0
    private var cur_chg2 = 0
    private var cur_chg3 = 0
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

        id = prefs.getInt(SpfConfig.CHG_MAX, 0)
        cur_chg = prefs.getInt(SpfConfig.CHG_CUR_MAX, 0)

        cur_chg0 = cur_chg
        cur_chg3 = cur_chg / 4
        cur_chg2 = cur_chg / 2
        cur_chg1 = cur_chg3 + cur_chg2

        if (id == 0) {
            id = 1
            newLabel = "${cur_chg1 * 5.5 / 1000000} Watt"
        } else if (id == 1) {
            id = 2
            newLabel = "${cur_chg2 * 5.5 / 1000000} Watt"
        } else if (id == 2) {
            id = 3
            newLabel = "${cur_chg3 * 5.5 / 1000000} Watt"
        } else {
            id = 0
            newLabel = "${cur_chg0 * 5.5 / 1000000} Watt"
        }

        newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_charge)
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
        cur_chg = prefs.getInt(SpfConfig.CHG_CUR_MAX, 0)

        cur_chg0 = cur_chg
        cur_chg3 = cur_chg / 4
        cur_chg2 = cur_chg / 2
        cur_chg1 = cur_chg3 + cur_chg2

        newState = Tile.STATE_ACTIVE
        newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_charge)
        newLabel = "${cur_chg0 * 5.5 / 1000000} Watt"

        fun perfMode(){
            if (prefs.getBoolean(SpfConfig.PERF_MODE, true)) {
                newState = Tile.STATE_ACTIVE
                if (id == 1) {
                    newLabel = "${cur_chg1 * 5.5 / 1000000} Watt"
                } else if (id == 2) {
                    newLabel = "${cur_chg2 * 5.5 / 1000000} Watt"
                } else if (id == 3) {
                    newLabel = "${cur_chg3 * 5.5 / 1000000} Watt"
                } else {
                    newLabel = "${cur_chg0 * 5.5 / 1000000} Watt"
                }
            } else {
                newState = Tile.STATE_UNAVAILABLE
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_none)
                newLabel = getString(R.string.performance_mode)
            }
        }
        if (hasModule(DYNAMIC_CHARGING)) {
            if (prefs.getBoolean(SpfConfig.DYNAMIC_CHARGING, true)) {
                newState = Tile.STATE_UNAVAILABLE
                newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_perf_ai)
                newLabel = getString(R.string.charger_max)
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