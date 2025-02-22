package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.BOEFFLA_WL_BLOCKER
import com.diphons.dkmsu.ui.util.Utils.CHG_CUR_MAX
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_BLUE
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_CONTRAST
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_GREEN
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_HUE
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_RED
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_SATURATED
import com.diphons.dkmsu.ui.util.Utils.DISPLAY_VALUE
import com.diphons.dkmsu.ui.util.Utils.DT2W
import com.diphons.dkmsu.ui.util.Utils.DT2W_LEGACY
import com.diphons.dkmsu.ui.util.Utils.DYNAMIC_CHARGING
import com.diphons.dkmsu.ui.util.Utils.EARPIECE_GAIN
import com.diphons.dkmsu.ui.util.Utils.FCHG_SYS
import com.diphons.dkmsu.ui.util.Utils.GAME_AI
import com.diphons.dkmsu.ui.util.Utils.GAME_AI_LIST_DEFAULT
import com.diphons.dkmsu.ui.util.Utils.GAME_AI_LOG
import com.diphons.dkmsu.ui.util.Utils.HAPTIC_LEVEL
import com.diphons.dkmsu.ui.util.Utils.HEADPHONE_GAIN
import com.diphons.dkmsu.ui.util.Utils.MICROPHONE_GAIN
import com.diphons.dkmsu.ui.util.Utils.NIGHT_CHARGING
import com.diphons.dkmsu.ui.util.Utils.SKIP_THERMAL
import com.diphons.dkmsu.ui.util.Utils.SOUND_CONTROL
import com.diphons.dkmsu.ui.util.Utils.STEP_CHARGING
import com.diphons.dkmsu.ui.util.Utils.TOUCH_SAMPLE
import com.diphons.dkmsu.ui.util.getCurrentCharger
import com.diphons.dkmsu.ui.util.getDefDT2W
import com.diphons.dkmsu.ui.util.getKNVersion
import com.diphons.dkmsu.ui.util.getXMPath
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.hasXiaomiDevice
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.diphons.dkmsu.ui.util.setGameList
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setProfile
import com.diphons.dkmsu.ui.util.setWLBlocker
import com.diphons.dkmsu.ui.util.setXiaomiTouch

class BootWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences
    private lateinit var gameai_prefs: SharedPreferences

    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        gameai_prefs = applicationContext.getSharedPreferences("game_ai", Context.MODE_PRIVATE)

        KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist +${applicationContext.packageName}")

        if (globalConfig.getBoolean(SpfConfig.SPOOF_ENCRYPT, false))
            RootUtils.runCommand("resetprop ro.crypto.state encrypted")

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
        // Start DKM Service
        runSVCWorker(applicationContext, "")

        return Result.success()
    }
}