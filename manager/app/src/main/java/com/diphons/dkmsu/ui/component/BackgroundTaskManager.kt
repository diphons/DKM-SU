package com.diphons.dkmsu.ui.component

import kotlinx.coroutines.*

class BackgroundTaskManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun <T> execute(
        block: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Job {
        return scope.launch {
            try {
                val result = block()
                withContext(Dispatchers.Main) {
                    onSuccess(result)
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }
}