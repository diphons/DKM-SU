package com.diphons.dkmsu.ui.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.util.checkPermission
import com.diphons.dkmsu.ui.util.grantPermissions

class PermWorker(context : Context, params : WorkerParameters) : Worker(context,params) {
    override fun doWork(): Result {
        if (!checkPermission(applicationContext))
            grantPermissions(applicationContext)
        return Result.success()
    }
}