package com.diphons.dkmsu.ui.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diphons.dkmsu.ui.component.ExtractAssets
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.BOEFFLA_WL_BLOCKER
import com.diphons.dkmsu.ui.util.getStartedSVC
import com.diphons.dkmsu.ui.util.getXMPath
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.hasXiaomiDevice
import com.diphons.dkmsu.ui.util.restartDkmSVC
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.diphons.dkmsu.ui.util.setWLBlocker

class ExtractAssetsWorker(context : Context, params : WorkerParameters) : Worker(context,params) {

    override fun doWork(): Result {
        restartDkmSVC(applicationContext)

        if (hasXiaomiDevice() && !hasModule(getXMPath(applicationContext))) {
            ExtractAssets(applicationContext).extractResources("xiaomi-touch")
            RootUtils.runCommand("find /data/data/${applicationContext.packageName}/files/xioami-touch -type f chmod 755")
        }

        if (hasModule(BOEFFLA_WL_BLOCKER)) {
            setWLBlocker(applicationContext)
        }

        if (!getStartedSVC())
            runSVCWorker(applicationContext, "")

        return Result.success()
    }
}