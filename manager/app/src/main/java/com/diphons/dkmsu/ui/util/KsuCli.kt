package com.diphons.dkmsu.ui.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.Service.NOTIFICATION_SERVICE
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.os.Process
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.diphons.dkmsu.BuildConfig
import com.diphons.dkmsu.KernelVersion
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.R
import com.diphons.dkmsu.getKernelVersion
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.services.ExtractAssetsWorker
import com.diphons.dkmsu.ui.services.PIFWorker
import com.diphons.dkmsu.ui.services.PermWorker
import com.diphons.dkmsu.ui.services.ProfileWorker
import com.diphons.dkmsu.ui.services.SVCWorker
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.store.*
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import java.io.File
import java.util.TreeMap

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"

private fun ksuDaemonMagicPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud_magic.so"
}

private fun ksuDaemonOverlayfsPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud_overlayfs.so"
}

// Get the path based on the user's choice
fun getKsuDaemonPath(): String {
    val prefs = ksuApp.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val useOverlayFs = prefs.getBoolean(SpfConfig.KSUD_MODE, false)

    return if (useOverlayFs) {
        ksuDaemonOverlayfsPath()
    } else {
        ksuDaemonMagicPath()
    }
}

object KsuCli {
    val SHELL: Shell = createRootShell()
    val GLOBAL_MNT_SHELL: Shell = createRootShell(true)
}

fun getRootShell(globalMnt: Boolean = false): Shell {
    return if (globalMnt) KsuCli.GLOBAL_MNT_SHELL else {
        KsuCli.SHELL
    }
}

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
}

fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    return try {
        if (globalMnt) {
            builder.build(getKsuDaemonPath(), "debug", "su", "-g")
        } else {
            builder.build(getKsuDaemonPath(), "debug", "su")
        }
    } catch (e: Throwable) {
        Log.w(TAG, "ksu failed: ", e)
        try {
            if (globalMnt) {
                builder.build("su")
            } else {
                builder.build("su", "-mm")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "su failed: ", e)
            builder.build("sh")
        }
    }
}

fun execKsud(args: String, newShell: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "${getKsuDaemonPath()} $args")
    }
}

fun install_dkmsvc() {
    val start = SystemClock.elapsedRealtime()
    val svcsusfs = File(ksuApp.applicationInfo.nativeLibraryDir, "libdkmsvc.so").absolutePath
    val result = execKsud("dkmsvc --dkmsvc $svcsusfs", true)
    Log.w(TAG, "install dkmsvc result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
}

fun install_susfs() {
    val start = SystemClock.elapsedRealtime()
    val susfs = File(ksuApp.applicationInfo.nativeLibraryDir, "libsusfs.so").absolutePath
    val result = execKsud("susfs --susfs $susfs", true)
    Log.w(TAG, "install susfs result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
}

fun install() {
    val start = SystemClock.elapsedRealtime()
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so").absolutePath
    val result = execKsud("install --magiskboot $magiskboot", true)
    Log.w(TAG, "install result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
	// Install susfs
	install_susfs()
    install_dkmsvc()
}

fun listModules(): String {
    val shell = getRootShell()

    val out =
        shell.newJob().add("${getKsuDaemonPath()} module list").to(ArrayList(), null).exec().out
    return out.joinToString("\n").ifBlank { "[]" }
}

fun getModuleCount(): Int {
    val result = listModules()
    runCatching {
        val array = JSONArray(result)
        return array.length()
    }.getOrElse { return 0 }
}

private fun getLibraryPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator
}

private fun getSuSFSPath(): String {
    return getLibraryPath() + "libsusfs.so"
}

fun getSuSFS(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} show support")
    return result
}

fun getSuSFSVersion(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} show version")
    return result
}

fun getSuSFSVariant(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} show variant")
    return result
}
fun getSuSFSFeatures(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} show enabled_features")
    return result
}

fun susfsSUS_SU_0(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} sus_su 0")
    return result
}

fun susfsSUS_SU_2(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} sus_su 2")
    return result
}

fun susfsSUS_SU_Mode(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "${getSuSFSPath()} sus_su show_working_mode")
    return result
}

/*
 * SVC Function
 * susfs1 - spoof kernel
 * susfs2 - susfs log
 * susfs3 - hide cusrom
 * susfs4 - hide gapps
 * susfs5 - hide revanced
 * susfs6 - hide cmdline
 * susfs7 - hide loop
 * susfs8 - hide lsposed
 * susfs9 - Set All (for set on boot)
 * susfsm - Set SUS Mount
 * susfsmr - Reset List SUS Mount
 * susfsu - Set TRY Umount
 * susfsur - Reset List TRY Umount
 * susfsp - Set SUS PATH
 * susfspr - Reset List SUS Path
 */
var svcInit: String = ""
fun getSVCVersion(): String{
    var result = RootUtils.runAndGetOutput("getprop init.dkmsvc.version")
    if (result.isEmpty())
        result = RootUtils.runAndGetOutput("dkmsvc version")
    return result
}

fun runSVCWorker(context: Context, value: String){
    svcInit = value
    val startSVCWK = OneTimeWorkRequest.Builder(SVCWorker::class.java)
        .build()
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(startSVCWK)
}

fun getSUSList(context: Context, filename: String): String{
    val path = "$DATA_DATA/${context.packageName}/files/susfs/${filename}.txt"
    if (hasModule(path)) {
        return readFile(path)
    } else {
        val result = if (filename.contains("sus_mount"))
            SUS_MOUNT()
        else if (filename.contains("try_umount"))
            TRY_UMOUNT()
        else
            SUS_PATH()
        return RootUtils.runAndGetOutput("echo \"$result\" > $path; cat $path")
    }
}

fun getSUSListReset(context: Context, filename: String): String{
    val path = "$DATA_DATA/${context.packageName}/files/susfs/${filename}.txt"
    val result = if (filename.contains("sus_mount"))
        SUS_MOUNT()
    else if (filename.contains("try_umount"))
        TRY_UMOUNT()
    else
       SUS_PATH()
    RootUtils.runCommand("echo \"$result\" > $path")
    return result
}

fun applySUSList(context: Context, value:String, filename: String){
    val path = "$DATA_DATA/${context.packageName}/files/susfs/${filename}.txt"
    RootUtils.runCommand("echo \"$value\" > $path")
}

fun spoofKernel(value: String) {
    RootUtils.runCommand("susfs set_uname $value 'default'")
}
fun getSpoofKnVersion(): String{
    var result = RootUtils.runAndGetOutput("uname -a")
    result = result.replace("Linux localhost ", "")
    return result
}
fun getKnVersion(): String{
    val result = RootUtils.runAndGetOutput("uname -v")
    return result
}

