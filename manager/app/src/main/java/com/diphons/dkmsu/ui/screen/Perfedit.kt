package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.cpu_av_freq
import com.diphons.dkmsu.ui.util.cpu_av_gov
import com.diphons.dkmsu.ui.util.getDefAdrenoBoost
import com.diphons.dkmsu.ui.util.getDefCPUGov
import com.diphons.dkmsu.ui.util.getDefCPUMaxFreq
import com.diphons.dkmsu.ui.util.getDefThermalProfile
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getGpuAVFreq
import com.diphons.dkmsu.ui.util.getGpuAVGov
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getListIOSched
import com.diphons.dkmsu.ui.util.getListPwrLevel
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.getMinFreq
import com.diphons.dkmsu.ui.util.getSpfProfileName
import com.diphons.dkmsu.ui.util.getThermalInt
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.setCPU
import com.diphons.dkmsu.ui.util.setGPU
import com.diphons.dkmsu.ui.util.getThermalString
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.parseAdrenoBoost
import com.diphons.dkmsu.ui.util.setIOSched
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setProfile
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.diphons.dkmsu.ui.util.thermalList
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator

/**
 * @author diphons
 * @date 2024/01/02.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PerfeditScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val context = LocalContext.current
    val globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
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

    var perf_mode by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.PERF_MODE, true))
    }
    val profile by rememberSaveable {
        mutableStateOf(globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
    }
    val prefs = context.getSharedPreferences(getSpfProfileName(perf_mode, profile), Context.MODE_PRIVATE)

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
    var thermal_profile by rememberSaveable {
        mutableStateOf(getThermalString(context, prefs.getInt("thermal", getDefThermalProfile(profile))))
    }
    var io_sched by rememberSaveable {
        mutableStateOf(prefs.getString("io_sched", getDefIOSched()))
    }

    val hasAdrenoBoost by rememberSaveable {
        mutableStateOf(hasModule("${getGPUPath(context)}/$ADRENO_BOOST"))
    }

    fun resetProfile(){
        prefs.edit().putInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1)).apply()
        prefs.edit().putInt("cpu1_min_freq", getMinFreq(context, 1)).apply()
        prefs.edit().putString("cpu1_gov", getDefCPUGov(context, profile, 1)).apply()

        cpu1_max_freq = getDefCPUMaxFreq(context, profile, 1)
        cpu1_min_freq = getMinFreq(context, 1)
        cpu1_gov = getDefCPUGov(context, profile, 1)
        if (getMaxCluster(context) > 1) {
            prefs.edit().putInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2)).apply()
            prefs.edit().putInt("cpu2_min_freq", getMinFreq(context, 2)).apply()
            prefs.edit().putString("cpu2_gov", getDefCPUGov(context, profile, 2)).apply()

            cpu2_max_freq = getDefCPUMaxFreq(context, profile, 2)
            cpu2_min_freq = getMinFreq(context, 2)
            cpu2_gov = getDefCPUGov(context, profile, 2)
            if (getMaxCluster(context) > 2) {
                prefs.edit().putInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3)).apply()
                prefs.edit().putInt("cpu3_min_freq", getMinFreq(context, 3)).apply()
                prefs.edit().putString("cpu3_gov", getDefCPUGov(context, profile, 3)).apply()

                cpu3_max_freq = getDefCPUMaxFreq(context, profile, 3)
                cpu3_min_freq = getMinFreq(context, 3)
                cpu3_gov = getDefCPUGov(context, profile, 3)
                if (getMaxCluster(context) > 3) {
                    prefs.edit().putInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4)).apply()
                    prefs.edit().putInt("cpu4_min_freq", getMinFreq(context, 4)).apply()
                    prefs.edit().putString("cpu4_gov", getDefCPUGov(context, profile, 4)).apply()

                    cpu4_max_freq = getDefCPUMaxFreq(context, profile, 4)
                    cpu4_min_freq = getMinFreq(context, 4)
                    cpu4_gov = getDefCPUGov(context, profile, 4)
                }
            }
        }

        prefs.edit().putInt("gpu_max_freq", getGPUMaxFreq(context)).apply()
        prefs.edit().putInt("gpu_min_freq", getGPUMinFreq(context)).apply()
        prefs.edit().putString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)).apply()
        prefs.edit().putInt("gpu_def_power", gpuMaxPwrLevel(context)).apply()
        if (hasAdrenoBoost)
            prefs.edit().putInt("gpu_adreno_boost", getDefAdrenoBoost(profile)).apply()
        prefs.edit().putInt("thermal", getDefThermalProfile(profile)).apply()
        prefs.edit().putString("io_sched", getDefIOSched()).apply()

        gpu_max_freq = getGPUMaxFreq(context)
        gpu_min_freq = getGPUMinFreq(context)
        gpu_gov = readKernel(getGPUPath(context), GPU_GOV)
        gpu_def_power = gpuMaxPwrLevel(context)
        if (hasAdrenoBoost)
            gpu_adreno_boost = parseAdrenoBoost(context, getDefAdrenoBoost(profile))
        thermal_profile = getThermalString(context, getDefThermalProfile(profile))
        io_sched = getDefIOSched()

        setProfile(context, profile)
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
                            prefs.edit().putInt("cpu${event.cpu}_max_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 1) {
                            if (event.cpu == 4)
                                cpu4_min_freq = strToInt(getValueDialog)
                            else if (event.cpu == 3)
                                cpu3_min_freq = strToInt(getValueDialog)
                            else if (event.cpu == 2)
                                cpu2_min_freq = strToInt(getValueDialog)
                            else
                                cpu1_min_freq = strToInt(getValueDialog)
                            prefs.edit().putInt("cpu${event.cpu}_min_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 2) {
                            if (event.cpu == 4)
                                cpu4_gov = getValueDialog
                            else if (event.cpu == 3)
                                cpu3_gov = getValueDialog
                            else if (event.cpu == 2)
                                cpu2_gov = getValueDialog
                            else
                                cpu1_gov = getValueDialog
                            prefs.edit().putString("cpu${event.cpu}_gov", getValueDialog).apply()
                        }
                        setCPU(getValueDialog, event.cpu, event.mode)
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
                            prefs.edit().putInt("gpu_max_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 1) {
                            gpu_min_freq = strToInt(getValueDialog)
                            prefs.edit().putInt("gpu_min_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 2) {
                            gpu_gov = getValueDialog
                            prefs.edit().putString("gpu_gov", getValueDialog).apply()
                        } else if (event.mode == 3) {
                            gpu_def_power = strToInt(getValueDialog)
                            prefs.edit().putInt("gpu_def_power", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 4) {
                            gpu_adreno_boost = getValueDialog
                            prefs.edit().putInt("gpu_adreno_boost", parseAdrenoBoost(context, getValueDialog)).apply()
                        }
                        setGPU(context, getValueDialog, event.mode)
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
                        prefs.edit().putString("io_sched", getValueDialog).apply()
                        setIOSched(getValueDialog)
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
                        prefs.edit().putInt("thermal", getThermalInt(context, getValueDialog)).apply()
                        setKernel("${getThermalInt(context, getValueDialog)}", THERMAL_PROFILE, true)
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
            cpu_read = "${prefs.getString("cpu${cpu}_gov", readKernel(context, cpu, default))}"
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
    Scaffold(
        topBar = {
            TopBar(
                onResetClick = {
                    resetProfile()
                },
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (getMaxCluster(context) > 1) {
                        if (getMaxCluster(context) > 2) {
                            if (getMaxCluster(context) > 3) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 22.dp),
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
                                            //dialogEvent = DialogEvent(DialogType.CpuPrime, 0, "Choose Freq Cpu Prime", readKernel(2, CPU_AV_FREQ), readKernel(2, CPU_INFO_MAX))
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
                                                CPU_GOV
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
                                            CPU_GOV
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
                                        CPU_GOV
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
                                setCpu("Choose Governor Cpu Little", 1, getModeSelect, cpu_av_gov(context, 1), CPU_GOV)
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
                                    thermalList(context),
                                    getThermalString(context, prefs.getInt("thermal", getDefThermalProfile(profile))))
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onResetClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    borderVisible: Boolean
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val get_profile_position by rememberSaveable {
        mutableStateOf(prefs.getInt(SpfConfig.PROFILE_MODE, 0))
    }
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
                Text(
                    "${stringResource(R.string.power_profile)} : ${
                        if (get_profile_position == 1)
                            stringResource(R.string.performance)
                        else if (get_profile_position == 2)
                            stringResource(R.string.game)
                        else if (get_profile_position == 4)
                            stringResource(R.string.battery)
                        else
                            stringResource(R.string.balance)
                    }"
                )
            },
            actions = {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                        MaterialTheme.colorScheme.primary
                    }),
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable {
                            onResetClick()
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 6.dp, horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
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

data class DialogEvent(
    val type: DialogType,
    val cpu: Int,
    val mode: Int,
    val title: String,
    val value: String,
    val selected: String
)

enum class DialogType {
    Cpu,
    Gpu,
    Io,
    Thermal
}

@Preview
@Composable
private fun KernelPreview() {
    PerfeditScreen(EmptyDestinationsNavigator)
}
