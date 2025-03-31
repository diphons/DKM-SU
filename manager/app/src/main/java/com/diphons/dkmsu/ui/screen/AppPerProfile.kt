package com.diphons.dkmsu.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.cpu_av_freq
import com.diphons.dkmsu.ui.util.cpu_av_gov
import com.diphons.dkmsu.ui.util.forceStopApp
import com.diphons.dkmsu.ui.util.getDefAdrenoBoost
import com.diphons.dkmsu.ui.util.getDefCPUGov
import com.diphons.dkmsu.ui.util.getDefCPUMaxFreq
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getDefThermalProfile
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getGpuAVFreq
import com.diphons.dkmsu.ui.util.getGpuAVGov
import com.diphons.dkmsu.ui.util.getListIOSched
import com.diphons.dkmsu.ui.util.getListPwrLevel
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.getMinFreq
import com.diphons.dkmsu.ui.util.getThermalInt
import com.diphons.dkmsu.ui.util.getThermalString
import com.diphons.dkmsu.ui.util.getfpsbyid
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.launchApp
import com.diphons.dkmsu.ui.util.parseAdrenoBoost
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.restartApp
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.diphons.dkmsu.ui.util.thermalList
import com.diphons.dkmsu.ui.viewmodel.PerAppViewModel
import androidx.core.content.edit

