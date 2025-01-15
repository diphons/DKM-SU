package com.diphons.dkmsu.ui.component

import android.content.Context
import android.os.Build
import com.diphons.dkmsu.KernelSUApplication
import com.diphons.dkmsu.ui.component.FileWrite
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.KernelProrp
import com.diphons.dkmsu.ui.component.RootFile
import com.diphons.dkmsu.ui.component.BatteryStatus

/**
 * Created by Hello on 2017/11/01.
 */

class BatteryUtils {
    companion object {
        private var fastChargeScript = ""
        private var changeLimitRunning = false
        private var isFirstRun = true

        fun getBatteryTemperature(): BatteryStatus {
            val batteryInfo = KeepShellPublic.doCmdSync("dumpsys battery")
            val batteryInfos = batteryInfo.split("\n")

            var levelReaded = false
            var tempReaded = false
            var statusReaded = false
            val batteryStatus = BatteryStatus()

            for (item in batteryInfos) {
                val info = item.trim()
                val index = info.indexOf(":")
                if (index > Int.MIN_VALUE && index < info.length - 1) {
                    val value = info.substring(info.indexOf(":") + 1).trim()
                    try {
                        if (info.startsWith("status")) {
                            if (!statusReaded) {
                                batteryStatus.statusText = value
                                statusReaded = true
                            } else {
                                continue
                            }
                        } else if (info.startsWith("level")) {
                            if (!levelReaded) {
                                batteryStatus.capacity = value.toInt()
                                levelReaded = true
                            } else continue
                        } else if (info.startsWith("temperature")) {
                            if (!tempReaded) {
                                tempReaded = true
                                batteryStatus.temperature = (value.toFloat() / 10.0)
                            } else continue
                        }
                    } catch (ex: java.lang.Exception) {

                    }
                }
            }
            return batteryStatus
        }
    }

    private fun str2voltage(str: String): String {
        val value = str.substring(0, if (str.length > 4) 4 else str.length).toDouble()

        return (when {
            value > 3000 -> {
                value / 1000
            }
            value > 300 -> {
                value / 100
            }
            value > 30 -> {
                value / 10
            }
            else -> {
                value
            }
        }).toString() + "v"
    }

    fun getChargeFull(): Int {
        val value = KernelProrp.getProp("/sys/class/power_supply/bms/charge_full")
        return if (Regex("^[0-9]+").matches(value)) (value.toInt() / 1000) else 0
    }

    fun setChargeFull(mAh: Int) {
        KernelProrp.setProp("/sys/class/power_supply/bms/charge_full", (mAh * 1000).toString())
    }

    fun getCapacity(): Int {
        val value = KernelProrp.getProp("/sys/class/power_supply/battery/capacity")
        return if (Regex("^[0-9]+").matches(value)) value.toInt() else 0
    }

    fun setCapacity(capacity: Int) {
        KernelProrp.setProp("/sys/class/power_supply/battery/capacity", capacity.toString())
    }

    private var kernelCapacitySupported: Boolean? = null

    fun getKernelCapacity(approximate: Int): Float {
        if (kernelCapacitySupported == null) {
            kernelCapacitySupported = RootFile.fileExists("/sys/class/power_supply/bms/capacity_raw")
        }
        if (kernelCapacitySupported == true) {
            try {
                val raw = KernelProrp.getProp("/sys/class/power_supply/bms/capacity_raw")
                val capacityValue = raw.toInt()

                val valueMA =
                    if (Math.abs(capacityValue - approximate) > Math.abs((capacityValue / 100f) - approximate)) {
                        capacityValue / 100f
                    } else {
                        raw.toFloat()
                    }
                // 如果和系统反馈的电量差距超过5%，则认为数值无效，不再读取
                return if (Math.abs(valueMA - approximate) > 5) {
                    kernelCapacitySupported = false
                    -1f
                } else {
                    valueMA
                }
            } catch (ex: java.lang.Exception) {
                kernelCapacitySupported = false
            }
        }
        return -1f
    }
}
