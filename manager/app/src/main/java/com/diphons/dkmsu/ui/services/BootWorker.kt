package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.SwapUtils
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.getCurrentCharger
import com.diphons.dkmsu.ui.util.getDefDT2W
import com.diphons.dkmsu.ui.util.getKNVersion
import com.diphons.dkmsu.ui.util.getXMPath
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.hasXiaomiDevice
import com.diphons.dkmsu.ui.util.mbToGB
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.diphons.dkmsu.ui.util.setGameList
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setProfile
import com.diphons.dkmsu.ui.util.setWLBlocker
import com.diphons.dkmsu.ui.util.setXiaomiTouch
import java.util.Timer
import java.util.TimerTask

class BootWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences
    private lateinit var gameai_prefs: SharedPreferences

    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        gameai_prefs = applicationContext.getSharedPreferences("game_ai", Context.MODE_PRIVATE)

        KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist +${applicationContext.packageName}")

        if (globalConfig.getBoolean(SpfConfig.SPOOF_ENCRYPT, false))
            RootUtils.runCommand("resetprop ro.crypto.state encrypted")

        if (globalConfig.getBoolean(SpfConfig.SELINUX_ONBOOT, false)) {
            val selinuxState = globalConfig.getBoolean(SpfConfig.SELINUX_MODE, !isSELinuxActive())
            activateSELinux(selinuxState, applicationContext)
        }

        globalConfig.edit().putBoolean(SpfConfig.GKI_MODE, getKNVersion()).apply()
        if (hasModule(GAME_AI)) {
            var listDefault = gameai_prefs.getString("game_ai_default", "")
            if (listDefault!!.isEmpty()) {
                listDefault = RootUtils.runAndGetOutput("cat $GAME_AI_LIST_DEFAULT | sed 's/\"/#/g'")
                gameai_prefs.edit().putString("game_ai_default", listDefault).apply()
            }
            setGameList(applicationContext)
        }
        if (globalConfig.getBoolean(SpfConfig.PERF_MODE, true)) {
            if (hasModule(GAME_AI)) {
                if (globalConfig.getBoolean(SpfConfig.GAME_AI, true))
                    setKernel("Y", GAME_AI)
                else
                    setKernel("N", GAME_AI)
                if (globalConfig.getBoolean(SpfConfig.GAME_AI_LOG, false)) {
                    setKernel("1", GAME_AI_LOG)
                }
            }
        }
        setProfile(applicationContext, globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
        if (hasModule(HAPTIC_LEVEL)) {
            setKernel("${(globalConfig.getFloat(SpfConfig.HAPTIC_LEVEL, 0.8f) * 100).toInt()}", HAPTIC_LEVEL)
        }
        if ((hasModule(DT2W) || hasModule(DT2W_LEGACY)) && globalConfig.getBoolean(SpfConfig.DT2W, getDefDT2W()))
            setKernel("1", DT2W)
        if (hasModule(TOUCH_SAMPLE) && globalConfig.getBoolean(SpfConfig.TOUCH_SAMPLE, false))
            setKernel("1", TOUCH_SAMPLE)
        if (hasModule(SOUND_CONTROL)) {
            setKernel("${(globalConfig.getFloat(SpfConfig.SC_MICROFON, 0f) * 100).toInt() * 20 / 100}", MICROPHONE_GAIN)
            setKernel("${(globalConfig.getFloat(SpfConfig.SC_EARFON, 0f) * 100).toInt() * 20 / 100}", EARPIECE_GAIN)
            setKernel("${(globalConfig.getFloat(SpfConfig.SC_HEADFON, 0f) * 100).toInt() * 20 / 100} ${(globalConfig.getFloat(
                SpfConfig.SC_HEADFON2, 0f) * 100).toInt() * 20 / 100}", HEADPHONE_GAIN
            )
        }
        if (hasXiaomiDevice() && hasModule(getXMPath(applicationContext))) {
            if (globalConfig.getBoolean(SpfConfig.DT2W, true)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.DT2W, true), 14, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_GAME, false), 0, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_ACTIVE, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_ACTIVE, false), 1, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_UP, false), 2, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_TOLE, false), 3, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_AIM, false), 4, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_TAP, false), 5, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_EXP, false), 6, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_EDGE, false), 7, applicationContext)
            }
            if (globalConfig.getBoolean(SpfConfig.XT_GAME, false)) {
                setXiaomiTouch(globalConfig.getBoolean(SpfConfig.XT_GRIP, false), 15, applicationContext)
            }
        }
        if (hasModule(DISPLAY_RED)) {
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_RED, 1f) * 256).toInt()}", DISPLAY_RED)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_GREEN, 1f) * 256).toInt()}", DISPLAY_GREEN)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_BLUE, 1f) * 256).toInt()}", DISPLAY_BLUE)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_SATURATED, 0.6662165f) * 383).toInt()}", DISPLAY_SATURATED)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_VALUE, 0.6662165f) * 383).toInt()}", DISPLAY_VALUE)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_CONTRAST, 0.6662165f) * 383).toInt()}", DISPLAY_CONTRAST)
            setKernel("${(globalConfig.getFloat(SpfConfig.DISPLAY_HUE, 0f) * 1536).toInt()}", DISPLAY_HUE)
        }
        if (hasModule(DYNAMIC_CHARGING)) {
            if (globalConfig.getBoolean(SpfConfig.DYNAMIC_CHARGING, false)) {
                setKernel("1", DYNAMIC_CHARGING)
            } else {
                setKernel("0", DYNAMIC_CHARGING)
                setKernel("${globalConfig.getInt(SpfConfig.CHG_MAX_CUR_PROF, getCurrentCharger())}", CHG_CUR_MAX)
            }
        } else {
            setKernel("${globalConfig.getInt(SpfConfig.CHG_MAX_CUR_PROF, getCurrentCharger())}", CHG_CUR_MAX)
        }
        if (hasModule(SKIP_THERMAL) && globalConfig.getBoolean(SpfConfig.SKIP_THERMAL, false))
            setKernel("1", SKIP_THERMAL)
        if (hasModule(FCHG_SYS) && globalConfig.getBoolean(SpfConfig.FAST_CHARGING, false))
            setKernel("1", FCHG_SYS)
        if (hasModule(NIGHT_CHARGING) && globalConfig.getBoolean(SpfConfig.NIGHT_CHARGING, false))
            setKernel("1", NIGHT_CHARGING)
        if (hasModule(STEP_CHARGING) && globalConfig.getBoolean(SpfConfig.STEP_CHARGING, false))
            setKernel("1", STEP_CHARGING)
        if (hasModule(BOEFFLA_WL_BLOCKER))
            setWLBlocker(applicationContext)
        if (globalConfig.getBoolean(SpfConfig.BATTERY_NOTIF, false))
            RootUtils.runCommand("setprop init.dkmsvc.cmd battery")

        // TCP Cong Default
        if (globalConfig.getString(SpfConfig.TCP_CONG_DEF, "")!!.isEmpty())
            globalConfig.edit().putString(SpfConfig.TCP_CONG_DEF, readKernel(TCP_CONGS)).apply()

        // Memory
        val swappiness = globalConfig.getString(SpfConfig.SWAPPINESS, "")
        val dirtyRatio = globalConfig.getString(SpfConfig.DIRTY_RATIO, "")
        val dirtyBackgroundRatio = globalConfig.getString(SpfConfig.DIRTY_BACK_RATIO, "")
        val dirtyWritebackCentisecs = globalConfig.getString(SpfConfig.DIRTY_WRITE_CENTISECS, "")
        val dirtyExpireCentisecs = globalConfig.getString(SpfConfig.DIRTY_EXPIRE_CENTISECS, "")
        if (swappiness!!.isNotEmpty())
            setKernel(swappiness, VM_SWAPPINESS)
        if (dirtyRatio!!.isNotEmpty())
            setKernel(dirtyRatio, VM_DIRTY_RATIO)
        if (dirtyBackgroundRatio!!.isNotEmpty())
            setKernel(dirtyBackgroundRatio, VM_DIRTY_BACK_RATIO)
        if (dirtyWritebackCentisecs!!.isNotEmpty())
            setKernel(dirtyWritebackCentisecs, VM_DIRTY_WRITE_CENTISECS)
        if (dirtyExpireCentisecs!!.isNotEmpty())
            setKernel(dirtyExpireCentisecs, VM_DIRTY_EXPIRE_CENTISECS)

        // Start DKM Service
        runSVCWorker(applicationContext, "")

        if (globalConfig.getBoolean(SpfConfig.SWAP_BOOT, false)) {
            val swapSize = globalConfig.getInt(SpfConfig.SWAP_SIZE, 0)
            val swapInfo = KeepShellPublic.doCmdSync("free -m | grep Swap")
            var swapTotal = 0
            if (swapInfo.contains("Swap")) {
                try {
                    val swapInfoStr =
                        swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
                    if (Regex("[\\d]+[\\s]+[\\d]+").matches(swapInfoStr)) {
                        swapTotal =
                            swapInfoStr.substring(0, swapInfoStr.indexOf(" ")).trim().toInt()
                    }
                } catch (_: java.lang.Exception) {
                }
            }
            val stop = Thread {
                if (swapTotal > 0) {
                    val timer = zramOffAwait(applicationContext)
                    SwapUtils().zramOff()
                    timer.cancel()
                    swapTotal = 0
                    swapRun = false
                }
            }
            val run = Thread {
                val timer: Timer
                if (swapTotal > 0) {
                    swapRun = true
                    timer = zramOffAwait(applicationContext)
                    SwapUtils().zramOff()
                    timer.cancel()
                }
                SwapUtils().resizeZram(
                    swapSize,
                    globalConfig.getString(SpfConfig.SWAP_ALGORITHM, SwapUtils().compAlgorithm)!!
                )
                swapRun = false
            }
            if (globalConfig.getBoolean(SpfConfig.SWAP_ENABLE, false) && swapSize > 0 && mbToGB(
                    swapSize
                ) != mbToGB(swapTotal)
            )
                Thread(run).start()
            else if (!globalConfig.getBoolean(SpfConfig.SWAP_ENABLE, false) && swapTotal > 0)
                Thread(stop).start()
        }

        return Result.success()
    }

    private fun zramOffAwait(context: Context): Timer {
        val timer = Timer()
        val totalUsed = SwapUtils().zramUsedSize
        val startTime = System.currentTimeMillis()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentUsed = SwapUtils().zramUsedSize
                val avgSpeed = (totalUsed - currentUsed).toFloat() / (System.currentTimeMillis() - startTime) * 1000
                val tipStr = StringBuilder()
                tipStr.append(
                    String.format("${context.getString(R.string.ram_recycle)} ${currentUsed}/${totalUsed}MB (%.1fMB/s).", avgSpeed) +
                            if (avgSpeed > 0)
                                " ${context.getString(R.string.ram_approximately)} " + (currentUsed / avgSpeed).toInt() + context.getString(R.string.second)
                            else
                                " ${context.getString(R.string.please_wait)}~"
                )
                CMD_MSG = tipStr.toString()
            }
        }, 0, 1000)
        return timer
    }
}