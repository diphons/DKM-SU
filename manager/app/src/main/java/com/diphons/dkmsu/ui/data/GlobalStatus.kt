package com.diphons.dkmsu.ui.data

import android.os.BatteryManager
import com.diphons.dkmsu.ui.component.BatteryUtils
import com.diphons.dkmsu.ui.util.rootAvailable

object GlobalStatus {
    var temperatureCurrent = -1.0
        private set
    private var batteryTempTime = 0L
    fun setBatteryTemperature(temperature: Double) {
        batteryTempTime = System.currentTimeMillis()
        temperatureCurrent = temperature
    }

    fun updateBatteryTemperature(): Double {
        if (rootAvailable() && System.currentTimeMillis() - 5000 >= batteryTempTime) {
            // 更新电池温度
            val temperature = BatteryUtils.getBatteryTemperature().temperature
            if (temperature > 10 && temperature < 100) {
                setBatteryTemperature(temperature)
            }
        }
        return temperatureCurrent
    }

    var batteryCapacity = -1
    var batteryVoltage = -1.0
    var batteryCurrentNow: Long = -1
    var batteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN
    var lastPackageName = ""
}