/**
 * @author diphons
 * @date 2025/1/25.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppPerProfileScreen(
    navigator: DestinationsNavigator,
    appInfo: PerAppViewModel.AppInfo,
) {
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    val packageName = appInfo.packageName
    val prefs = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
    val profile by rememberSaveable {
        mutableStateOf(globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
    }
    var getValueDialog by rememberSaveable {
        mutableStateOf("")
    }
    var getModeSelect by rememberSaveable {
        mutableStateOf(0)
    }
    var cpu1_max_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1)))
    }
    var cpu1_min_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu1_min_freq", getMinFreq(context, 1)))
    }
    var cpu1_gov by rememberSaveable {
        mutableStateOf(prefs.getString("cpu1_gov", getDefCPUGov(context, profile, 1)))
    }
    var cpu2_max_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2)))
    }
    var cpu2_min_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu2_min_freq", getMinFreq(context, 2)))
    }
    var cpu2_gov by rememberSaveable {
        mutableStateOf(prefs.getString("cpu2_gov", getDefCPUGov(context, profile, 2)))
    }
    var cpu3_max_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3)))
    }
    var cpu3_min_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu3_min_freq", getMinFreq(context, 3)))
    }
    var cpu3_gov by rememberSaveable {
        mutableStateOf(prefs.getString("cpu3_gov", getDefCPUGov(context, profile, 3)))
    }
    var cpu4_max_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4)))
    }
    var cpu4_min_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("cpu4_min_freq", getMinFreq(context, 4)))
    }
    var cpu4_gov by rememberSaveable {
        mutableStateOf(prefs.getString("cpu4_gov", getDefCPUGov(context, profile, 4)))
    }
    var gpu_max_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("gpu_max_freq", getGPUMaxFreq(context)))
    }
    var gpu_min_freq by rememberSaveable {
        mutableStateOf(prefs.getInt("gpu_min_freq", getGPUMinFreq(context)))
    }
    var gpu_gov by rememberSaveable {
        mutableStateOf(prefs.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)))
    }
    var gpu_def_power by rememberSaveable {
        mutableStateOf(prefs.getInt("gpu_def_power", gpuMaxPwrLevel(context)))
    }
    var gpu_adreno_boost by rememberSaveable {
        mutableStateOf(parseAdrenoBoost(context, prefs.getInt("gpu_adreno_boost", getDefAdrenoBoost(profile))))
    }
    val gki_mode by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.GKI_MODE, false))
    }
    var thermal_profile by rememberSaveable {
        mutableStateOf(getThermalString(context, prefs.getInt("thermal", getDefThermalProfile(profile, gki_mode)), gki_mode))
    }
    var io_sched by rememberSaveable {
        mutableStateOf(prefs.getString("io_sched", getDefIOSched()))
    }
    var tcp_congs by rememberSaveable {
        mutableStateOf(prefs.getString("tcp", globalConfig.getString(SpfConfig.TCP_CONG_DEF, readKernel(TCP_CONGS))))
    }
    var fps_monitor by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI, false))
    }
    val hasAdrenoBoost by rememberSaveable {
        mutableStateOf(hasModule("${getGPUPath(context)}/$ADRENO_BOOST"))
    }

    var scrollPos by remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {

                val delta = available.y
                val newOffset = scrollPos - delta
                scrollPos = newOffset
                return Offset.Zero
            }
        }
    }

    val resetTitletxt by rememberSaveable {
        mutableStateOf(context.getString(R.string.remove))
    }
    var resetVisible by rememberSaveable {
        mutableStateOf(globalConfig.getString(SpfConfig.PER_APP_LIST, "")!!.contains(packageName))
    }
    var getList by rememberSaveable {
        mutableStateOf(globalConfig.getString(SpfConfig.PER_APP_LIST, ""))
    }
    fun updateVisible(){
        if (!resetVisible)
           resetVisible = true
        if (!getList!!.contains(packageName)) {
            if (getList!!.isEmpty())
                globalConfig.edit { putString(SpfConfig.PER_APP_LIST, packageName) }
            else
                globalConfig.edit { putString(SpfConfig.PER_APP_LIST, "$getList $packageName") }
        }
        getList = globalConfig.getString(SpfConfig.PER_APP_LIST, "")
    }
    @Composable
    fun listDialog(
        text: String,
        value: String,
        selected: String,
        onDismissRequest: () -> Unit
    ) {
        val list = if (value.contains(";")) stringToList2(value) else stringToList(value)
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(list.find { it.equals(selected) }) }

        Dialog(onDismissRequest = onDismissRequest) {
            Column (
                modifier = Modifier
                    .heightIn(0.dp, 400.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .padding(22.dp)
                            .fillMaxWidth(),
                        text = text,
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Column(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        list.forEach { text ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (text == selectedOption),
                                        onClick = {
                                            onOptionSelected(text)
                                            getValueDialog = text
                                            onDismissRequest()
                                        }
                                    )
                                    .padding(start = 22.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (text == selectedOption),
                                    onClick = {
                                        onOptionSelected(text)
                                        getValueDialog = text
                                        onDismissRequest()
                                    },
                                    modifier = Modifier.padding(5.dp),
                                    enabled = true,
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                                Text(
                                    text = text,
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (getModeSelect < 2) {
                                    Text(
                                        text = "Hz",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * CPU Mode
     * 0 - Max Freq
     * 1 - Min Freq
     * 2 - Gov
     *
     * GPU Mode
     * 0 - Max Freq
     * 1 - Min Freq
     * 2 - Gov
     * 3 - Power Level
     * 4 - Adreno Boost
     */
    var dialogEvent by remember {
        mutableStateOf<DialogEvent?>(value = null)
    }
    dialogEvent?.let { event ->
        when (event.type) {
            DialogType.Cpu -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty() && !getValueDialog.contains(" ")) {
                        if (event.mode == 0) {
                            if (event.cpu == 4)
                                cpu4_max_freq = strToInt(getValueDialog)
                            else if (event.cpu == 3)
                                cpu3_max_freq = strToInt(getValueDialog)
                            else if (event.cpu == 2)
                                cpu2_max_freq = strToInt(getValueDialog)
                            else
                                cpu1_max_freq = strToInt(getValueDialog)
                            prefs.edit { putInt("cpu${event.cpu}_max_freq", strToInt(getValueDialog)) }
                        } else if (event.mode == 1) {
                            if (event.cpu == 4)
                                cpu4_min_freq = strToInt(getValueDialog)
                            else if (event.cpu == 3)
                                cpu3_min_freq = strToInt(getValueDialog)
                            else if (event.cpu == 2)
                                cpu2_min_freq = strToInt(getValueDialog)
                            else
                                cpu1_min_freq = strToInt(getValueDialog)
                            prefs.edit { putInt("cpu${event.cpu}_min_freq", strToInt(getValueDialog)) }
                        } else if (event.mode == 2) {
                            if (event.cpu == 4)
                                cpu4_gov = getValueDialog
                            else if (event.cpu == 3)
                                cpu3_gov = getValueDialog
                            else if (event.cpu == 2)
                                cpu2_gov = getValueDialog
                            else
                                cpu1_gov = getValueDialog
                            prefs.edit { putString("cpu${event.cpu}_gov", getValueDialog) }
                        }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
            DialogType.Gpu -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        if (event.mode == 0) {
                            gpu_max_freq = strToInt(getValueDialog)
                            prefs.edit { putInt("gpu_max_freq", strToInt(getValueDialog)) }
                        } else if (event.mode == 1) {
                            gpu_min_freq = strToInt(getValueDialog)
                            prefs.edit { putInt("gpu_min_freq", strToInt(getValueDialog)) }
                        } else if (event.mode == 2) {
                            gpu_gov = getValueDialog
                            prefs.edit { putString("gpu_gov", getValueDialog) }
                        } else if (event.mode == 3) {
                            gpu_def_power = strToInt(getValueDialog)
                            prefs.edit { putInt("gpu_def_power", strToInt(getValueDialog)) }
                        } else if (event.mode == 4) {
                            gpu_adreno_boost = getValueDialog
                            prefs.edit { putInt("gpu_adreno_boost", parseAdrenoBoost(context, getValueDialog)) }
                        }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
            DialogType.Io -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        io_sched = getValueDialog
                        prefs.edit { putString("io_sched", getValueDialog) }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
            DialogType.Thermal -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        thermal_profile = getValueDialog
                        prefs.edit { putInt("thermal", getThermalInt(context, getValueDialog, gki_mode)) }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
            DialogType.Tcp -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        tcp_congs = getValueDialog
                        prefs.edit { putString("tcp", getValueDialog) }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
        }
    }

    var cpu_read by rememberSaveable {
        mutableStateOf("")
    }
    var cpu_read2 by rememberSaveable {
        mutableStateOf("")
    }
    fun setCpu(title: String, cpu: Int, mode: Int, reference: String, default: String) {
        if (mode == 0) {
            cpu_read = "${prefs.getInt("cpu${cpu}_max_freq", getDefCPUMaxFreq(context, profile, cpu))}"
        } else if (mode == 1) {
            cpu_read = "${prefs.getInt("cpu${cpu}_min_freq", getMinFreq(context, cpu))}"
        } else {
            cpu_read = "${prefs.getString("cpu${cpu}_gov", default)}"
        }
        cpu_read2 = reference
        if (mode == 0) {
            val read_min_freq = "${prefs.getInt("cpu${cpu}_min_freq", getMinFreq(context, cpu))}"
            if (!cpu_read2.contains(cpu_read)) {
                cpu_read2 = "$cpu_read2 $cpu_read"
            }
            cpu_read2 = cpu_read2.replace(read_min_freq, " - $read_min_freq")
            cpu_read2 = cpu_read2.replace("  ", " ")
            val int = cpu_read2.indexOf(" - ")
            cpu_read2 = cpu_read2.substring(int+3)
        } else if (mode == 1) {
            val read_max_freq = "${prefs.getInt("cpu${cpu}_max_freq", getDefCPUMaxFreq(context, profile, cpu))}"
            cpu_read2 = cpu_read2.replace(read_max_freq, "$read_max_freq - ")
            cpu_read2 = cpu_read2.replace("  ", " ")
            cpu_read2 = cpu_read2.substring(0, cpu_read2.indexOf(" - "))
        }
        dialogEvent = DialogEvent(DialogType.Cpu, cpu, mode, title, cpu_read2, cpu_read)
    }
    var gpu_ref by rememberSaveable {
        mutableStateOf("")
    }
    var gpu_def by rememberSaveable {
        mutableStateOf("")
    }
    fun setGpu(title: String, mode: Int) {
        if (mode == 0) {
            gpu_def = "${prefs.getInt("gpu_max_freq", getGPUMaxFreq(context))}"
            gpu_ref = getGpuAVFreq(context)
        } else if (mode == 1) {
            gpu_def = "${prefs.getInt("gpu_min_freq", getGPUMinFreq(context))}"
            gpu_ref = getGpuAVFreq(context)
        } else if (mode == 2) {
            gpu_def = "${prefs.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV))}"
            gpu_ref = getGpuAVGov(context)
        } else if (mode == 3) {
            gpu_def = "${prefs.getInt("gpu_def_power", gpuMaxPwrLevel(context))}"
            gpu_ref = getListPwrLevel(context)
        } else if (mode == 4) {
            gpu_def = parseAdrenoBoost(context, prefs.getInt("gpu_adreno_boost", getDefAdrenoBoost(profile)))
            gpu_ref = "${context.getString(R.string.off)} ${context.getString(R.string.low)} ${context.getString(R.string.medium)} ${context.getString(R.string.high)}"
        }
        if (mode == 0) {
            val gpumin_freq = "${prefs.getInt("gpu_min_freq", getGPUMinFreq(context))}"
            gpu_ref = gpu_ref.replace(gpumin_freq, "$gpumin_freq - ")
            gpu_ref = gpu_ref.replace("  ", " ")
            gpu_ref = gpu_ref.substring(0, gpu_ref.indexOf(" - "))
        } else if (mode == 1) {
            val gpumax_freq = "${prefs.getInt("gpu_max_freq", getGPUMaxFreq(context))}"
            gpu_ref = gpu_ref.replace(gpumax_freq, " - $gpumax_freq")
            gpu_ref = gpu_ref.replace("  ", " ")
            val int = gpu_ref.indexOf(" - ")
            gpu_ref = gpu_ref.substring(int+3)
        }
        dialogEvent = DialogEvent(DialogType.Gpu, 0, mode, title, gpu_ref, gpu_def)
    }
    var showDropdown by remember { mutableStateOf(false) }
    var refreshrete by rememberSaveable { mutableStateOf(
        prefs.getString(SpfConfig.REFRESH_RATE, "Default")
    )}
    var touch_sample by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.TOUCH_SAMPLE, false))
    }
    var seek_microfon by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.SC_MICROFON, 0f))
    }
    var seek_earfon by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.SC_EARFON, 0f))
    }
    var switch_headfon by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false))
    }
    var seek_headfon by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.SC_HEADFON, 0f))
    }
    var seek_headfon2 by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.SC_HEADFON2, 0f))
    }

    fun resetValue(){
        prefs.edit { putInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1)) }
        prefs.edit { putInt("cpu1_min_freq", getMinFreq(context, 1)) }
        prefs.edit { putString("cpu1_gov", getDefCPUGov(context, profile, 1)) }
        prefs.edit { putInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2)) }
        prefs.edit { putInt("cpu2_min_freq", getMinFreq(context, 2)) }
        prefs.edit { putString("cpu2_gov", getDefCPUGov(context, profile, 2)) }
        prefs.edit { putInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3)) }
        prefs.edit { putInt("cpu3_min_freq", getMinFreq(context, 3)) }
        prefs.edit { putString("cpu3_gov", getDefCPUGov(context, profile, 3)) }
        prefs.edit { putInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4)) }
        prefs.edit { putInt("cpu4_min_freq", getMinFreq(context, 4)) }
        prefs.edit { putString("cpu4_gov", getDefCPUGov(context, profile, 4)) }
        prefs.edit { putInt("gpu_max_freq", getGPUMaxFreq(context)) }
        prefs.edit { putInt("gpu_min_freq", getGPUMinFreq(context)) }
        prefs.edit { putString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)) }
        prefs.edit { putInt("gpu_def_power", gpuMaxPwrLevel(context)) }
        prefs.edit { putInt("gpu_adreno_boost", getDefAdrenoBoost(profile)) }
        prefs.edit { putInt("thermal", getDefThermalProfile(profile, gki_mode)) }
        prefs.edit { putString("io_sched", getDefIOSched()) }
        prefs.edit { putString("tcp", globalConfig.getString(SpfConfig.TCP_CONG_DEF, readKernel(TCP_CONGS))) }
        prefs.edit { putBoolean(SpfConfig.MONITOR_MINI, false) }
    }
    Scaffold(
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                appInfo = appInfo,
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false,
                visibleReset = resetVisible,
                titleReset = resetTitletxt,
                onClick = {
                    if (getList!!.contains(" $packageName"))
                        getList = getList!!.replace(" $packageName", "")
                    else if (getList!!.contains("$packageName "))
                        getList = getList!!.replace("$packageName ", "")
                    else
                        getList = getList!!.replace(packageName, "")
                    globalConfig.edit { putString(SpfConfig.PER_APP_LIST, getList) }
                    resetValue()
                    RootUtils.runCommand("rm -fr /data/data/${context.packageName}/shared_prefs/$packageName.xml")
                    navigator.popBackStack()
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (getFPSCount() > 1) {
                    @Composable
                    fun RefreshRateDropdownItem(title: String, reason: Int) {
                        DropdownMenuItem(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    text = title,
                                    textAlign = TextAlign.Center
                                )
                            }, onClick = {
                                refreshrete = if (reason == 0) "Default" else "$reason"
                                prefs.edit { putString(SpfConfig.REFRESH_RATE, "$reason") }
                                showDropdown = false
                                updateVisible()
                            })
                    }

                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    22.dp
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                            ) {
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(end = 80.dp),
                                    text = stringResource(R.string.refresh_rate),
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        MaterialTheme.colorScheme.primary
                                    }),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                showDropdown = true
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .padding(
                                                        top = 9.dp,
                                                        bottom = 9.dp,
                                                        start = 15.dp,
                                                        end = 32.dp
                                                    ),
                                                text = "$refreshrete${if (refreshrete!!.contains("Default")) "" else " Hz"}",
                                                fontSize = 16.sp,
                                                style = MaterialTheme.typography.titleSmall,
                                                textAlign = TextAlign.Center
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(end = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.KeyboardArrowDown, "",
                                                )
                                            }
                                        }
                                        if (getFPSCount() > 1) {
                                            DropdownMenu(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                expanded = showDropdown, onDismissRequest = {
                                                    showDropdown = false
                                                }) {
                                                RefreshRateDropdownItem("Default", reason = 0)
                                                for (i in 1..getFPSCount()) {
                                                    RefreshRateDropdownItem(
                                                        "${getfpsbyid(i)} Hz", reason = strToInt(
                                                            getfpsbyid(i)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.fps_monitor_show),
                            checked = fps_monitor,
                            summary = if (fps_monitor)
                                stringResource(R.string.fps_monitor_show_disable)
                            else
                                stringResource(R.string.fps_monitor_show_enable)
                        ) {
                            prefs.edit { putBoolean(SpfConfig.MONITOR_MINI, it) }
                            fps_monitor = it
                            updateVisible()
                        }
                    }
                }

                if (hasModule(TOUCH_SAMPLE) || hasModule(TOUCH_SAMPLE_GOODIX)) {
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                        ) {
                            SwitchItem(
                                title = stringResource(id = R.string.touch_sample),
                                checked = touch_sample,
                                summary = stringResource(id = R.string.touch_sample_sum)
                            ) {
                                prefs.edit { putBoolean(SpfConfig.TOUCH_SAMPLE, it) }
                                touch_sample = it
                                updateVisible()
                            }
                        }
                    }
                }

                if (getMaxCluster(context) > 3) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                                .padding(top = 15.dp, bottom = 10.dp),
                            text = "CPU Elite",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 0
                                    setCpu(
                                        "Choose Max Freq Cpu Elite",
                                        4,
                                        getModeSelect,
                                        cpu_av_freq(context, 4),
                                        CPU_INFO_MAX
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Max Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu4_max_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 1
                                    setCpu(
                                        "Choose Min Freq Cpu Elite",
                                        4,
                                        getModeSelect,
                                        cpu_av_freq(context, 4),
                                        ""
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Min Freq"
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu4_min_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 2
                                    setCpu(
                                        "Choose Governor Cpu Elite",
                                        4,
                                        getModeSelect,
                                        cpu_av_gov(context, 4),
                                        "$cpu4_gov"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Governor",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "$cpu4_gov",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (getMaxCluster(context) > 2) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                                .padding(top = 15.dp, bottom = 10.dp),
                            text = "CPU Prime",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 0
                                    setCpu(
                                        "Choose Max Freq Cpu Prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        CPU_INFO_MAX
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Max Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu3_max_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 1
                                    setCpu(
                                        "Choose Min Freq Cpu Prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        ""
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Min Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu3_min_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 2
                                    setCpu(
                                        "Choose Governor Cpu Prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_gov(context, 3),
                                        "$cpu3_gov"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Governor",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "$cpu3_gov",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (getMaxCluster(context) > 1) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                                .padding(top = 15.dp, bottom = 10.dp),
                            text = "CPU Big",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 0
                                    setCpu(
                                        "Choose Max Freq Cpu Big",
                                        2,
                                        getModeSelect,
                                        cpu_av_freq(context, 2),
                                        CPU_INFO_MAX
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Max Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu2_max_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 1
                                    setCpu(
                                        "Choose Min Freq Cpu Big",
                                        2,
                                        getModeSelect,
                                        cpu_av_freq(context, 2),
                                        ""
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Min Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu2_min_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 2
                                    setCpu(
                                        "Choose Governor Cpu Big",
                                        2,
                                        getModeSelect,
                                        cpu_av_gov(context, 2),
                                        "$cpu2_gov"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Governor",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "$cpu2_gov",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (getMaxCluster(context) > 0) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                                .padding(top = 15.dp, bottom = 10.dp),
                            text = "CPU Little",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 0
                                    setCpu("Choose Max Freq Cpu Little", 1, getModeSelect,
                                        cpu_av_freq(context, 1), CPU_INFO_MAX)
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Max Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu1_max_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 1
                                    setCpu("Choose Min Freq Cpu Little", 1, getModeSelect, cpu_av_freq(context, 1), "")
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Min Freq",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "${cpu1_min_freq / 1000} Mhz",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 2
                                    setCpu("Choose Governor Cpu Little", 1, getModeSelect, cpu_av_gov(context, 1), "$cpu1_gov")
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Governor",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = "$cpu1_gov",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 22.dp),
                        text = "GPU",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 0
                                setGpu("Choose GPU Max Freq", getModeSelect)
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Max Freq",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "${gpu_max_freq / 1000000} Mhz",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 1
                                setGpu("Choose GPU Min Freq", getModeSelect)
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Min Freq",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "${gpu_min_freq / 1000000} Mhz",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                setGpu("Choose GPU Governor", getModeSelect)
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Governor",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "$gpu_gov",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 3
                                setGpu("Choose GPU Default Power Level", getModeSelect)
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Default Power Level",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "$gpu_def_power",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (hasAdrenoBoost) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 4
                                    setGpu("Choose Adreno Boost Level", getModeSelect)
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Adreno Boost",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd),
                                text = gpu_adreno_boost,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 22.dp),
                        text = "Thermal",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                dialogEvent = DialogEvent(
                                    DialogType.Thermal,
                                    0, getModeSelect,
                                    "Choose Thermal Profile",
                                    thermalList(context, gki_mode),
                                    getThermalString(context, prefs.getInt("thermal", getDefThermalProfile(profile, gki_mode)), gki_mode))
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Profile",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = thermal_profile,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 22.dp),
                        text = "IO Sched",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                dialogEvent = DialogEvent(
                                    DialogType.Io,
                                    0, getModeSelect,
                                    "Choose IO Scheduler",
                                    getListIOSched(),
                                    "${prefs.getString("io_sched", getDefIOSched())}"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Profile",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "$io_sched",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 22.dp),
                        text = "TCP Congestion",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                dialogEvent = DialogEvent(
                                    DialogType.Tcp,
                                    0, getModeSelect,
                                    "Choose TCP Congestion",
                                    readKernel(TCP_CONGS_AV),
                                    "$tcp_congs"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Profile",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            text = "$tcp_congs",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (hasModule(SOUND_CONTROL)) {
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 5.dp),
                                text = stringResource(id = R.string.microphone_gain)
                            )
                            Box (
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 9.dp, end = 22.dp),
                            ){
                                Slider(
                                    modifier = Modifier
                                        .padding(end = 30.dp),
                                    value = seek_microfon,
                                    onValueChange = {
                                        prefs.edit { putFloat(SpfConfig.SC_MICROFON, it) }
                                        seek_microfon = it
                                        updateVisible()
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                    thumb = {
                                        SliderDefaults.Thumb(interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                            thumbSize = DpSize(20.dp, 20.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                )
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 1.dp),
                                    text = "${(seek_microfon * 100).toInt() * 20 / 100}")
                            }

                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 5.dp),
                                text = stringResource(id = R.string.earpiece_gain)
                            )
                            Box (
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 9.dp, end = 22.dp),
                            ){
                                Slider(
                                    modifier = Modifier
                                        .padding(end = 30.dp),
                                    value = seek_earfon,
                                    onValueChange = {
                                        prefs.edit { putFloat(SpfConfig.SC_EARFON, it) }
                                        seek_earfon = it
                                        updateVisible()
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                    thumb = {
                                        SliderDefaults.Thumb(interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                            thumbSize = DpSize(20.dp, 20.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                )
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 1.dp),
                                    text = "${(seek_earfon * 100).toInt() * 20 / 100}")
                            }

                            Spacer(modifier = Modifier.height(5.dp))
                            SwitchItem(
                                title = stringResource(id = R.string.per_channel_controls),
                                checked = switch_headfon
                            ) {
                                prefs.edit() { putBoolean("sc_switch_headfon", it) }
                                switch_headfon = it
                                if (!switch_headfon) {
                                    seek_headfon2 = seek_headfon
                                    prefs.edit() {
                                        putFloat("sc_headfon2", seek_headfon2)
                                    }
                                }
                                updateVisible()
                            }
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp),
                                text = if (switch_headfon) stringResource(id = R.string.headphone_gain_left) else stringResource(id = R.string.headphone_gain)
                            )
                            Box (
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 9.dp, end = 22.dp),
                            ){
                                Slider(
                                    modifier = Modifier
                                        .padding(end = 30.dp),
                                    value = seek_headfon,
                                    onValueChange = {
                                        prefs.edit { putFloat(SpfConfig.SC_HEADFON, it) }
                                        if (!switch_headfon) {
                                            prefs.edit {
                                                putFloat(SpfConfig.SC_HEADFON2, it)
                                                 }
                                            seek_headfon2 = it
                                        }
                                        seek_headfon = it
                                        updateVisible()
                                    },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.4f
                                        )
                                    ),
                                    thumb = {
                                        SliderDefaults.Thumb(interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                            thumbSize = DpSize(20.dp, 20.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                )
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 1.dp),
                                    text = "${(seek_headfon * 100).toInt() * 20 / 100}")
                            }

                            if (switch_headfon) {
                                Text(
                                    modifier = Modifier
                                        .padding(start = 16.dp),
                                    text = stringResource(id = R.string.headphone_gain_right)
                                )
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(start = 9.dp, end = 22.dp),
                                ) {
                                    Slider(
                                        modifier = Modifier
                                            .padding(end = 30.dp),
                                        value = seek_headfon2,
                                        onValueChange = {
                                            prefs.edit {
                                                putFloat(SpfConfig.SC_HEADFON2, it)
                                                 }
                                            seek_headfon2 = it
                                            updateVisible()
                                        },
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            )
                                        ),
                                        thumb = {
                                            SliderDefaults.Thumb(
                                                interactionSource = remember {
                                                    MutableInteractionSource()
                                                },
                                                thumbSize = DpSize(20.dp, 20.dp),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(bottom = 1.dp),
                                        text = "${(seek_headfon2 * 100).toInt() * 20 / 100}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit,
    appInfo: PerAppViewModel.AppInfo,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    borderVisible: Boolean,
    visibleReset: Boolean,
    titleReset: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val packageName = appInfo.packageName

    Box(modifier = Modifier
        .fillMaxWidth()
    ) {
        if (borderVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(20.dp)
                    .border(
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        RoundedCornerShape(
                            bottomStart = 19.dp,
                            bottomEnd = 19.dp
                        ),
                    )
            ) { }
        }
        TopAppBar(
            title = {
                val appIcon: @Composable () -> Unit = {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(appInfo.packageInfo).crossfade(true).build(),
                        contentDescription = appInfo.label,
                        modifier = Modifier
                            .padding(4.dp)
                            .width(48.dp)
                            .height(48.dp)
                    )
                }
                AppMenuBox(packageName) {
                    ListItem(
                        colors = ListItemDefaults.colors(Color.Transparent),
                        headlineContent = { Text(appInfo.label) },
                        supportingContent = { Text(packageName) },
                        leadingContent = appIcon,
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            },
            actions = {
                if (visibleReset) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = run {
                            MaterialTheme.colorScheme.primary
                        }),
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clickable {
                                onClick()
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = titleReset,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(if (borderVisible) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background),
            modifier = Modifier
                .padding(bottom = 1.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                    clip = true
                }
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AppMenuBox(packageName: String, content: @Composable () -> Unit) {

    var expanded by remember { mutableStateOf(false) }
    var touchPoint: Offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    touchPoint = it
                    expanded = true
                }
            }
    ) {

        content()

        val (offsetX, offsetY) = with(density) {
            (touchPoint.x.toDp()) to (touchPoint.y.toDp())
        }

        DropdownMenu(
            expanded = expanded,
            offset = DpOffset(offsetX, -offsetY),
            onDismissRequest = {
                expanded = false
            },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.launch_app)) },
                onClick = {
                    expanded = false
                    launchApp(packageName)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.force_stop_app)) },
                onClick = {
                    expanded = false
                    forceStopApp(packageName)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.restart_app)) },
                onClick = {
                    expanded = false
                    restartApp(packageName)
                },
            )
        }
    }
}

@Preview
@Composable
private fun AppPerProfilePreview() {
}