fun susfsEnableFeatures(): String{
    return  RootUtils.runAndGetOutput("getef=$(susfs show enabled_features); echo \"\$getef\" | sed 's/CONFIG_KSU_SUSFS_//g' | sed 's/_/ /g' | awk '{print \"·\", \$0}'")
}

fun hasModule(path: String): Boolean {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "if [[ -f $path ]] || [[ -d $path ]]; then echo 1;else echo 0;fi;")
    return result.equals("1")
}

fun readKernel(path: String, name: String): String {
    return if (name.isEmpty())
        RootUtils.runAndGetOutput("if [[ -f $path ]]; then cat $path;else echo \"\";fi;")
    else
        RootUtils.runAndGetOutput("if [[ -f $path/$name ]]; then cat $path/$name;else echo \"\";fi;")
}

fun setKernel(value: String, path: String, perm: Boolean) {
    if (perm)
        RootUtils.runCommand("chmod 644 $path;echo \"$value\" > $path; chmod 444 $path")
    else
        RootUtils.runCommand("chmod 644 $path;echo \"$value\" > $path")
}

fun setKernel(value: Int, path: String, perm: Boolean) {
    setKernel("$value", path, perm)
}

fun hasXiaomiDevice(): Boolean{
    val shell = getRootShell()
    var result = ShellUtils.fastCmd(shell, "resetprop | grep 'xiaomi'")
    return if (result.isEmpty()) {
        result = ShellUtils.fastCmd(shell, "resetprop | grep 'miui'")
        if (result.isEmpty()) {
            hasModule(TOUCH_DEV)
        } else
            true
    } else
        true
}

fun getBoard(context: Context): String {
    val prefs: SharedPreferences = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    var result = prefs.getString(SpfConfig.DEVICE_BOARD, "")
    if (result!!.isEmpty()) {
        result = RootUtils.runAndGetOutput("getprop ro.board.platform")
        prefs.edit().putString(SpfConfig.DEVICE_BOARD, result).apply()
    }
    return result
}

var getListFPS = ""
fun checkFPS(): String {
    if (getListFPS.isEmpty())
        getListFPS = RootUtils.runAndGetOutput("getfps=$(dumpsys display | grep 'DisplayDeviceInfo' | sed 's/, /\n/g' | grep 'fps' | sed 's/\n/ - /g' | sed 's/fps=//g'); resultfps=$(echo \"\$getfps\" | sort -n -r); echo \$resultfps")
    return getListFPS
}

fun getFPSCount(): Int{
    val result = RootUtils.runAndGetOutput("getfps=\$(dumpsys display | grep 'DisplayDeviceInfo' | sed 's/, /\n" +
            "/g' | grep 'fps' | sed 's/\n" +
            "/ - /g' | sed 's/fps=//g'); echo \"\$getfps\" | wc -l")
    if (result.isEmpty())
        return 1
    return strToInt(result)
}

fun getCurFPSID(refreshrate: String): Int{
    var result: String
    for (i in 1 .. getFPSCount()) {
        result = RootUtils.runAndGetOutput("echo ${checkFPS()} | awk '{print $$i}'")
        if (result.contains(refreshrate))
            return i
    }
    return 1
}

fun getfpsbyid(id: Int): String{
    var result = RootUtils.runAndGetOutput("echo ${checkFPS()} | awk '{print $$id}'")
    if (result.contains("."))
        result = result.substring(0, result.indexOf("."))
    return result
}

fun checkRefresRate(): String {
    var result: String
    if (hasModule(CEK_MIUI) || hasModule(CEK_MIUI2) || hasModule(CEK_MIUI3)) {
        result = RootUtils.runAndGetOutput("settings get system user_refresh_rate")
        if (result.isEmpty() || result.contains("null")) {
            result = RootUtils.runAndGetOutput("settings get system peak_refresh_rate")
        }
    } else {
        result = RootUtils.runAndGetOutput("settings get system peak_refresh_rate")
        if (result.isEmpty() || result.contains("null")) {
            result = RootUtils.runAndGetOutput("settings get system min_refresh_rate")
        }
        if (result.isEmpty() || result.contains("null")) {
            result = RootUtils.runAndGetOutput("settings get global oneplus_screen_refresh_rate")
        }
    }
    if (result.contains("."))
        result = result.substring(0, result.indexOf("."))

    if (result.isEmpty() || result.contains("null"))
        result="60"
    return result
}

fun checkRRMin(): String {
    val result = RootUtils.runAndGetOutput("settings get system min_refresh_rate")
    return result
}

fun setRefreshRate(value: Int){
    val shell = getRootShell()
    if (hasModule(CEK_MIUI) || hasModule(CEK_MIUI2) || hasModule(CEK_MIUI3)) {
        ShellUtils.fastCmd(shell, "settings put system user_refresh_rate $value")
    } else {
        ShellUtils.fastCmd(shell, "settings put system peak_refresh_rate $value")
        if (checkRRMin().isNotEmpty() || !checkRRMin().contains("null")) {
            ShellUtils.fastCmd(shell, "settings put system min_refresh_rate $value")
        }

        if (hasModule(CEK_OP) || hasModule(CEK_OP2) || hasModule(CEK_OP3)) {
            val valSet = when (value) {
                120 -> 3
                90 -> 0
                144 -> 2
                else -> 1
            }
            ShellUtils.fastCmd(shell, "settings put system oneplus_screen_refresh_rate $valSet")
        }
    }
}

fun getCurrentCharger(): Int {
    return strToInt(RootUtils.runAndGetOutput("cat $CHG_CUR_MAX"))
}

@SuppressLint("SdCardPath")
fun getXMPath(context: Context): String {
    return "/data/data/" + context.packageName + "/files/xiaomi-touch/xm-touch"
}

fun getStartedSVC(): Boolean{
    val result = RootUtils.runAndGetOutput("getprop init.dkmsvc")
    return !(result.isEmpty() || result.equals("0"))
}

fun getSVCCMD(): String{
    return RootUtils.runAndGetOutput("getprop init.dkmsvc.cmd")
}

fun setSVCCMD(value: String){
    var result = getSVCCMD()
    result = if (result.isNotEmpty())
        "$result-$value"
    else
        value
    RootUtils.runCommand("setprop init.dkmsvc.cmd \"$result\"")
}

fun startSVC(){
    RootUtils.runCommand("dkmsvc service")
}

