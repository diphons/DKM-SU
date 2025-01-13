package com.diphons.dkmsu.ui.services

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.svcInit
import com.diphons.dkmsu.ui.util.spoofKernel
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.getStartedSVC
import com.diphons.dkmsu.ui.util.setSVCCMD
import com.diphons.dkmsu.ui.util.startSVC

class SVCWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    private lateinit var globalConfig: SharedPreferences

    override fun doWork(): Result {
        globalConfig = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val spoof_kernel = globalConfig.getString(SpfConfig.SPOOF_KERNEL, "")
        val susfs_log = globalConfig.getBoolean(SpfConfig.SUSFS_LOG, true)

        if (!getStartedSVC())
            startSVC()

        if (svcInit.contains("susfs9")) {
            if ("$spoof_kernel".isNotEmpty() && !"$spoof_kernel".contains(" "))
                spoofKernel("$spoof_kernel")
            setSuSFSLog(susfs_log)
            setSVCCMD(svcInit)
        } else if (svcInit.contains("susfs1")) {
            if ("$spoof_kernel".isNotEmpty() && !"$spoof_kernel".contains(" "))
                spoofKernel("$spoof_kernel")
            else
                spoofKernel("default")
        } else if (svcInit.contains("susfs2")) {
            setSuSFSLog(susfs_log)
        } else {
            setSVCCMD(svcInit)
        }

        // Reset
        svcInit = ""

        return Result.success()
    }
}