package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.ADRENO_BOOST
import com.diphons.dkmsu.ui.util.Utils.CPU_GOV
import com.diphons.dkmsu.ui.util.Utils.CPU_INFO_MIN
import com.diphons.dkmsu.ui.util.Utils.CPU_MAX_FREQ
import com.diphons.dkmsu.ui.util.Utils.CPU_MIN_FREQ
import com.diphons.dkmsu.ui.util.Utils.GPU_DEF_PWRLEVEL
import com.diphons.dkmsu.ui.util.Utils.GPU_GOV
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_CKL
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_FREQ
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_GPUCLK
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_CKL
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_FREQ
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_GPUCLK
import com.diphons.dkmsu.ui.util.Utils.OPROFILE
import com.diphons.dkmsu.ui.util.Utils.THERMAL_PROFILE
import com.diphons.dkmsu.ui.util.Utils.strToInt
import com.diphons.dkmsu.ui.util.getDefCPUGov
import com.diphons.dkmsu.ui.util.getDefCPUMaxFreq
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getDefThermalProfile
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.readKernelInt
import com.diphons.dkmsu.ui.util.setIOSched
import com.diphons.dkmsu.ui.util.setKernel

class ProfileWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences

    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val perf_mode = globalConfig.getBoolean(SpfConfig.PERF_MODE, true)
        val profile = globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)
        val prefs = applicationContext.getSharedPreferences(
            if (perf_mode) {
                if (profile == 1)
                    SpfProfile.PERFORMANCE
                else if (profile == 2)
                    SpfProfile.GAME
                else if (profile == 4)
                    SpfProfile.BATTERY
                else
                    SpfProfile.BALANCE
            } else
                SpfProfile.NONE,
            Context.MODE_PRIVATE
        )

        // OProfile
        if (hasModule(OPROFILE)) {
            if (perf_mode)
                setKernel("${globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)}", OPROFILE)
            else
                setKernel("1", OPROFILE)
        }

        // CpuFreq
        for (i in 1..getMaxCluster(applicationContext)) {
            if (hasModule(i, CPU_MAX_FREQ)) {
                setKernel(
                    "${prefs.getInt("cpu${i}_max_freq", getDefCPUMaxFreq(applicationContext, profile, i))}",
                    i,
                    CPU_MAX_FREQ
                )
                setKernel(
                    "${prefs.getInt("cpu${i}_min_freq", readKernelInt(applicationContext, i, CPU_INFO_MIN))}",
                    i,
                    CPU_MIN_FREQ
                )
                setKernel(
                    "${prefs.getString("cpu${i}_gov", getDefCPUGov(applicationContext, profile, i))}",
                    i,
                    CPU_GOV
                )
            }
        }
        //GPU
        val gpu_mx_freq = "${prefs.getInt("gpu_max_freq", getGPUMaxFreq(applicationContext))}"
        val gpu_mn_freq = "${prefs.getInt("gpu_max_freq", getGPUMinFreq(applicationContext))}"
        setKernel(gpu_mx_freq, "${getGPUPath(applicationContext)}/$GPU_MAX_FREQ")
        setKernel(gpu_mx_freq, "${getGPUPath(applicationContext)}/$GPU_MAX_GPUCLK")
        setKernel("${strToInt(gpu_mx_freq) / 1000000}", "${getGPUPath(applicationContext)}/$GPU_MAX_CKL")
        setKernel(gpu_mn_freq, "${getGPUPath(applicationContext)}/$GPU_MIN_FREQ")
        setKernel(gpu_mn_freq, "${getGPUPath(applicationContext)}/$GPU_MIN_GPUCLK")
        setKernel("${strToInt(gpu_mn_freq) / 1000000}", "${getGPUPath(applicationContext)}/$GPU_MIN_CKL")
        setKernel("${prefs.getString("gpu_gov", readKernel(getGPUPath(applicationContext), GPU_GOV))}", "${getGPUPath(applicationContext)}/$GPU_GOV")
        setKernel("${prefs.getInt("gpu_def_power", gpuMaxPwrLevel(applicationContext))}", "${getGPUPath(applicationContext)}/$GPU_DEF_PWRLEVEL")
        setKernel("${prefs.getInt("gpu_adreno_boost", 0)}", "${getGPUPath(applicationContext)}/$ADRENO_BOOST")

        //Thermal
        if (hasModule(THERMAL_PROFILE)) {
            val check_thermal = readKernel(THERMAL_PROFILE)
            if (check_thermal.contains("8"))
                setKernel(0, THERMAL_PROFILE, true)
            setKernel(
                "${prefs.getInt("thermal", getDefThermalProfile(profile))}",
                THERMAL_PROFILE,
                true
            )
        }

        //IO
        setIOSched("${prefs.getString("io_sched", getDefIOSched())}")

        return Result.success()
    }
}