fun setXiaomiTouch(active: Boolean, value: Int, context: Context){
    val shell = getRootShell()
    var setbol = "disable"
    var setbol2 = "0"
    val kernelVersion: KernelVersion = getKernelVersion()

    if (active) {
        setbol = "enable"
        setbol2 = "1"
    }
    if (kernelVersion.isGKI()) {
        ShellUtils.fastCmd(shell, "${getXMPath(context)}-gki --$setbol $value")
        ShellUtils.fastCmd(shell, "${getXMPath(context)}-gki-aosp --$setbol2 $value")
    } else {
        ShellUtils.fastCmd(shell, "${getXMPath(context)} --$setbol $value")
    }
}

var maxCluster: Int = 0
fun getMaxCluster(context: Context): Int {
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    if (maxCluster == 0) {
        var getCluster = prefs.getInt("cpu_cluster", 0)
        if (getCluster == 0) {
            getCluster =
                strToInt(RootUtils.runAndGetOutput("cpu=\$(ls $CPUFREQ | grep 'policy'); echo \"\$cpu\" | wc -l"))
            prefs.edit().putInt("cpu_cluster", getCluster).apply()
        }
        maxCluster = getCluster
    }
    return maxCluster
}

var gpuPath: String = ""
fun getGPUPath(context: Context): String {
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    if (gpuPath.isEmpty()) {
        var getPath = prefs.getString("gpu_path", "")
        if (getPath.toString().isEmpty()) {
            getPath = if (hasModule(GPU_MALI))
                GPU_MALI
            else
                GPU_ADRENO
            prefs.edit().putString("gpu_path", getPath).apply()
        }
        gpuPath = getPath.toString()
    }
    return gpuPath
}

fun readKernel(path: String): String {
    return readKernel(path, "")
}

fun readKernel(context: Context, cpu: Int, name: String): String {
    if (getMaxCluster(context) < cpu)
        return ""
    val result = RootUtils.runAndGetOutput("cpu=\$(ls $CPUFREQ | grep 'policy'); echo \$cpu | awk '{print $$cpu}'")
    return readKernel("$CPUFREQ/$result/$name")
}

fun readKernelInt(context: Context, cpu: Int, name: String): Int {
    if (getMaxCluster(context) < cpu)
        return 0
    val result = RootUtils.runAndGetOutput("cpu=\$(ls $CPUFREQ | grep 'policy'); echo \$cpu | awk '{print $$cpu}'")
    return strToInt(readKernel("$CPUFREQ/$result/$name"))
}

fun readKernelInt(path: String, name: String): Int {
    return strToInt(readKernel("$path/$name"))
}

fun setKernel(value: String, path: String) {
    setKernel(value, path, false)
}

fun setKernel(value: String, cpu: Int, name: String) {
    //val result = "$path/policy$cpu/$name"
    val result = RootUtils.runAndGetOutput("cpu=\$(ls $CPUFREQ | grep 'policy'); echo \$cpu | awk '{print $$cpu}'")
    setKernel(value, "$CPUFREQ/$result/$name", false)
}

fun setCPU(value: String, cpu: Int, mode: Int) {
    when (mode) {
        0 -> setKernel(value, cpu, CPU_MAX_FREQ)
        1 -> setKernel(value, cpu, CPU_MIN_FREQ)
        2 -> setKernel(value, cpu, CPU_GOV)
    }
}

fun setGPU(context: Context, value: String, mode: Int) {
    when (mode) {
        0 -> {
            setKernel(value, "${getGPUPath(context)}/$GPU_MAX_FREQ")
            setKernel(value, "${getGPUPath(context)}/$GPU_MAX_GPUCLK")
            setKernel("${strToInt(value) / 1000000}", "${getGPUPath(context)}/$GPU_MAX_CKL")
        }
        1 -> {
            setKernel(value, "${getGPUPath(context)}/$GPU_MIN_FREQ")
            setKernel(value, "${getGPUPath(context)}/$GPU_MIN_GPUCLK")
            setKernel("${strToInt(value) / 1000000}", "${getGPUPath(context)}/$GPU_MIN_CKL")
        }
        2 -> setKernel(value, "${getGPUPath(context)}/$GPU_GOV")
        3 -> setKernel(value, "${getGPUPath(context)}/$GPU_DEF_PWRLEVEL")
        4 -> setKernel(value, "${getGPUPath(context)}/$ADRENO_BOOST")
    }
}

fun hasModule(path: String, name: String): Boolean {
    return hasModule("$path/$name")
}

fun hasModule(cpu: Int, name: String): Boolean {
    val result = RootUtils.runAndGetOutput("cpu=$(ls $CPUFREQ | grep 'policy'); echo \$cpu | awk '{print $$cpu}'")
    return hasModule("$CPUFREQ/$result/$name")
}

fun get_cpu_av_freq2(cluster: String): String{
    var result: String
    val getMaxFreq = RootUtils.runAndGetOutput("cat $CPUFREQ/$cluster/$CPU_INFO_MAX")
    result = RootUtils.runAndGetOutput("cat $CPUFREQ/$cluster/$CPU_AV_FREQ")
    if (!result.contains(getMaxFreq))
        result = "$result $getMaxFreq"
    return result
}

fun get_cpu_av_freq(context: Context, cpu: Int): String{
    if (getMaxCluster(context) < cpu)
        return ""
    var result: String
    val getCluster = RootUtils.runAndGetOutput("cpu=$(ls $CPUFREQ | grep 'policy'); echo \$cpu | awk '{print $$cpu}'")
    if (hasModule("$CPUFREQ/$getCluster/$CPUFREQ_FULL_TABLE")) {
        result =
            RootUtils.runAndGetOutput("cpuavfreq=$(cat $CPUFREQ/$getCluster/$CPUFREQ_FULL_TABLE);parse0=$(echo \$cpuavfreq | sed 's/From : To : //g');parse1=\${parse0%%:*};parse2=\${parse1% *};echo \$parse2")
        if (result.isEmpty() || result.contains("too large"))
            result = get_cpu_av_freq2(getCluster)
    } else {
        result = get_cpu_av_freq2(getCluster)
    }
    return result
}

var cpuAvFreq1: String = ""
var cpuAvFreq2: String = ""
var cpuAvFreq3: String = ""
var cpuAvFreq4: String = ""
fun cpu_av_freq(context: Context, cpu: Int): String{
    val result: String
    when (cpu) {
        4 -> {
            if (cpuAvFreq4.isEmpty())
                cpuAvFreq4 = get_cpu_av_freq(context, cpu)
            result = cpuAvFreq4
        }
        3 -> {
            if (cpuAvFreq3.isEmpty())
                cpuAvFreq3 = get_cpu_av_freq(context, cpu)
            result = cpuAvFreq3
        }
        2 -> {
            if (cpuAvFreq2.isEmpty())
                cpuAvFreq2 = get_cpu_av_freq(context, cpu)
            result = cpuAvFreq2
        }
        else -> {
            if (cpuAvFreq1.isEmpty())
                cpuAvFreq1 = get_cpu_av_freq(context, cpu)
            result = cpuAvFreq1
        }
    }
    return result
}

