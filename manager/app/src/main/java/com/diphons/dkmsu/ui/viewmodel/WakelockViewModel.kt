package com.diphons.dkmsu.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.diphons.dkmsu.ui.util.HanziToPinyin
import com.diphons.dkmsu.ui.util.getWakelockList
import com.diphons.dkmsu.ui.util.overlayFsAvailable
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

class WakelockViewModel : ViewModel() {

    companion object {
        private const val TAG = "WakelockViewModel"
        private var wakelock by mutableStateOf<List<WakelockInfo>>(emptyList())
    }

    data class WakelockInfo(
        val id: String,
        val name: String,
        val active: Int,
        val abort: Int,
        val times: Int,
        val block: Int,
    )

    var isOverlayAvailable by mutableStateOf(overlayFsAvailable())
        private set

    var isRefreshing by mutableStateOf(false)
        private set
    var search by mutableStateOf("")

    var sortAToZ by mutableStateOf(false)
    var sortZToA by mutableStateOf(false)

    val wakelockList by derivedStateOf {
        val comparator = when {
            sortAToZ -> compareBy<WakelockInfo> { it.name.lowercase() }
            else -> compareByDescending<WakelockInfo> { it.name.lowercase() }
        }.thenBy(Collator.getInstance(Locale.getDefault()), WakelockInfo::name)
        wakelock.filter {
            it.name.contains(search, true) || it.name.contains(search, true) || HanziToPinyin.getInstance()
                .toPinyinString(it.name).contains(search, true)
        }.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    fun emptyWakelock(){
        wakelock = emptyList()
    }
    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isRefreshing = true
    }

    fun fetchWakelockList() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true

            val oldWakelockList = wakelock

            val start = SystemClock.elapsedRealtime()

            kotlin.runCatching {
                isOverlayAvailable = overlayFsAvailable()

                val result = getWakelockList()

                Log.i(TAG, "result: $result")

                val array = JSONArray(result)
                wakelock = (0 until array.length())
                    .asSequence()
                    .map { array.getJSONObject(it) }
                    .map { obj ->
                        WakelockInfo(
                            obj.getString("id"),
                            obj.getString("name"),
                            obj.optInt("active"),
                            obj.optInt("abort"),
                            obj.optInt("times"),
                            obj.optInt("block"),
                        )
                    }.toList()
                isNeedRefresh = false
            }.onFailure { e ->
                Log.e(TAG, "fetchWakelockList: ", e)
                isRefreshing = false
            }

            // when both old and new is kotlin.collections.EmptyList
            // wokelockList update will don't trigger
            if (oldWakelockList === wakelock) {
                isRefreshing = false
            }

            Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, wakelocks: $wakelock")
        }
    }
}