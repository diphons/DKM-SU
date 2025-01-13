package com.diphons.dkmsu.ui.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.CMD_MSG
import com.diphons.dkmsu.ui.util.Utils.PIF_UPDATER

class PIFWorker(context : Context, params : WorkerParameters) : Worker(context,params) {

    override fun doWork(): Result {
        CMD_MSG = RootUtils.runAndGetOutput("su -c $PIF_UPDATER")

        return Result.success()
    }
}