var cpuAvGov: String = ""
fun cpu_av_gov(context: Context, cpu: Int): String{
    if (getMaxCluster(context) < cpu)
        return ""
    if (cpuAvGov.isEmpty())
        cpuAvGov = readKernel(context, cpu, CPU_AV_GOV)
    return cpuAvGov
}

fun getDefCPUGov(context: Context, profile: Int, cpu: Int): String{
    return if (profile == 1 || profile == 2 || profile == 3) {
        if (cpu_av_gov(context, cpu).contains("game"))
            "game"
        else if (cpu_av_gov(context, cpu).contains("game_walt"))
            "game_walt"
        else if (cpu_av_gov(context, cpu).contains("walt"))
            "walt"
        else
            "schedutil"
    } else
        "schedutil"
}

fun getCPUCurProfile(context: Context, cpu: Int, profile: Int): Int{
    val getValue = if (profile == 0 || profile == 4)
        RootUtils.runAndGetOutput("cpulist=\"${cpu_av_freq(context, cpu)}\";parse0=\${cpulist% *};parse1=\${parse0% *};parse2=\${parse1% *};parse3=\${parse2% *};parse4=\${parse3##* };echo \$parse4")
    else
        RootUtils.runAndGetOutput("cpulist=\"${cpu_av_freq(context, cpu)}\";parse0=\${cpulist% *};parse1=\${parse0##* };echo \$parse1")
    return if (getValue.isEmpty())
        0
    else
        strToInt(getValue)
}

fun parseMaxFreq(context: Context, cpu: Int): Int{
    val result = RootUtils.runAndGetOutput("cpulist=\"${cpu_av_freq(context, cpu)}\";parse=\${cpulist##* }; echo \$parse")
    if (result.isEmpty())
        return 0
    return strToInt(result)
}

var maxFreq1 = 0
var maxFreq2 = 0
var maxFreq3 = 0
var maxFreq4 = 0
fun getMaxFreq(context: Context, cpu: Int): Int{
    when (cpu) {
        4 -> {
            if (maxFreq4 == 0) {
                maxFreq4 = parseMaxFreq(context, cpu)
            }
            return maxFreq4
        }
        3 -> {
            if (maxFreq3 == 0) {
                maxFreq3 = parseMaxFreq(context, cpu)
            }
            return maxFreq3
        }
        2 -> {
            if (maxFreq2 == 0) {
                maxFreq2 = parseMaxFreq(context, cpu)
            }
            return maxFreq2
        }
        else -> {
            if (maxFreq1 == 0) {
                maxFreq1 = parseMaxFreq(context, cpu)
            }
            return maxFreq1
        }
    }
}

fun parseMinFreq(context: Context, cpu: Int): Int{
    val result = RootUtils.runAndGetOutput("cpulist=\"${cpu_av_freq(context, cpu)}\";parse=\${cpulist%% *}; echo \$parse")
    if (result.isEmpty())
        return 0
    return strToInt(result)
}

var minFreq1 = 0
var minFreq2 = 0
var minFreq3 = 0
var minFreq4 = 0
fun getMinFreq(context: Context, cpu: Int): Int{
    when (cpu) {
        4 -> {
            if (minFreq4 == 0) {
                minFreq4 = parseMinFreq(context, cpu)
            }
            return minFreq4
        }
        3 -> {
            if (minFreq3 == 0) {
                minFreq3 = parseMinFreq(context, cpu)
            }
            return minFreq3
        }
        2 -> {
            if (minFreq2 == 0) {
                minFreq2 = parseMinFreq(context, cpu)
            }
            return minFreq2
        }
        else -> {
            if (minFreq1 == 0) {
                minFreq1 = parseMinFreq(context, cpu)
            }
            return minFreq1
        }
    }
}

fun getDefCPUMaxFreq(context: Context, profile: Int, cpu: Int): Int{
    if (cpu > 1) {
        if (profile == 1 || profile == 3 || profile == 5) {
            return getMaxFreq(context, cpu)
        } else {
            return if (getMaxCluster(context) > 3) {
                if (cpu > 3)
                    getCPUCurProfile(context, cpu, profile)
                else
                    getMaxFreq(context, cpu)
            } else if (getMaxCluster(context) > 2) {
                if (cpu > 2)
                    getCPUCurProfile(context, cpu, profile)
                else
                    getMaxFreq(context, cpu)
            } else
                getCPUCurProfile(context, cpu, profile)
        }
    } else
        return getMaxFreq(context, cpu)
}

// Extract XiaomiTouch Assets
fun extractXTAssets(context: Context) {
    val startXTWorker = OneTimeWorkRequest.Builder(ExtractAssetsWorker::class.java)
        .build()
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(startXTWorker)
}

fun setProfile(context: Context, profile: Int){
    val preferences: SharedPreferences = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    preferences.edit().putInt(SpfConfig.PROFILE_MODE, profile).apply()
    runProfileWorker(context)
}

fun runProfileWorker(context: Context){
    val startSetProfile = OneTimeWorkRequest.Builder(ProfileWorker::class.java)
        .build()
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(startSetProfile)
}

fun isInternetAvailable(context: Context): Boolean {
    val result: Boolean
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
    result = when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
    AV_INTERNET = result
    return result
}

fun startPIFWorker(context: Context){
    val startPIF = OneTimeWorkRequest.Builder(PIFWorker::class.java)
        .build()
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(startPIF)
}

var gpuNumPwrLevel: Int = 0
fun gpuMaxPwrLevel(context: Context): Int {
    if (gpuNumPwrLevel == 0) {
        if (hasModule(getGPUPath(context), GPU_NUM_PWRLEVEL))
            gpuNumPwrLevel = readKernelInt(getGPUPath(context), GPU_NUM_PWRLEVEL) - 1
    }
    return gpuNumPwrLevel
}

var gpuAVFreq: String = ""
fun getGpuAVFreq(context: Context): String{
    if (gpuAVFreq.isEmpty()) {
        if (hasModule(getGPUPath(context), GPU_AV_FREQ))
            gpuAVFreq = readKernel(getGPUPath(context), GPU_AV_FREQ)
        else if (hasModule(getGPUPath(context), GPU_AV_CLK))
            gpuAVFreq = readKernel(getGPUPath(context), GPU_AV_CLK)
    }
    return gpuAVFreq
}

