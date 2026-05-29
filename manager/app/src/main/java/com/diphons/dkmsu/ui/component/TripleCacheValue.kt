package com.diphons.dkmsu.ui.component

import android.content.Context
import androidx.core.content.edit
import com.diphons.dkmsu.ui.store.SpfConfig

abstract class TripleCacheValue(private val context: Context, private val storageKey: String) {
    private var cache: String? = null
    private val value: String?
        get() {
            if (cache == null) {
                val storage = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
                if (!storage.contains(storageKey)) {
                    cache = initValue()
                    if (cache?.isNotEmpty() == true) {
                        storage.edit { putString(storageKey, cache) }
                    }
                } else {
                    cache = storage.getString(storageKey, "")
                }
            }
            return cache
        }

    abstract fun initValue(): String?

    override fun toString(): String {
        val value = this.value
        return value ?: ""
    }

    fun toInt(): Int {
        val value = this.value
        if (value.isNullOrEmpty()) {
            return 0
        }
        return value.toInt()
    }
}