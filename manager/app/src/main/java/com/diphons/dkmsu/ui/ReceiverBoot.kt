package com.diphons.dkmsu.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.diphons.dkmsu.ui.services.BootWorker

class ReceiverBoot : BroadcastReceiver() {
    companion object {
        var bootCompleted: Boolean = false
    }
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        if (bootCompleted) {
            return
        }
        bootCompleted = true

        val startBootWorker = OneTimeWorkRequest.Builder(BootWorker::class.java)
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(startBootWorker)
    }
}