var gpuMinFreq = 0
fun getGPUMinFreq(context: Context): Int{
    if (gpuMinFreq == 0) {
        val result =
            RootUtils.runAndGetOutput("gpulist=\"${getGpuAVFreq(context)}\";parse=\${gpulist##* }; echo \$parse")
        gpuMinFreq = if (result.isEmpty())
            0
        else
            strToInt(result)
    }
    return gpuMinFreq
}

var gpuMaxFreq = 0
fun getGPUMaxFreq(context: Context): Int{
    if (gpuMinFreq == 0) {
        val result =
            RootUtils.runAndGetOutput("gpulist=\"${getGpuAVFreq(context)}\";parse=\${gpulist%% *}; echo \$parse")
        gpuMaxFreq = if (result.isEmpty())
            0
        else
            strToInt(result)
    }
    return gpuMaxFreq
}

var gpuAVGov: String = ""
fun getGpuAVGov(context: Context): String{
    if (gpuAVGov.isEmpty()) {
        if (hasModule(getGPUPath(context), GPU_AV_GOV))
            gpuAVGov = readKernel(getGPUPath(context), GPU_AV_GOV)
    }
    return gpuAVGov
}

var listPwrLevel: String = ""
fun getListPwrLevel(context: Context): String{
    if (listPwrLevel.isEmpty()) {
        for (i in 0..gpuMaxPwrLevel(context)) {
            listPwrLevel = if (listPwrLevel.isEmpty())
                "$i"
            else
                "$listPwrLevel $i"
        }
    }
    return listPwrLevel
}

fun parseAdrenoBoost(context: Context, value: Int): String{
    return when (value) {
        3 -> context.getString(R.string.high)
        2 -> context.getString(R.string.medium)
        1 -> context.getString(R.string.low)
        else -> context.getString(R.string.off)
    }
}

fun parseAdrenoBoost(context: Context, value: String): Int{
    return if (value.contains(context.getString(R.string.high)))
        3
    else if (value.contains(context.getString(R.string.medium)))
        2
    else if (value.contains(context.getString(R.string.low)))
        1
    else
        0
}

fun getDefAdrenoBoost(profile: Int): Int{
    return when (profile) {
        1 -> 3
        3 -> 2
        2 -> 1
        else -> 0
    }
}

fun setChgMax(preferences: SharedPreferences, value: Int, position: Int){
    preferences.edit().putInt(SpfConfig.CHG_MAX, position).apply()
    preferences.edit().putInt(SpfConfig.CHG_MAX_CUR_PROF, value).apply()
    setKernel("$value", CHG_CUR_MAX)
}

fun getThermalString(context: Context, posisi: Int, gki: Boolean): String {
    if (gki) {
        return when (posisi) {
            6 -> context.getString(R.string.performance)
            1 -> context.getString(R.string.battery)
            else -> context.getString(R.string.balance)
        }
    } else {
        return when (posisi) {
            10 -> context.getString(R.string.evaluation)
            11 -> context.getString(R.string.class_0)
            15 -> context.getString(R.string.ar_vr)
            8 -> context.getString(R.string.in_calls)
            9 -> context.getString(R.string.game)
            16 -> context.getString(R.string.game2)
            2 -> context.getString(R.string.extreme)
            12 -> context.getString(R.string.camera)
            13 -> context.getString(R.string.pubg)
            14 -> context.getString(R.string.youtube)
            else -> context.getString(R.string.not_set)
        }
    }
}

fun getThermalInt(context: Context, value: String, gki: Boolean): Int {
    if (gki) {
        return if (value.contains(context.getString(R.string.performance)))
            6
        else if (value.contains(context.getString(R.string.battery)))
            1
        else
            0
    } else {
        if (value.contains(context.getString(R.string.evaluation)))
            return 10
        else if (value.contains(context.getString(R.string.class_0)))
            return 11
        else if (value.contains(context.getString(R.string.ar_vr)))
            return 15
        else if (value.contains(context.getString(R.string.in_calls)))
            return 8
        else if (value.equals(context.getString(R.string.game)))
            return 9
        else if (value.equals(context.getString(R.string.game2)))
            return 16
        else if (value.contains(context.getString(R.string.extreme)))
            return 2
        else if (value.contains(context.getString(R.string.camera)))
            return 12
        else if (value.contains(context.getString(R.string.pubg)))
            return 13
        else if (value.contains(context.getString(R.string.youtube)))
            return 14
        else
            return -1
    }
}

fun getDefThermalProfile(profile: Int, gki: Boolean): Int{
    if (gki) {
        return when (profile) {
            1 -> 6
            2 -> 6
            3 -> 6
            4 -> 1
            else -> 0
        }
    } else {
        return when (profile) {
            1 -> 10
            3 -> 10
            2 -> 16
            4 -> 8
            else -> -1
        }
    }
}

fun thermalList(context: Context, gki: Boolean): String{
    if (gki) {
        return "${context.getString(R.string.performance)};${context.getString(R.string.battery)};${context.getString(R.string.balance)}"
    } else {
        return "${context.getString(R.string.evaluation)};${context.getString(R.string.class_0)};${
            context.getString(
                R.string.ar_vr
            )
        };" +
                "${context.getString(R.string.in_calls)};${context.getString(R.string.game)};${
                    context.getString(
                        R.string.game2
                    )
                };" +
                "${context.getString(R.string.extreme)};${context.getString(R.string.camera)};${
                    context.getString(
                        R.string.pubg
                    )
                };" +
                context.getString(R.string.not_set)
    }
}

fun stringToList(s : String) = s.trim().splitToSequence(' ')
    .filter { it.isNotEmpty() } // or: .filter { it.isNotBlank() }
    .toList()

fun stringToList2(s : String) = s.trim().splitToSequence(';')
    .filter { it.isNotEmpty() } // or: .filter { it.isNotBlank() }
    .toList()

fun getSpfProfileName(mode: Boolean, profile: Int): String{
    return if (mode) {
        when (profile) {
            1 -> SpfProfile.PERFORMANCE
            2 -> SpfProfile.GAME
            4 -> SpfProfile.BATTERY
            else -> SpfProfile.BALANCE
        }
    } else
        SpfProfile.NONE
}

fun getProfileString(context: Context, profile: Int): String{
    return when (profile) {
        1 -> context.getString(R.string.performance)
        2 -> context.getString(R.string.game)
        4 -> context.getString(R.string.battery)
        else -> context.getString(R.string.balance)
    }
}

fun getProfileInt(context: Context, profile: String): Int{
    return if (profile.contains(context.getString(R.string.performance)))
        1
    else if (profile.contains(context.getString(R.string.game)))
        2
    else if (profile.contains(context.getString(R.string.battery)))
        4
    else
        0
}

