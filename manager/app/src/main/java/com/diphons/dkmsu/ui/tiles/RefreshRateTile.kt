package com.diphons.dkmsu.ui.tiles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.checkFPS
import com.diphons.dkmsu.ui.util.checkRefresRate
import com.diphons.dkmsu.ui.util.getCurFPSID
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.getfpsbyid
import com.diphons.dkmsu.ui.util.setRefreshRate

class RefreshRateTile : TileService() {
    lateinit var prefs: SharedPreferences
    lateinit var refreshrete: String
    lateinit var infinity_display: String
    lateinit var getFPSdata: String
    private var id = 1

    override fun onClick() {
        super.onClick()
        if (getFPSCount() > 1)
            updateTile()
    }

    private fun updateTile() {
        val tile = this.qsTile
        val newState: Int = Tile.STATE_ACTIVE
        id = getCurFPSID(refreshrete)
        if (id == getFPSCount())
            id = 1
        else
            id += 1
        refreshrete = getfpsbyid(id)
        prefs.edit().putString(SpfConfig.REFRESH_RATE, refreshrete).apply()
        setRefreshRate(strToInt(refreshrete))

        val newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_refreshrate)
        val newLabel = "$refreshrete Hz"
        // Change the UI of the tile.
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        prefs = applicationContext.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
        refreshrete = "${prefs.getString(SpfConfig.REFRESH_RATE, "60")}"
        infinity_display = "${prefs.getString(SpfConfig.DYNAMIC_FPS, "")}"
        getFPSdata = checkFPS()
        refreshrete = checkRefresRate()
        id = getCurFPSID(refreshrete)
        resetTile()
    }

    fun resetTile() {
        val tile = this.qsTile
        if (getFPSCount() > 1) {
            if (!checkRefresRate().contains(refreshrete))
                refreshrete = checkRefresRate()
            if (infinity_display.isEmpty() && refreshrete.contains("finity")) {
                prefs.edit().putString(SpfConfig.DYNAMIC_FPS, refreshrete).apply()
                infinity_display = "infinity"
            }
        } else
            refreshrete = getfpsbyid(1)

        val newLabel = "$refreshrete Hz"
        val newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_refreshrate)
        val newState = Tile.STATE_ACTIVE
        // Change the UI of the tile.
        tile.label = newLabel
        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }
}