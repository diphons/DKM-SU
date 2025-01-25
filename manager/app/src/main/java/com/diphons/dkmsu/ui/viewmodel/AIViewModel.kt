package com.diphons.dkmsu.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.IBinder
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.diphons.dkmsu.IKsuInterface
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.KsuService
import com.diphons.dkmsu.ui.util.HanziToPinyin
import com.diphons.dkmsu.ui.util.KsuCli
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.getGameAllList
import com.diphons.dkmsu.ui.util.getGameList
import com.diphons.dkmsu.ui.util.reloadData
import java.text.Collator
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AIViewModel : ViewModel() {
    private lateinit var prefs: SharedPreferences

    var refreshOnReturn by mutableStateOf(false)
        public set

    companion object {
        private const val TAG = "AIViewModel"
        private var apps by mutableStateOf<List<AppInfo>>(emptyList())
    }

    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val gameai: Boolean,
        val game: Boolean,
        val op: String,
        val gov: String,
        val t: String,
        val gpu: String
    ) : Parcelable {
        val packageName: String
            get() = packageInfo.packageName
        val uid: Int
            get() = packageInfo.applicationInfo!!.uid
        val gameAi: Boolean
            get() = gameai
        val gameMode: Boolean
            get() = game
        val oprofile: String
            get () = op
        val governor: String
            get () = gov
        val thermal: String
            get () = t
        val adreno: String
            get () = gpu
    }

    var search by mutableStateOf("")
    var showSystemApps by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
        private set

    private val sortedList by derivedStateOf {
        val comparator = compareBy<AppInfo> {
            when {
                it.gameAi -> 0
                it.oprofile.isNotEmpty() || it.governor.isNotEmpty() || it.thermal.isNotEmpty() || it.adreno.isNotEmpty() -> 1
                else -> 2
            }
        }.then(compareBy(Collator.getInstance(Locale.getDefault()), AppInfo::label))
        apps.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    val appList by derivedStateOf {
        sortedList.filter {
            it.label.contains(search, true) || it.packageName.contains(
                search,
                true
            ) || HanziToPinyin.getInstance()
                .toPinyinString(it.label).contains(search, true)
        }.filter {
            it.uid == 2000 // Always show shell
                    || showSystemApps || it.packageInfo.applicationInfo!!.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    private suspend inline fun connectKsuService(
        crossinline onDisconnect: () -> Unit = {}
    ): Pair<IBinder, ServiceConnection> = suspendCoroutine {
        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                onDisconnect()
            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                it.resume(binder as IBinder to this)
            }
        }

        val intent = Intent(ksuApp, KsuService::class.java)

        val task = KsuService.bindOrTask(
            intent,
            Shell.EXECUTOR,
            connection,
        )
        val shell = KsuCli.SHELL
        task?.let { it1 -> shell.execTask(it1) }
    }

    private fun stopKsuService() {
        val intent = Intent(ksuApp, KsuService::class.java)
        KsuService.stop(intent)
    }

    suspend fun fetchAppList() {

        isRefreshing = true

        val result = connectKsuService {
            Log.w(TAG, "KsuService disconnected")
        }

        withContext(Dispatchers.IO) {
            val pm = ksuApp.packageManager
            val start = SystemClock.elapsedRealtime()
            val binder = result.first
            val allPackages = IKsuInterface.Stub.asInterface(binder).getPackages(0)

            withContext(Dispatchers.Main) {
                stopKsuService()
            }

            val packages = allPackages.list

            apps = packages.map {
                val appInfo = it.applicationInfo
                val uid = appInfo!!.uid
                if (reloadData) {
                    reloadData = false
                    clearEndData(ksuApp.applicationContext)
                }
                val gameAi = getGameAllList(ksuApp.applicationContext).contains(it.packageName)
                var getlist = ""
                if (gameAi) {
                    getlist = getGameList(ksuApp.applicationContext, it.packageName)
                }
                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = it,
                    gameai = gameAi,
                    game = parserGameList(getlist, "gm").contains("1"),
                    op = parserGameList(getlist, "op"),
                    gov = parserGameList(getlist, "gov"),
                    t = parserGameList(getlist, "t"),
                    gpu = parserGameList(getlist, "gpu")
                )
            }.filter { it.packageName != ksuApp.packageName }
            Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}")
        }
    }
}