var defval_ioSched = ""
fun getDefIOSched(): String{
    if (defval_ioSched.isEmpty()) {
        defval_ioSched = if (getListIOSched().contains("cfq"))
            "cfq"
        else if (getListIOSched().contains("ssg"))
            "ssg"
        else if (getListIOSched().contains("bfq"))
            "bfq"
        else if (getListIOSched().contains("bbr"))
            "bbr"
        else if (getListIOSched().contains("mq-deadline"))
            "mq-deadline"
        else if (getListIOSched().contains("deadline"))
            "deadline"
        else
            getIOSelect()
    }
    return defval_ioSched
}

fun getIOSelect(): String{
    return RootUtils.runAndGetOutput("getIO=$(cat $IO_PATH/sda/$IO_SCHEDULER);parse0=\${getIO##*[};parse1=\${parse0%%]*}; echo \$parse1")
}

fun getListIOSched(): String{
    return RootUtils.runAndGetOutput("cat $IO_PATH/sda/$IO_SCHEDULER | sed 's/\\[//g' | sed 's/\\]//g'")
}

fun setIOSched(value: String) {
    var count = 'a'
    while (count <= getLastDirListIO().single()) {
        setKernel(value, "$IO_PATH/sd$count/$IO_SCHEDULER", true)
        ++count
    }
}

fun getDefDT2W(): Boolean{
    val result: String = if (hasModule(DT2W))
        readKernel(DT2W)
    else if (hasModule(DT2W_LEGACY))
        readKernel(DT2W_LEGACY)
    else
        ""
    return result.contains("1")
}

fun getUFSHelat(): Int{
    var result = RootUtils.runAndGetOutput("cat $UFS_HEALTH")
    if (result.contains("1"))
        result = "100"
    else if (result.contains("2"))
        result = "90"
    else if (result.contains("3"))
        result = "80"
    else if (result.contains("4"))
        result = "70"
    else if (result.contains("5"))
        result = "60"
    else if (result.contains("6"))
        result = "50"
    else if (result.contains("7"))
        result = "40"
    else if (result.contains("8"))
        result = "30"
    else if (result.contains("9"))
        result = "20"
    else if (result.contains("A") || result.contains("a"))
        result = "10"
    else
        result = "0"
    return strToInt(result)
}

fun getLastDirListIO(): String{
    return RootUtils.runAndGetOutput("dirIO=$(ls $IO_PATH | grep 'sd');parse0=$(echo \$dirIO);parse1=\${parse0##* }; echo \$parse1 | sed 's/sd//g'")
}

fun getSuperuserCount(): Int {
    return Natives.allowList.size
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execKsud(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

fun restoreModule(id: String): Boolean {
    val cmd = "module restore $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "restore module $id result: $result")
    return result
}

private fun flashWithIO(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    return withNewRootShell {
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }
}

fun flashModule(
    uri: Uri,
    onFinish: (Boolean, Int) -> Unit,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Boolean {
    val resolver = ksuApp.contentResolver
    with(resolver.openInputStream(uri)) {
        val file = File(ksuApp.cacheDir, "module.zip")
        file.outputStream().use { output ->
            this?.copyTo(output)
        }
        val cmd = "module install ${file.absolutePath}"
        val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
        Log.i("DKM-SU", "install module $uri result: $result")

        file.delete()

        onFinish(result.isSuccess, result.code)
        return result.isSuccess
    }
}

fun runModuleAction(
    moduleId: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val shell = getRootShell()

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    val result = shell.newJob().add("${getKsuDaemonPath()} module action $moduleId")
        .to(stdoutCallback, stderrCallback).exec()
    Log.i("DKM-SU", "Module runAction result: $result")

    return result.isSuccess
}

fun restoreBoot(
    onFinish: (Boolean, Int) -> Unit, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    val result = flashWithIO("${getKsuDaemonPath()} boot-restore -f --magiskboot $magiskboot", onStdout, onStderr)
    onFinish(result.isSuccess, result.code)
    return result.isSuccess
}

fun uninstallPermanently(
    onFinish: (Boolean, Int) -> Unit, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    val result = flashWithIO("${getKsuDaemonPath()} uninstall --magiskboot $magiskboot", onStdout, onStderr)
    onFinish(result.isSuccess, result.code)
    return result.isSuccess
}

suspend fun shrinkModules(): Boolean = withContext(Dispatchers.IO) {
    execKsud("module shrink", true)
}

@Parcelize
sealed class LkmSelection : Parcelable {
    data class LkmUri(val uri: Uri) : LkmSelection()
    data class KmiString(val value: String) : LkmSelection()
    data object KmiNone : LkmSelection()
}

fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    onFinish: (Boolean, Int) -> Unit,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): Boolean {
    val resolver = ksuApp.contentResolver

    val bootFile = bootUri?.let { uri ->
        with(resolver.openInputStream(uri)) {
            val bootFile = File(ksuApp.cacheDir, "boot.img")
            bootFile.outputStream().use { output ->
                this?.copyTo(output)
            }

            bootFile
        }
    }

    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    var cmd = "boot-patch --magiskboot ${magiskboot.absolutePath}"

    cmd += if (bootFile == null) {
        // no boot.img, use -f to force install
        " -f"
    } else {
        " -b ${bootFile.absolutePath}"
    }

    if (ota) {
        cmd += " -u"
    }

    var lkmFile: File? = null
    when (lkm) {
        is LkmSelection.LkmUri -> {
            lkmFile = with(resolver.openInputStream(lkm.uri)) {
                val file = File(ksuApp.cacheDir, "kernelsu-tmp-lkm.ko")
                file.outputStream().use { output ->
                    this?.copyTo(output)
                }

                file
            }
            cmd += " -m ${lkmFile.absolutePath}"
        }

        is LkmSelection.KmiString -> {
            cmd += " --kmi ${lkm.value}"
        }

        LkmSelection.KmiNone -> {
            // do nothing
        }
    }

    // output dir
    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    cmd += " -o $downloadsDir"

    val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
    Log.i("DKM-SU", "install boot result: ${result.isSuccess}")

    bootFile?.delete()
    lkmFile?.delete()

    // if boot uri is empty, it is direct install, when success, we should show reboot button
    onFinish(bootUri == null && result.isSuccess, result.code)
    return result.isSuccess
}

fun reboot(reason: String = "") {
    val shell = getRootShell()
    if (reason == "shutdown") {
        ShellUtils.fastCmd(shell, "setprop sys.powerctl \"reboot,shutdown\"")
    } else {
        if (reason == "recovery") {
            ShellUtils.fastCmd(shell, "/system/bin/input keyevent 26")
        }
        ShellUtils.fastCmd(
            shell,
            "/system/bin/svc power reboot $reason || /system/bin/reboot $reason"
        )
    }
}

fun rootAvailable(): Boolean {
    val shell = getRootShell()
    return shell.isRoot
}

fun isAbDevice(): Boolean {
    val shell = getRootShell()
    return ShellUtils.fastCmd(shell, "getprop ro.build.ab_update").trim().toBoolean()
}

fun isInitBoot(): Boolean {
    return !Os.uname().release.contains("android12-")
}

suspend fun getCurrentKmi(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info current-kmi"
    ShellUtils.fastCmd(shell, "${getKsuDaemonPath()} $cmd")
}

suspend fun getSupportedkmsuis(): List<String> = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val cmd = "boot-info supported-kmi"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

fun overlayFsAvailable(): Boolean {
    val shell = getRootShell()
    // check /proc/filesystems
    return ShellUtils.fastCmdResult(shell, "cat /proc/filesystems | grep overlay")
}

fun hasMagisk(): Boolean {
    val shell = getRootShell(true)
    val result = shell.newJob().add("which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun isSepolicyValid(rules: String?): Boolean {
    if (rules == null) {
        return true
    }
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} sepolicy check '$rules'").to(ArrayList(), null)
            .exec()
    return result.isSuccess
}

fun getSepolicy(pkg: String): String {
    val shell = getRootShell()
    val result =
        shell.newJob().add("${getKsuDaemonPath()} profile get-sepolicy $pkg").to(ArrayList(), null)
            .exec()
    Log.i(TAG, "code: ${result.code}, out: ${result.out}, err: ${result.err}")
    return result.out.joinToString("\n")
}

fun setSepolicy(pkg: String, rules: String): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("${getKsuDaemonPath()} profile set-sepolicy $pkg '$rules'")
        .to(ArrayList(), null).exec()
    Log.i(TAG, "set sepolicy result: ${result.code}")
    return result.isSuccess
}

fun listAppProfileTemplates(): List<String> {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile list-templates").to(ArrayList(), null)
        .exec().out
}

fun getAppProfileTemplate(id: String): String {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile get-template '${id}'")
        .to(ArrayList(), null).exec().out.joinToString("\n")
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown Module"
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    } else if (uri.scheme == "file") {
        name = uri.lastPathSegment ?: "Unknown Module"
    }
    return name
}

