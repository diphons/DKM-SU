package com.diphons.dkmsu.ui.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.AV_INTERNET
import com.diphons.dkmsu.ui.util.Utils.CMD_MSG
import com.diphons.dkmsu.ui.util.Utils.PIF_UPDATER

class PIFWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    var countMax = 0

    fun read_pif(context: Context){
        countMax += 1
        if (countMax < 100) {
            CMD_MSG =
                RootUtils.runAndGetOutput("cat /data/data/${context.packageName}/files/pif.txt")

            if (!CMD_MSG.contains("Your PIF"))
                read_pif(context)
        }
    }

    override fun doWork(): Result {
        CMD_MSG = ""
        RootUtils.runCommand("setprop init.dkmsvc.cmd 'pif'")
        //CMD_MSG = RootUtils.runAndGetOutput("su -c $PIF_UPDATER")

        countMax = 0
        read_pif(applicationContext)
        return Result.success()
    }
}