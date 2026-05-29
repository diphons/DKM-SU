package com.diphons.dkmsu.ui.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.ADRENO_BOOST
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_IDLE_BIG
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_IDLE_LITTLE
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_IDLE_PRIME
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_MIN_BIG
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_MIN_LITTLE
import com.diphons.dkmsu.ui.util.Utils.CPU_FREQ_MIN_PRIME
import com.diphons.dkmsu.ui.util.Utils.CPU_GOV
import com.diphons.dkmsu.ui.util.Utils.CPU_INFO_MIN
import com.diphons.dkmsu.ui.util.Utils.CPU_MAX_FREQ
import com.diphons.dkmsu.ui.util.Utils.CPU_MIN_FREQ
import com.diphons.dkmsu.ui.util.Utils.EARPIECE_GAIN
import com.diphons.dkmsu.ui.util.Utils.GPU_DEF_PWRLEVEL
import com.diphons.dkmsu.ui.util.Utils.GPU_GOV
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_CKL
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_FREQ
import com.diphons.dkmsu.ui.util.Utils.GPU_MAX_GPUCLK
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_CKL
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_FREQ
import com.diphons.dkmsu.ui.util.Utils.GPU_MIN_GPUCLK
import com.diphons.dkmsu.ui.util.Utils.HEADPHONE_GAIN
import com.diphons.dkmsu.ui.util.Utils.INPUT_BOOST_DURATION
import com.diphons.dkmsu.ui.util.Utils.INPUT_BOOST_FREQ_BIG
import com.diphons.dkmsu.ui.util.Utils.INPUT_BOOST_FREQ_LITTLE
import com.diphons.dkmsu.ui.util.Utils.INPUT_BOOST_FREQ_PRIME
import com.diphons.dkmsu.ui.util.Utils.MAX_BOOST_FREQ_BIG
import com.diphons.dkmsu.ui.util.Utils.MAX_BOOST_FREQ_LITTLE
import com.diphons.dkmsu.ui.util.Utils.MAX_BOOST_FREQ_PRIME
import com.diphons.dkmsu.ui.util.Utils.MICROPHONE_GAIN
import com.diphons.dkmsu.ui.util.Utils.OPROFILE
import com.diphons.dkmsu.ui.util.Utils.SOUND_CONTROL
import com.diphons.dkmsu.ui.util.Utils.TCP_CONGS
import com.diphons.dkmsu.ui.util.Utils.THERMAL_PROFILE
import com.diphons.dkmsu.ui.util.Utils.TOUCH_SAMPLE
import com.diphons.dkmsu.ui.util.Utils.TOUCH_SAMPLE_GOODIX
import com.diphons.dkmsu.ui.util.Utils.WAKE_BOOST_DURATION
import com.diphons.dkmsu.ui.util.Utils.strToInt
import com.diphons.dkmsu.ui.util.getDefCIB
import com.diphons.dkmsu.ui.util.getDefCPUGov
import com.diphons.dkmsu.ui.util.getDefCPUMaxFreq
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getDefThermalProfile
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.onFocusedApp
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.readKernelInt
import com.diphons.dkmsu.ui.util.setIOSched
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setRefreshRate

class ProfileWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences
    private lateinit var prefs: SharedPreferences

    @SuppressLint("SuspiciousIndentation")
    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val perf_mode = globalConfig.getBoolean(SpfConfig.PERF_MODE, true)
        val profile = globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)
        val gki_mode = globalConfig.getBoolean(SpfConfig.GKI_MODE, false)
        val list = globalConfig.getString(SpfConfig.PER_APP_LIST, "")
        val gameAi = globalConfig.getBoolean(SpfConfig.GAME_AI, false)
        if (gameAi || (list!!.isNotEmpty() && list.contains(onFocusedApp)))
            prefs = applicationContext.getSharedPreferences(onFocusedApp, Context.MODE_PRIVATE)
        else {
            prefs = applicationContext.getSharedPreferences(
                if (perf_mode) {
                    when (profile) {
                        1 -> SpfProfile.PERFORMANCE
                        2 -> SpfProfile.GAME
                        4 -> SpfProfile.BATTERY
                        else -> SpfProfile.BALANCE
                    }
                } else
                    SpfProfile.NONE,
                Context.MODE_PRIVATE
            )
        }
        val defSpf = applicationContext.getSharedPreferences("cib", Context.MODE_PRIVATE)

        getDefCIB(defSpf)
        if (!gameAi) {
            // OProfile
            if (hasModule(OPROFILE)) {
                if (perf_mode)
                    setKernel("${globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)}", OPROFILE)
                else
                    setKernel("1", OPROFILE)
            }

            //Thermal
            if (hasModule(THERMAL_PROFILE)) {
                val checkThermal = readKernel(THERMAL_PROFILE)
                if (checkThermal.contains("8"))
                    setKernel(0, THERMAL_PROFILE, true)
                setKernel(
                    "${prefs.getInt("thermal", getDefThermalProfile(profile, gki_mode))}",
                    THERMAL_PROFILE,
                    true
                )
            }

            // CpuFreq
            for (i in 1..getMaxCluster(applicationContext)) {
                if (hasModule(i, CPU_MAX_FREQ)) {
                    setKernel(
                        "${
                            prefs.getInt(
                                "cpu${i}_max_freq",
                                getDefCPUMaxFreq(applicationContext, profile, i)
                            )
                        }",
                        i,
                        CPU_MAX_FREQ
                    )
                    setKernel(
                        "${
                            prefs.getInt(
                                "cpu${i}_min_freq",
                                readKernelInt(applicationContext, i, CPU_INFO_MIN)
                            )
                        }",
                        i,
                        CPU_MIN_FREQ
                    )
                    setKernel(
                        "${
                            prefs.getString(
                                "cpu${i}_gov",
                                getDefCPUGov(applicationContext, profile, i)
                            )
                        }",
                        i,
                        CPU_GOV
                    )
                }
            }
        }
        //GPU
        val gpu_mx_freq = "${prefs.getInt("gpu_max_freq", getGPUMaxFreq(applicationContext))}"
        val gpu_mn_freq = "${prefs.getInt("gpu_min_freq", getGPUMinFreq(applicationContext))}"
        setKernel(gpu_mx_freq, "${getGPUPath(applicationContext)}/$GPU_MAX_FREQ")
        setKernel(gpu_mx_freq, "${getGPUPath(applicationContext)}/$GPU_MAX_GPUCLK")
        setKernel(
            "${strToInt(gpu_mx_freq) / 1000000}",
            "${getGPUPath(applicationContext)}/$GPU_MAX_CKL"
        )
        setKernel(gpu_mn_freq, "${getGPUPath(applicationContext)}/$GPU_MIN_FREQ")
        setKernel(gpu_mn_freq, "${getGPUPath(applicationContext)}/$GPU_MIN_GPUCLK")
        setKernel(
            "${strToInt(gpu_mn_freq) / 1000000}",
            "${getGPUPath(applicationContext)}/$GPU_MIN_CKL"
        )
        setKernel(
            "${
                prefs.getString(
                    "gpu_gov",
                    readKernel(getGPUPath(applicationContext), GPU_GOV)
                )
            }", "${getGPUPath(applicationContext)}/$GPU_GOV"
        )
        setKernel(
            "${prefs.getInt("gpu_def_power", gpuMaxPwrLevel(applicationContext))}",
            "${getGPUPath(applicationContext)}/$GPU_DEF_PWRLEVEL"
        )
        if (!gameAi) {
            setKernel(
                "${prefs.getInt("gpu_adreno_boost", 0)}",
                "${getGPUPath(applicationContext)}/$ADRENO_BOOST"
            )
        }

        //IO
        setIOSched("${prefs.getString("io_sched", getDefIOSched())}")

        //TCP Congestion
        var tcpDef = globalConfig.getString(SpfConfig.TCP_CONG_DEF, "")
        if (tcpDef!!.isEmpty())
            tcpDef = readKernel(TCP_CONGS)
        setKernel("${prefs.getString("tcp", tcpDef)}", TCP_CONGS)

        if (list!!.isNotEmpty()) {
            val preferences: SharedPreferences = if (!list.contains(onFocusedApp)) {
                globalConfig
            } else
                prefs

            // Touch Improve
            if (hasModule(TOUCH_SAMPLE_GOODIX))
                setKernel(if (preferences.getBoolean(SpfConfig.TOUCH_SAMPLE, false)) "1" else "0", TOUCH_SAMPLE_GOODIX)
            else if (hasModule(TOUCH_SAMPLE))
                setKernel(if (preferences.getBoolean(SpfConfig.TOUCH_SAMPLE, false)) "1" else "0", TOUCH_SAMPLE)

            // Sound Control
            if (hasModule(SOUND_CONTROL)) {
                setKernel("${(preferences.getFloat(SpfConfig.SC_MICROFON, 0f) * 100).toInt() * 20 / 100}", MICROPHONE_GAIN)
                setKernel("${(preferences.getFloat(SpfConfig.SC_EARFON, 0f) * 100).toInt() * 20 / 100}", EARPIECE_GAIN)
                setKernel("${(preferences.getFloat(SpfConfig.SC_HEADFON, 0f) * 100).toInt() * 20 / 100} ${(preferences.getFloat(SpfConfig.SC_HEADFON2, 0f) * 100).toInt() * 20 / 100}", HEADPHONE_GAIN)
            }

            if (getFPSCount() > 1) {
                if (!preferences.getString(SpfConfig.REFRESH_RATE, "Default")!!.contains("Default") && preferences.getString(SpfConfig.REFRESH_RATE, "Default")!!.isNotEmpty())
                setRefreshRate(strToInt(preferences.getString(SpfConfig.REFRESH_RATE, "60")))
            }

            // Cpu Input Boost
            if (!defSpf.all.isEmpty()) {
                val primeCpu = getMaxCluster(applicationContext) > 2
                if (hasModule(CPU_FREQ_IDLE_LITTLE)) {
                    val cpu_freq_idle_big = prefs.getInt(
                        "cpu_freq_idle_big",
                        defSpf.getInt("cpu_freq_idle_big", strToInt(readKernel(CPU_FREQ_IDLE_BIG)))
                    )
                    val cpu_freq_idle_little = prefs.getInt(
                        "cpu_freq_idle_little",
                        defSpf.getInt(
                            "cpu_freq_idle_little",
                            strToInt(readKernel(CPU_FREQ_IDLE_LITTLE))
                        )
                    )
                    if (primeCpu) {
                        val cpu_freq_idle_prime = prefs.getInt(
                            "cpu_freq_idle_prime",
                            defSpf.getInt(
                                "cpu_freq_idle_prime",
                                strToInt(readKernel(CPU_FREQ_IDLE_PRIME))
                            )
                        )
                        setKernel("$cpu_freq_idle_prime", CPU_FREQ_IDLE_PRIME)
                    }
                    setKernel("$cpu_freq_idle_big", CPU_FREQ_IDLE_BIG)
                    setKernel("$cpu_freq_idle_little", CPU_FREQ_IDLE_LITTLE)
                }

                if (hasModule(CPU_FREQ_MIN_LITTLE)) {
                    val cpu_freq_min_big = prefs.getInt(
                        "cpu_freq_min_big",
                        defSpf.getInt("cpu_freq_min_big", strToInt(readKernel(CPU_FREQ_MIN_BIG)))
                    )
                    val cpu_freq_min_little = prefs.getInt(
                        "cpu_freq_min_little",
                        defSpf.getInt(
                            "cpu_freq_min_little",
                            strToInt(readKernel(CPU_FREQ_MIN_LITTLE))
                        )
                    )
                    if (primeCpu) {
                        val cpu_freq_min_prime = prefs.getInt(
                            "cpu_freq_min_prime",
                            defSpf.getInt(
                                "cpu_freq_min_prime",
                                strToInt(readKernel(CPU_FREQ_MIN_PRIME))
                            )
                        )
                        setKernel("$cpu_freq_min_prime", CPU_FREQ_MIN_PRIME)
                    }
                    setKernel("$cpu_freq_min_big", CPU_FREQ_MIN_BIG)
                    setKernel("$cpu_freq_min_little", CPU_FREQ_MIN_LITTLE)
                }
                val input_boost_duration = prefs.getInt("input_boost_duration", defSpf.getInt("input_boost_duration", strToInt(readKernel(INPUT_BOOST_DURATION))))
                setKernel("$input_boost_duration", INPUT_BOOST_DURATION)

                if (hasModule(INPUT_BOOST_FREQ_LITTLE)) {
                    val input_boost_freq_big = prefs.getInt(
                        "input_boost_freq_big",
                        defSpf.getInt(
                            "input_boost_freq_big",
                            strToInt(readKernel(INPUT_BOOST_FREQ_BIG))
                        )
                    )
                    val input_boost_freq_little = prefs.getInt(
                        "input_boost_freq_little",
                        defSpf.getInt(
                            "input_boost_freq_little",
                            strToInt(readKernel(INPUT_BOOST_FREQ_LITTLE))
                        )
                    )
                    if (primeCpu) {
                        val input_boost_freq_prime = prefs.getInt(
                            "input_boost_freq_prime",
                            defSpf.getInt(
                                "input_boost_freq_prime",
                                strToInt(readKernel(INPUT_BOOST_FREQ_PRIME))
                            )
                        )
                        setKernel("$input_boost_freq_prime", INPUT_BOOST_FREQ_PRIME)
                    }
                    setKernel("$input_boost_freq_big", INPUT_BOOST_FREQ_BIG)
                    setKernel("$input_boost_freq_little", INPUT_BOOST_FREQ_LITTLE)
                }
                if (hasModule(MAX_BOOST_FREQ_LITTLE)) {
                    val max_boost_freq_big = prefs.getInt(
                        "max_boost_freq_big",
                        defSpf.getInt(
                            "max_boost_freq_big",
                            strToInt(readKernel(MAX_BOOST_FREQ_BIG))
                        )
                    )
                    val max_boost_freq_little = prefs.getInt(
                        "max_boost_freq_little",
                        defSpf.getInt(
                            "max_boost_freq_little",
                            strToInt(readKernel(MAX_BOOST_FREQ_LITTLE))
                        )
                    )
                    if (primeCpu) {
                        val max_boost_freq_prime = prefs.getInt(
                            "max_boost_freq_prime",
                            defSpf.getInt(
                                "max_boost_freq_prime",
                                strToInt(readKernel(MAX_BOOST_FREQ_PRIME))
                            )
                        )
                        setKernel("$max_boost_freq_prime", MAX_BOOST_FREQ_PRIME)
                    }
                    setKernel("$max_boost_freq_big", MAX_BOOST_FREQ_BIG)
                    setKernel("$max_boost_freq_little", MAX_BOOST_FREQ_LITTLE)
                }
                if (hasModule(WAKE_BOOST_DURATION)) {
                    val wake_boost_duration = prefs.getInt(
                        "wake_boost_duration",
                        defSpf.getInt(
                            "wake_boost_duration",
                            strToInt(readKernel(WAKE_BOOST_DURATION))
                        )
                    )
                    setKernel("$wake_boost_duration", WAKE_BOOST_DURATION)
                }
            }
        }

        return Result.success()
    }
}