fun moduleBackupDir(): String? {
    val shell = getRootShell()
    val baseBackupDir = "/data/adb/modules_bak"
    val resultBase = ShellUtils.fastCmd(shell, "mkdir -p $baseBackupDir").trim()
    if (resultBase.isNotEmpty()) return null
    val timestamp = ShellUtils.fastCmd(shell, "date +%Y%m%d_%H%M%S").trim()
    if (timestamp.isEmpty()) return null
    val newBackupDir = "$baseBackupDir/$timestamp"
    val resultNewDir = ShellUtils.fastCmd(shell, "mkdir -p $newBackupDir").trim()
    if (resultNewDir.isEmpty()) return newBackupDir
    return null
}

fun moduleBackup(): Boolean {
    val shell = getRootShell()
    val checkEmptyCommand = "if [ -z \"$(ls -A /data/adb/modules)\" ]; then echo 'empty'; fi"
    val resultCheckEmpty = ShellUtils.fastCmd(shell, checkEmptyCommand).trim()
    if (resultCheckEmpty == "empty") {
        return false
    }
    val backupDir = moduleBackupDir() ?: return false
    val command = "cp -rp /data/adb/modules/* $backupDir"
    val result = ShellUtils.fastCmd(shell, command).trim()
    return result.isEmpty()
}

fun moduleMigration(): Boolean {
    val shell = getRootShell()
    val command = "cp -rp /data/adb/modules/* /data/adb/modules_update"
    val result = ShellUtils.fastCmd(shell, command).trim()
    return result.isEmpty()
}

fun moduleRestore(): Boolean {
    val shell = getRootShell()
    val command = "ls -t /data/adb/modules_bak | head -n 1"
    val latestBackupDir = ShellUtils.fastCmd(shell, command).trim()
    if (latestBackupDir.isEmpty()) return false
    val sourceDir = "/data/adb/modules_bak/$latestBackupDir"
    val destinationDir = "/data/adb/modules_update"
    val createDestDirCommand = "mkdir -p $destinationDir"
    ShellUtils.fastCmd(shell, createDestDirCommand)
    val moveCommand = "cp -rp $sourceDir/* $destinationDir"
    val result = ShellUtils.fastCmd(shell, moveCommand).trim()
    return result.isEmpty()
}

fun setAppProfileTemplate(id: String, template: String): Boolean {
    val shell = getRootShell()
    val escapedTemplate = template.replace("\"", "\\\"")
    val cmd = """${getKsuDaemonPath()} profile set-template "$id" "$escapedTemplate'""""
    return shell.newJob().add(cmd)
        .to(ArrayList(), null).exec().isSuccess
}

fun deleteAppProfileTemplate(id: String): Boolean {
    val shell = getRootShell()
    return shell.newJob().add("${getKsuDaemonPath()} profile delete-template '${id}'")
        .to(ArrayList(), null).exec().isSuccess
}

fun forceStopApp(packageName: String) {
    val shell = getRootShell()
    val result = shell.newJob().add("am force-stop $packageName").exec()
    Log.i(TAG, "force stop $packageName result: $result")
}

fun launchApp(packageName: String) {

    val shell = getRootShell()
    val result =
        shell.newJob()
            .add("cmd package resolve-activity --brief $packageName | tail -n 1 | xargs cmd activity start-activity -n")
            .exec()
    Log.i(TAG, "launch $packageName result: $result")
}

fun restartApp(packageName: String) {
    forceStopApp(packageName)
    launchApp(packageName)
}

fun getKernelPropLong(path: String): Long {
    return Natives.getKernelPropLong(path)
}

fun setPermissions(context: Context){
    val result = OneTimeWorkRequest.Builder(PermWorker::class.java)
        .build()
    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(result)
}

fun checkPermission(context: Context): Boolean {
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    return prefs.getBoolean(SpfConfig.PERMISSIONS, false)
}

