package com.diphons.dkmsu.ui.component

import com.diphons.dkmsu.ui.component.FileWrite
import com.diphons.dkmsu.ui.component.*
import com.diphons.dkmsu.ui.component.ZramWriteBackStat
import com.diphons.dkmsu.ui.util.Utils
import java.io.File

/**
 * @author diphons
 * @date 2025/02/25.
 */
class SwapUtils() {
    val zramSupport: Boolean
        get() {
            return KeepShellPublic.doCmdSync(
                "if [[ ! -e /dev/block/zram0 ]] && [[ -e /sys/class/zram-control ]]; then\n" +
                        "  cat /sys/class/zram-control/hot_add\n" +
                        "fi\n" +
                        "if [[ -e /dev/block/zram0 ]]; then echo 1; else echo 0; fi;"
            ) == "1"
        }

    val zramWriteBackSupport: Boolean
        get() {
            return KeepShellPublic.doCmdSync("if [[ -e /sys/block/zram0/backing_dev ]]; then echo 1; else echo 0; fi;") == "1"
        }

    val zramEnabled: Boolean
        get() {
            return KeepShellPublic.doCmdSync("cat /proc/swaps | grep /block/zram0").contains("/block/zram0")
        }

    fun zramOff() {
        val sb = StringBuilder("sync\necho 3 > /proc/sys/vm/drop_caches\n")

        sb.append("swapoff /dev/block/zram0\n")
        sb.append("echo 1 > /sys/block/zram0/reset\n")
        sb.append("echo 0 > /sys/block/zram0/disksize\n")
        sb.append("echo 0 > /sys/block/zram0/mem_limit\n")
        sb.append("echo 0 > /sys/block/zram0/max_comp_streams\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())
        keepShell.tryExit()
    }

    fun resizeZram(sizeVal: Int, algorithm: String = "") {
        val keepShell = KeepShell()
        val sb = StringBuilder()
        if (!zramEnabled) {
            sb.append("echo 8 > /sys/block/zram0/max_comp_streams\n")
            sb.append("sync\n")
            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  backing_dev=$(cat /sys/block/zram0/backing_dev)\n")
            sb.append("fi\n")

            sb.append("echo 3 > /proc/sys/vm/drop_caches\n")
            sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("echo 1 > /sys/block/zram0/reset\n")

            if (algorithm.isNotEmpty()) {
                sb.append("echo \"$algorithm\" > /sys/block/zram0/comp_algorithm\n")
            }

            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  echo \"\$backing_dev\" > /sys/block/zram0/backing_dev\n")
            sb.append("fi\n")

            if (sizeVal > 2047) {
                sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
            } else {
                sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
            }
            sb.append("echo " + sizeVal + "M > /sys/block/zram0/mem_limit\n")

            sb.append("echo 8 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("swapon /dev/block/zram0 -p 0 >/dev/null 2>&1\n")
            keepShell.doCmdSync(sb.toString())
        }
        keepShell.tryExit()
    }

    val writeBackStat: ZramWriteBackStat
        get() {
            return ZramWriteBackStat().apply {
                backingDev = KernelProrp.getProp("/sys/block/zram0/backing_dev")
                val bdStats = KernelProrp.getProp("/sys/block/zram0/bd_stat").trim().split(Regex("[ ]+"))
                if (bdStats.size == 3) {
                    backed = bdStats[0].toInt() * 4
                    backReads = bdStats[1].toInt() * 4
                    backWrites = bdStats[2].toInt() * 4
                }
            }
        }

    val zramCurrentSizeMB: Int
        get() {
            val currentSize = KeepShellPublic.doCmdSync("cat /sys/block/zram0/disksize")
            try {
                return (currentSize.toLong() / 1024 / 1024).toInt()
            } catch (ex: java.lang.Exception) {
                return 0
            }
        }

    val compAlgorithmOptions: String
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm")
            return compAlgorithmItems.replace("[", "").replace("]", "")
        }

    var compAlgorithm: String
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm").split(" ")
            val result = compAlgorithmItems.find {
                it.startsWith("[") && it.endsWith("]")
            }
            if (result != null) {
                return result.replace("[", "").replace("]", "").trim()
            }
            return ""
        }
        set(value) {
            KeepShellPublic.doCmdSync("echo 1 > /sys/block/zram0/reset")
            KernelProrp.setProp("/sys/block/zram0/comp_algorithm", value)
        }

    val procSwaps: MutableList<String>
        get() {
            val ret = KernelProrp.getProp("/proc/swaps")
            var txt = ret.replace("\t\t", "\t").replace("\t", " ")
            while (txt.contains("  ")) {
                txt = txt.replace("  ", " ")
            }
            val rows = txt.split("\n").toMutableList()
            return rows
        }

    val zramUsedSize: Int
        get() {
            for (row in procSwaps) {
                if (row.startsWith("/block/zram0 ") || row.startsWith("/dev/block/zram0 ")) {
                    val cols = row.split(" ").toMutableList()
                    val sizeStr = cols[2]
                    val usedStr = cols[3]

                    try {
                        return usedStr.toInt() / 1024
                    } catch (ex: java.lang.Exception) {
                        break
                    }
                }
            }
            return -1
        }

    val zramPriority: Int?
        get() {
            for (row in procSwaps) {
                if (row.startsWith("/block/zram0 ") || row.startsWith("/dev/block/zram0 ")) {
                    val cols = row.split(" ").toMutableList()

                    try {
                        return cols[4].toInt()
                    } catch (ex: java.lang.Exception) {
                        break
                    }
                }
            }
            return null
        }
}