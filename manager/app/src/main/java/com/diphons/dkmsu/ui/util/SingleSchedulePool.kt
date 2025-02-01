package com.diphons.dkmsu.ui.util

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SingleSchedulePool {
    val executorService = Executors.newScheduledThreadPool(1)
    var future: ScheduledFuture<*>? = null

    fun delay(delay: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, body: (() -> Unit)) {
        future?.cancel(false)

        val runnable = Runnable { body() }

        future = executorService.schedule(runnable, delay, timeUnit)
    }

    fun shutdown() = executorService.shutdown()
}