fun grantPermissions(context: Context) {
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val requiredPermission = arrayOf(
        "READ_EXTERNAL_STORAGE",
        "WRITE_EXTERNAL_STORAGE",
        "CHANGE_CONFIGURATION",
        "WRITE_SECURE_SETTINGS",
        "SYSTEM_ALERT_WINDOW",
        "WRITE_SETTINGS",
        "WRITE_SYNC_SETTINGS",
        "RECEIVE_BOOT_COMPLETED",
        "BATTERY_STATS",
        "SET_WALLPAPER",
        "INTERNET",
        "READ_SYNC_SETTINGS",
        "VIBRATE",
        "WAKE_LOCK",
        "QUERY_ALL_PACKAGES",
        "FOREGROUND_SERVICE",
        "FOREGROUND_SERVICE_SPECIAL_USE",
        "START_FOREGROUND_SERVICES_FROM_BACKGROUND",
        "REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND",
        "FOREGROUND_SERVICE_DATA_SYNC",
        "FOREGROUND_SERVICE_HEALTH",
        "RUN_USER_INITIATED_JOBS",
        "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "PACKAGE_USAGE_STATS",
        "ACCESS_NOTIFICATION_POLICY",
        "CHANGE_OVERLAY_PACKAGES",
        "REAL_GET_TASKS",
        "MANAGE_EXTERNAL_STORAGE",
        "RECORD_AUDIO",
        "MODIFY_AUDIO_SETTINGS",
        "WRITE_INTERNAL_STORAGE",
        "ACCESS_NETWORK_STATE",
        "RECEIVE_USER_PRESENT",
        "BROADCAST_CLOSE_SYSTEM_DIALOGS",
    )

    val requiredPermission2 = arrayOf(
        //"android.permission.ACCESS_FINE_LOCATION",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.RECORD_AUDIO",
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    )

    requiredPermission.forEach {
        CMD_MSG = it
        KeepShellPublic.doCmdSync("appops set ${context.packageName} $it allow")
        KeepShellPublic.doCmdSync("pm grant ${context.packageName} $it")
        if (CMD_MSG.contains("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")) {
            KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist +${context.packageName}")
            prefs.edit().putBoolean(SpfConfig.PERMISSIONS, true).apply()
        }
    }
    requiredPermission2.forEach {
        CMD_MSG = it
        KeepShellPublic.doCmdSync("pm grant ${context.packageName} $it")
    }
    if (prefs.getBoolean(SpfConfig.PERMISSIONS, true))
        CMD_MSG = "Finished"
}

var batt_health = ""
fun getBattHealth(): Int{
    if (batt_health.isEmpty()) {
        var result =
            RootUtils.runAndGetOutput("bat_health=$(awk -v capacity_now=$(cat /sys/class/power_supply/battery/charge_full) -v capacity_original=$(cat /sys/class/power_supply/battery/charge_full_design) 'BEGIN { print  ( capacity_now / capacity_original * 100 ) }'); echo \$bat_health")
        if (result.isEmpty())
            return 0
        else if (result.contains("."))
            result = result.substring(0, result.indexOf("."))
        batt_health = result
    }
    return strToInt(batt_health)
}

fun getKNVersion(): Boolean{
    val result = RootUtils.runAndGetOutput("getKn=$(cat /proc/version | sed 's/Linux version //g');parse=\${getKn%%-*};parse1=\${parse%.*}; echo \$parse1")
    val out = strToInt(result.substring(0, result.indexOf(".")))
    if (out > 5)
        return true
    else if (out == 5) {
        return result.contains("$out.15")
    }
    return false
}

var getGameDef = ""
var getGameNewList = ""
var reloadData = false
fun getGameAllList(context: Context): String{
    val prefs = context.getSharedPreferences("game_ai", Context.MODE_PRIVATE)
    if (getGameDef.isEmpty()) {
        var listDefault = prefs.getString("game_ai_default", "")
        if (listDefault!!.isEmpty()) {
            listDefault = RootUtils.runAndGetOutput("cat $GAME_AI_LIST_DEFAULT | sed 's/\"/#/g'")
            prefs.edit().putString("game_ai_default", listDefault).apply()
        }
        getGameDef = listDefault
    }
    val list = prefs.getString("game_ai_list", "")
    getGameNewList = "$list"
    var spacer = ""
    if (getGameNewList.isNotEmpty())
        spacer="&#10;"
    return "$getGameDef$spacer$getGameNewList"
}

fun getGameList(context: Context, pkg: String): String{
    val prefs = context.getSharedPreferences("game_ai", Context.MODE_PRIVATE)
    val listDefault = prefs.getString("game_ai_default", "")
    val list = prefs.getString("game_ai_list", "")
    var result = ""
    if (list!!.isNotEmpty())
        result = RootUtils.runAndGetOutput("echo \"$list\" | sed 's/&#10;/\\n/g' | grep '$pkg'")
    if (result.isEmpty())
        result = RootUtils.runAndGetOutput("echo \"$listDefault\" | sed 's/&#10;/\\n/g' | grep '$pkg'")
    return result
}

fun setGameList(context: Context){
    clearEndData(context)
    val prefs = context.getSharedPreferences("game_ai", Context.MODE_PRIVATE)
    val list = prefs.getString("game_ai_list", "")
    RootUtils.runCommand("path=/data/data/${context.packageName}/files/game_ai_list; rm -fr \$path; echo \"\" > \$path")
    if (list!!.isNotEmpty())
        RootUtils.runCommand("path=/data/data/${context.packageName}/files/game_ai_list; echo -e \"$list\" | sed 's/&#10;/\\n/g' | sed 's/#/\"/g' > \$path")
    RootUtils.runCommand("cat /data/data/${context.packageName}/files/game_ai_list > $GAME_AI_LIST")
}

/*
 * exp action : Settings.ACTION_USAGE_ACCESS_SETTINGS
 */
fun requestPermission(activity: ComponentActivity, action: String) {
    val intent = Intent(action)
    activity.startActivity(intent)
}

fun requestPermission(activity: ComponentActivity, context: Context, action: String) {
    val intent = Intent(action)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    activity.startActivity(intent)
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun getTopAppPackageName(context: Context): String? {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val time = System.currentTimeMillis()
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)

    if (stats != null) {
        val sortedMap = TreeMap<Long, UsageStats>()
        for (usageStats in stats) {
            sortedMap[usageStats.lastTimeUsed] = usageStats
        }
        if (!sortedMap.isEmpty()) {
            return sortedMap[sortedMap.lastKey()]?.packageName
        }
    }
    return null
}

fun clearNotification(context: Context) {
    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(1)
}

var onFocusedApp = ""
var FPSActive = false

fun restartDkmSVC(context: Context){
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val hash = RootUtils.runAndGetOutput("data=$(dkmsvc hash); echo \${data% *}")
    if (!prefs.getString("dkmsvc_hash", "")!!.contains(hash)) {
        prefs.edit().putString("dkmsvc_hash", hash).apply()
        RootUtils.runCommand("setprop init.dkmsvc.exit 1")
        for (i in 0 .. 50) {
            if (i >= 50) {
                install_dkmsvc()
                RootUtils.runCommand("setprop init.dkmsvc.exit 0")
                startSVC()
            }
        }
    }
}
