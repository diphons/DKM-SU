package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SearchAppBar
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.store.SpfProfile
import com.diphons.dkmsu.ui.util.Utils.ADRENO_BOOST
import com.diphons.dkmsu.ui.util.Utils.CPU_GOV
import com.diphons.dkmsu.ui.util.Utils.CPU_INFO_MAX
import com.diphons.dkmsu.ui.util.Utils.GAME_AI
import com.diphons.dkmsu.ui.util.Utils.GAME_AI_LOG
import com.diphons.dkmsu.ui.util.Utils.GPU_GOV
import com.diphons.dkmsu.ui.util.Utils.THERMAL_PROFILE
import com.diphons.dkmsu.ui.util.Utils.strToInt
import com.diphons.dkmsu.ui.util.cpu_av_freq
import com.diphons.dkmsu.ui.util.cpu_av_gov
import com.diphons.dkmsu.ui.util.getDefAdrenoBoost
import com.diphons.dkmsu.ui.util.getDefCPUGov
import com.diphons.dkmsu.ui.util.getDefCPUMaxFreq
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getDefThermalProfile
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getGpuAVFreq
import com.diphons.dkmsu.ui.util.getGpuAVGov
import com.diphons.dkmsu.ui.util.getIOSelect
import com.diphons.dkmsu.ui.util.getListIOSched
import com.diphons.dkmsu.ui.util.getListPwrLevel
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.getMinFreq
import com.diphons.dkmsu.ui.util.getSpfProfileName
import com.diphons.dkmsu.ui.util.getThermalInt
import com.diphons.dkmsu.ui.util.getThermalString
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.parseAdrenoBoost
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.setCPU
import com.diphons.dkmsu.ui.util.setGPU
import com.diphons.dkmsu.ui.util.setIOSched
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setProfile
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.diphons.dkmsu.ui.util.thermalList
import com.diphons.dkmsu.ui.viewmodel.AIViewModel
import com.ramcosta.composedestinations.generated.destinations.PerfeditScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppGameAIScreenDestination

/**
 * @author diphons
 * @date 2025/01/25.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PerfmodeScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<AIViewModel>()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

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
    LaunchedEffect(key1 = navigator) {
        viewModel.search = ""
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    LaunchedEffect(viewModel.search) {
        if (viewModel.search.isEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.refreshOnReturn) {
            viewModel.fetchAppList()
            viewModel.refreshOnReturn = false
        }
    }

    var perf_mode by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.PERF_MODE, true))
    }
    var gameai by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.GAME_AI, true))
    }
    var profile by rememberSaveable {
        mutableStateOf(globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
    }
    var profile_last by rememberSaveable {
        mutableStateOf(globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
    }

    var getValueDialog by rememberSaveable {
        mutableStateOf("")
    }
    var getModeSelect by rememberSaveable {
        mutableStateOf(0)
    }

    val hasAdrenoBoost by rememberSaveable {
        mutableStateOf(hasModule("${getGPUPath(context)}/$ADRENO_BOOST"))
    }

    val gki_mode by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.GKI_MODE, false))
    }
    //Live Data
    val profile_load = context.getSharedPreferences(getSpfProfileName(perf_mode, profile), Context.MODE_PRIVATE)
    val profile_balance = context.getSharedPreferences(SpfProfile.BALANCE, Context.MODE_PRIVATE)
    val profile_performance = context.getSharedPreferences(SpfProfile.PERFORMANCE, Context.MODE_PRIVATE)
    val profile_game = context.getSharedPreferences(SpfProfile.GAME, Context.MODE_PRIVATE)
    val profile_battery = context.getSharedPreferences(SpfProfile.BATTERY, Context.MODE_PRIVATE)
    val profile_none = context.getSharedPreferences(SpfProfile.NONE, Context.MODE_PRIVATE)

    var cpu1_max_freq = MutableLiveData<Int>(profile_load.getInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1)))
    var cpu1_min_freq = MutableLiveData<Int>(profile_load.getInt("cpu1_min_freq", getMinFreq(context, 1)))
    var cpu1_gov = MutableLiveData<String>(profile_load.getString("cpu1_gov", getDefCPUGov(context, profile, 1)))
    var cpu2_max_freq = MutableLiveData<Int>(profile_load.getInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2)))
    var cpu2_min_freq = MutableLiveData<Int>(profile_load.getInt("cpu2_min_freq", getMinFreq(context, 2)))
    var cpu2_gov = MutableLiveData<String>(profile_load.getString("cpu2_gov", getDefCPUGov(context, profile, 2)))
    var cpu3_max_freq = MutableLiveData<Int>(profile_load.getInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3)))
    var cpu3_min_freq = MutableLiveData<Int>(profile_load.getInt("cpu3_min_freq", getMinFreq(context, 3)))
    var cpu3_gov = MutableLiveData<String>(profile_load.getString("cpu3_gov", getDefCPUGov(context, profile, 3)))
    var cpu4_max_freq = MutableLiveData<Int>(profile_load.getInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4)))
    var cpu4_min_freq = MutableLiveData<Int>(profile_load.getInt("cpu4_min_freq", getMinFreq(context, 4)))
    var cpu4_gov = MutableLiveData<String>(profile_load.getString("cpu4_gov", getDefCPUGov(context, profile, 4)))
    var gpu_max_freq = MutableLiveData<Int>(profile_load.getInt("gpu_max_freq", getGPUMaxFreq(context)))
    var gpu_min_freq = MutableLiveData<Int>(profile_load.getInt("gpu_min_freq", getGPUMinFreq(context)))
    var gpu_gov = MutableLiveData<String>(profile_load.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)))
    var gpu_def_power = MutableLiveData<Int>(profile_load.getInt("gpu_def_power", gpuMaxPwrLevel(context)))
    var gpu_adreno_boost = MutableLiveData<String>(parseAdrenoBoost(context, profile_load.getInt("gpu_adreno_boost", getDefAdrenoBoost(profile))))
    var thermal_profile = MutableLiveData<String>(getThermalString(context, profile_load.getInt("thermal", getDefThermalProfile(profile, gki_mode)), gki_mode))
    var io_sched = MutableLiveData<String>(profile_load.getString("io_sched", getIOSelect()))

    fun reloadDataInfo(pref_profile: SharedPreferences){
        cpu1_max_freq.value = pref_profile.getInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1))
        cpu1_min_freq.value = pref_profile.getInt("cpu1_min_freq", getMinFreq(context, 1))
        cpu1_gov.value = pref_profile.getString("cpu1_gov", getDefCPUGov(context, profile, 1))
        if (getMaxCluster(context) > 1) {
            cpu2_max_freq.value =
                pref_profile.getInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2))
            cpu2_min_freq.value = pref_profile.getInt("cpu2_min_freq", getMinFreq(context, 2))
            cpu2_gov.value = pref_profile.getString("cpu2_gov", getDefCPUGov(context, profile, 2))
            if (getMaxCluster(context) > 2) {
                cpu3_max_freq.value =
                    pref_profile.getInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3))
                cpu3_min_freq.value = pref_profile.getInt("cpu3_min_freq", getMinFreq(context, 3))
                cpu3_gov.value =
                    pref_profile.getString("cpu3_gov", getDefCPUGov(context, profile, 3))
                if (getMaxCluster(context) > 3) {
                    cpu4_max_freq.value =
                        pref_profile.getInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4))
                    cpu4_min_freq.value =
                        pref_profile.getInt("cpu4_min_freq", getMinFreq(context, 4))
                    cpu4_gov.value =
                        pref_profile.getString("cpu4_gov", getDefCPUGov(context, profile, 4))
                }
            }
        }
        gpu_max_freq.value = pref_profile.getInt("gpu_max_freq", getGPUMaxFreq(context))
        gpu_min_freq.value = pref_profile.getInt("gpu_min_freq", getGPUMinFreq(context))
        gpu_gov.value = pref_profile.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV))
        gpu_def_power.value = pref_profile.getInt("gpu_def_power", gpuMaxPwrLevel(context))
        if (hasAdrenoBoost)
            gpu_adreno_boost.value = parseAdrenoBoost(context, pref_profile.getInt("gpu_adreno_boost", getDefAdrenoBoost(profile)))
        thermal_profile.value = getThermalString(context, pref_profile.getInt("thermal", getDefThermalProfile(profile, gki_mode)), gki_mode)
        io_sched.value = pref_profile.getString("io_sched", getIOSelect())
    }

    fun resetProfile(){
        profile_none.edit().putInt("cpu1_max_freq", getDefCPUMaxFreq(context, profile, 1)).apply()
        profile_none.edit().putInt("cpu1_min_freq", getMinFreq(context, 1)).apply()
        profile_none.edit().putString("cpu1_gov", getDefCPUGov(context, profile, 1)).apply()

        cpu1_max_freq.value = getDefCPUMaxFreq(context, profile, 1)
        cpu1_min_freq.value = getMinFreq(context, 1)
        cpu1_gov.value = getDefCPUGov(context, profile, 1)
        if (getMaxCluster(context) > 1) {
            profile_none.edit().putInt("cpu2_max_freq", getDefCPUMaxFreq(context, profile, 2)).apply()
            profile_none.edit().putInt("cpu2_min_freq", getMinFreq(context, 2)).apply()
            profile_none.edit().putString("cpu2_gov", getDefCPUGov(context, profile, 2)).apply()

            cpu2_max_freq.value = getDefCPUMaxFreq(context, profile, 2)
            cpu2_min_freq.value = getMinFreq(context, 2)
            cpu2_gov.value = getDefCPUGov(context, profile, 2)
            if (getMaxCluster(context) > 2) {
                profile_none.edit().putInt("cpu3_max_freq", getDefCPUMaxFreq(context, profile, 3)).apply()
                profile_none.edit().putInt("cpu3_min_freq", getMinFreq(context, 3)).apply()
                profile_none.edit().putString("cpu3_gov", getDefCPUGov(context, profile, 3)).apply()

                cpu3_max_freq.value = getDefCPUMaxFreq(context, profile, 3)
                cpu3_min_freq.value = getMinFreq(context, 3)
                cpu3_gov.value = getDefCPUGov(context, profile, 3)
                if (getMaxCluster(context) > 3) {
                    profile_none.edit().putInt("cpu4_max_freq", getDefCPUMaxFreq(context, profile, 4)).apply()
                    profile_none.edit().putInt("cpu4_min_freq", getMinFreq(context, 4)).apply()
                    profile_none.edit().putString("cpu4_gov", getDefCPUGov(context, profile, 4)).apply()

                    cpu4_max_freq.value = getDefCPUMaxFreq(context, profile, 4)
                    cpu4_min_freq.value = getMinFreq(context, 4)
                    cpu4_gov.value = getDefCPUGov(context, profile, 4)
                }
            }
        }

        profile_none.edit().putInt("gpu_max_freq", getGPUMaxFreq(context)).apply()
        profile_none.edit().putInt("gpu_min_freq", getGPUMinFreq(context)).apply()
        profile_none.edit().putString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)).apply()
        profile_none.edit().putInt("gpu_def_power", gpuMaxPwrLevel(context)).apply()
        if (hasAdrenoBoost)
            profile_none.edit().putInt("gpu_adreno_boost", getDefAdrenoBoost(profile)).apply()
        profile_none.edit().putInt("thermal", getDefThermalProfile(profile, gki_mode)).apply()
        profile_none.edit().putString("io_sched", getDefIOSched()).apply()

        gpu_max_freq.value = getGPUMaxFreq(context)
        gpu_min_freq.value = getGPUMinFreq(context)
        gpu_gov.value = readKernel(getGPUPath(context), GPU_GOV)
        gpu_def_power.value = gpuMaxPwrLevel(context)
        if (hasAdrenoBoost)
            gpu_adreno_boost.value = parseAdrenoBoost(context, getDefAdrenoBoost(profile))
        thermal_profile.value = getThermalString(context, getDefThermalProfile(profile, gki_mode), gki_mode)
        io_sched.value = getDefIOSched()

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
                                cpu4_max_freq.value = strToInt(getValueDialog)
                            else if (event.cpu == 3)
                                cpu3_max_freq.value = strToInt(getValueDialog)
                            else if (event.cpu == 2)
                                cpu2_max_freq.value = strToInt(getValueDialog)
                            else
                                cpu1_max_freq.value = strToInt(getValueDialog)
                            profile_none.edit().putInt("cpu${event.cpu}_max_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 1) {
                            if (event.cpu == 4)
                                cpu4_min_freq.value = strToInt(getValueDialog)
                            else if (event.cpu == 3)
                                cpu3_min_freq.value = strToInt(getValueDialog)
                            else if (event.cpu == 2)
                                cpu2_min_freq.value = strToInt(getValueDialog)
                            else
                                cpu1_min_freq.value = strToInt(getValueDialog)
                            profile_none.edit().putInt("cpu${event.cpu}_min_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 2) {
                            if (event.cpu == 4)
                                cpu4_gov.value = getValueDialog
                            else if (event.cpu == 3)
                                cpu3_gov.value = getValueDialog
                            else if (event.cpu == 2)
                                cpu2_gov.value = getValueDialog
                            else
                                cpu1_gov.value = getValueDialog
                            profile_none.edit().putString("cpu${event.cpu}_gov", getValueDialog).apply()
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
                            gpu_max_freq.value = strToInt(getValueDialog)
                            profile_none.edit().putInt("gpu_max_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 1) {
                            gpu_min_freq.value = strToInt(getValueDialog)
                            profile_none.edit().putInt("gpu_min_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 2) {
                            gpu_gov.value = getValueDialog
                            profile_none.edit().putString("gpu_gov", getValueDialog).apply()
                        } else if (event.mode == 3) {
                            gpu_def_power.value = strToInt(getValueDialog)
                            profile_none.edit().putInt("gpu_def_power", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 4) {
                            gpu_adreno_boost.value = getValueDialog
                            profile_none.edit().putInt("gpu_adreno_boost", parseAdrenoBoost(context, getValueDialog)).apply()
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
                        io_sched.value = getValueDialog
                        profile_none.edit().putString("io_sched", getValueDialog).apply()
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
                        thermal_profile.value = getValueDialog
                        profile_none.edit().putInt("thermal", getThermalInt(context, getValueDialog, gki_mode)).apply()
                        setKernel("${getThermalInt(context, getValueDialog, gki_mode)}", THERMAL_PROFILE, true)
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
            cpu_read = "${profile_none.getInt("cpu${cpu}_max_freq", getDefCPUMaxFreq(context, profile, cpu))}"
        } else if (mode == 1) {
            cpu_read = "${profile_none.getInt("cpu${cpu}_min_freq", getMinFreq(context, cpu))}"
        } else {
            cpu_read = "${profile_none.getString("cpu${cpu}_gov", readKernel(context, cpu, default))}"
        }
        cpu_read2 = reference
        if (mode == 0) {
            val read_min_freq = "${profile_none.getInt("cpu${cpu}_min_freq", getMinFreq(context, cpu))}"
            if (!cpu_read2.contains(cpu_read)) {
                cpu_read2 = "$cpu_read2 $cpu_read"
            }
            cpu_read2 = cpu_read2.replace(read_min_freq, " - $read_min_freq")
            cpu_read2 = cpu_read2.replace("  ", " ")
            val int = cpu_read2.indexOf(" - ")
            cpu_read2 = cpu_read2.substring(int+3)
        } else if (mode == 1) {
            val read_max_freq = "${profile_none.getInt("cpu${cpu}_max_freq", getDefCPUMaxFreq(context, profile, cpu))}"
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
            gpu_def = "${profile_none.getInt("gpu_max_freq", getGPUMaxFreq(context))}"
            gpu_ref = getGpuAVFreq(context)
        } else if (mode == 1) {
            gpu_def = "${profile_none.getInt("gpu_min_freq", getGPUMinFreq(context))}"
            gpu_ref = getGpuAVFreq(context)
        } else if (mode == 2) {
            gpu_def = "${profile_none.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV))}"
            gpu_ref = getGpuAVGov(context)
        } else if (mode == 3) {
            gpu_def = "${profile_none.getInt("gpu_def_power", gpuMaxPwrLevel(context))}"
            gpu_ref = getListPwrLevel(context)
        } else if (mode == 4) {
            gpu_def = parseAdrenoBoost(context, profile_none.getInt("gpu_adreno_boost", getDefAdrenoBoost(profile)))
            gpu_ref = "${context.getString(R.string.off)} ${context.getString(R.string.low)} ${context.getString(R.string.medium)} ${context.getString(R.string.high)}"
        }
        if (mode == 0) {
            val gpumin_freq = "${profile_none.getInt("gpu_min_freq", getGPUMinFreq(context))}"
            gpu_ref = gpu_ref.replace(gpumin_freq, "$gpumin_freq - ")
            gpu_ref = gpu_ref.replace("  ", " ")
            gpu_ref = gpu_ref.substring(0, gpu_ref.indexOf(" - "))
        } else if (mode == 1) {
            val gpumax_freq = "${profile_none.getInt("gpu_max_freq", getGPUMaxFreq(context))}"
            gpu_ref = gpu_ref.replace(gpumax_freq, " - $gpumax_freq")
            gpu_ref = gpu_ref.replace("  ", " ")
            val int = gpu_ref.indexOf(" - ")
            gpu_ref = gpu_ref.substring(int+3)
        }
        dialogEvent = DialogEvent(DialogType.Gpu, 0, mode, title, gpu_ref, gpu_def)
    }
    fun perf_mode_switch(mode: Boolean){
        perf_mode = mode
        globalConfig.edit().putBoolean(SpfConfig.PERF_MODE, mode).apply()
        if (mode) {
            profile = globalConfig.getInt(SpfConfig.PROFILE_MODE_LAST, 0)
            globalConfig.edit().putInt(SpfConfig.PROFILE_MODE, profile).apply()
            if (gameai)
                setKernel("1", GAME_AI)
            else {
                setKernel("0", GAME_AI)
                setProfile(context, globalConfig.getInt(SpfConfig.PROFILE_MODE, 0))
            }
        } else {
            globalConfig.edit().putInt(SpfConfig.PROFILE_MODE_LAST, profile).apply()
            profile = 5
            reloadDataInfo(profile_none)
            setProfile(context, profile)
        }
    }
    Scaffold(
        topBar = {
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                if (!gameai && scrollPos > 0.1f) {
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
                    title = {Text(if (perf_mode && gameai && hasModule(GAME_AI)) "" else stringResource(R.string.performance_mode))},
                    actions = {
                        if (perf_mode && gameai && hasModule(GAME_AI))  {
                            SearchAppBar(
                                title = { Text(stringResource(R.string.performance_mode)) },
                                searchText = viewModel.search,
                                onSearchTextChange = { viewModel.search = it },
                                onClearClick = { viewModel.search = "" },
                                dropdownContent = {
                                    var showDropdown by remember { mutableStateOf(false) }

                                    IconButton(
                                        onClick = { showDropdown = true },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = stringResource(id = R.string.settings)
                                        )

                                        DropdownMenu(expanded = showDropdown, onDismissRequest = {
                                            showDropdown = false
                                        }) {
                                            DropdownMenuItem(text = {
                                                Text(stringResource(R.string.refresh))
                                            }, onClick = {
                                                scope.launch {
                                                    viewModel.fetchAppList()
                                                }
                                                showDropdown = false
                                            })
                                            DropdownMenuItem(text = {
                                                Text(
                                                    if (viewModel.showSystemApps) {
                                                        stringResource(R.string.hide_system_apps)
                                                    } else {
                                                        stringResource(R.string.show_system_apps)
                                                    }
                                                )
                                            }, onClick = {
                                                viewModel.showSystemApps = !viewModel.showSystemApps
                                                showDropdown = false
                                            })
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Switch(
                                        modifier = Modifier
                                            .padding(end = 8.dp),
                                        checked = perf_mode,
                                        onCheckedChange = {
                                            perf_mode_switch(it)
                                        }
                                    )
                                },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
                            )
                        } else {
                            Switch(
                                modifier = Modifier
                                    .padding(end = 16.dp),
                                checked = perf_mode,
                                onCheckedChange = {
                                    perf_mode_switch(it)
                                }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(if (!gameai && scrollPos > 0.1f) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background),
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
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        if (perf_mode && gameai && hasModule(GAME_AI)) {
            PullToRefreshBox(
                modifier = Modifier.padding(innerPadding),
                onRefresh = {
                    scope.launch { viewModel.fetchAppList() }
                },
                isRefreshing = viewModel.isRefreshing
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, end = 5.dp)
                        ) {
                            SwitchItem(
                                title = stringResource(id = R.string.game_ai),
                                checked = gameai,
                                summary = if (gameai) stringResource(id = R.string.game_ai_sum2) else stringResource(
                                    id = R.string.game_ai_sum
                                )
                            ) {
                                if (it)
                                    setKernel("1", GAME_AI)
                                else {
                                    setKernel("0", GAME_AI)
                                    // Set saved profile mode
                                    profile = globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)
                                    setProfile(context, profile)
                                }
                                globalConfig.edit().putBoolean(SpfConfig.GAME_AI, it).apply()
                                gameai = it
                            }
                            if (gameai) {
                                var game_ai_log by rememberSaveable {
                                    mutableStateOf(globalConfig.getBoolean(SpfConfig.GAME_AI_LOG, false))
                                }
                                SwitchItem(
                                    title = stringResource(id = R.string.game_ai_log),
                                    checked = game_ai_log,
                                    summary = stringResource(id = R.string.game_ai_log_sum)
                                ) {
                                    if (it)
                                        setKernel("1", GAME_AI_LOG)
                                    else
                                        setKernel("0", GAME_AI_LOG)
                                    globalConfig.edit().putBoolean(SpfConfig.GAME_AI_LOG, it).apply()
                                    game_ai_log = it
                                }
                            }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        items(viewModel.appList, key = { it.packageName }) { app ->
                            AppItem(app) {
                                viewModel.refreshOnReturn = true
                                navigator.navigate(AppGameAIScreenDestination(app))
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (perf_mode) {
                    ElevatedCard {
                        if (hasModule(GAME_AI)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 5.dp, end = 5.dp)
                            ) {
                                SwitchItem(
                                    title = stringResource(id = R.string.game_ai),
                                    checked = gameai,
                                    summary = if (gameai) stringResource(id = R.string.game_ai_sum2) else stringResource(
                                        id = R.string.game_ai_sum
                                    )
                                ) {
                                    if (it)
                                        setKernel("1", GAME_AI)
                                    else {
                                        setKernel("0", GAME_AI)
                                        // Set saved profile mode
                                        profile = globalConfig.getInt(SpfConfig.PROFILE_MODE, 0)
                                        setProfile(context, profile)
                                    }
                                    globalConfig.edit().putBoolean(SpfConfig.GAME_AI, it).apply()
                                    gameai = it
                                }
                                if (gameai) {
                                    var game_ai_log by rememberSaveable {
                                        mutableStateOf(globalConfig.getBoolean(SpfConfig.GAME_AI_LOG, false))
                                    }
                                    SwitchItem(
                                        title = stringResource(id = R.string.game_ai_log),
                                        checked = game_ai_log,
                                        summary = stringResource(id = R.string.game_ai_log_sum)
                                    ) {
                                        if (it)
                                            setKernel("1", GAME_AI_LOG)
                                        else
                                            setKernel("0", GAME_AI_LOG)
                                        globalConfig.edit().putBoolean(SpfConfig.GAME_AI_LOG, it).apply()
                                        game_ai_log = it
                                    }
                                }
                            }
                        }
                        if (!hasModule(GAME_AI)) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, top = 2.dp, bottom = 22.dp, end = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(1.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        if (profile == 0)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(end = 5.dp)
                                        .border(
                                            if (profile == 0)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (profile == 0)
                                                        Modifier.padding(end = 30.dp)
                                                    else
                                                        Modifier.padding(end = 0.dp)
                                                )
                                                .clickable {
                                                    profile = 0
                                                    setProfile(context, profile)
                                                    reloadDataInfo(profile_balance)
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                text = stringResource(R.string.balance),
                                                style = MaterialTheme.typography.titleSmall,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (profile == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(30.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    .clickable {
                                                        navigator.navigate(
                                                            PerfeditScreenDestination
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .height(16.dp)
                                                        .align(Alignment.Center)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Menu, "",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        if (profile == 2)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(start = 5.dp)
                                        .border(
                                            if (profile == 2)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (profile == 2)
                                                        Modifier.padding(end = 30.dp)
                                                    else
                                                        Modifier.padding(end = 0.dp)
                                                )
                                                .clickable {
                                                    profile = 2
                                                    setProfile(context, profile)
                                                    reloadDataInfo(profile_game)
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                text = stringResource(R.string.game),
                                                style = MaterialTheme.typography.titleSmall,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (profile == 2) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(30.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    .clickable {
                                                        navigator.navigate(
                                                            PerfeditScreenDestination
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .height(16.dp)
                                                        .align(Alignment.Center)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Menu, "",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(1.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        if (profile == 1)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(end = 5.dp)
                                        .border(
                                            if (profile == 1)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (profile == 1)
                                                        Modifier.padding(end = 30.dp)
                                                    else
                                                        Modifier.padding(end = 0.dp)
                                                )
                                                .clickable {
                                                    profile = 1
                                                    setProfile(context, profile)
                                                    reloadDataInfo(profile_performance)
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                text = stringResource(R.string.performance),
                                                style = MaterialTheme.typography.titleSmall,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (profile == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(30.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    .clickable {
                                                        navigator.navigate(
                                                            PerfeditScreenDestination
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .height(16.dp)
                                                        .align(Alignment.Center)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Menu, "",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        if (profile == 4)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.background
                                    }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(start = 5.dp)
                                        .border(
                                            if (profile == 4)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (profile == 4)
                                                        Modifier.padding(end = 30.dp)
                                                    else
                                                        Modifier.padding(end = 0.dp)
                                                )
                                                .clickable {
                                                    profile = 4
                                                    setProfile(context, profile)
                                                    reloadDataInfo(profile_battery)
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                text = stringResource(R.string.battery),
                                                style = MaterialTheme.typography.titleSmall,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (profile == 4) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(30.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    .clickable {
                                                        navigator.navigate(
                                                            PerfeditScreenDestination
                                                        )
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .height(16.dp)
                                                        .align(Alignment.Center)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Menu, "",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 17.dp)
                            ) {
                                if (getMaxCluster(context) > 1) {
                                    if (getMaxCluster(context) > 2) {
                                        if (getMaxCluster(context) > 3) {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 5.dp),
                                                text = "CPU Elite",
                                                fontSize = 12.sp,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 5.dp)
                                            ) {
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart),
                                                    text = "Max Freq",
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd),
                                                    text = "${cpu4_max_freq.value?.div(1000)}Mhz",
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 5.dp)
                                            ) {
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart),
                                                    text = "Min Freq",
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd),
                                                    text = "${cpu4_min_freq.value?.div(1000)}Mhz",
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 5.dp)
                                            ) {
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart),
                                                    text = "Gov",
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd),
                                                    text = "${cpu4_gov.value}",
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 10.sp,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(5.dp))
                                        }
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 5.dp),
                                            text = "CPU Prime",
                                            fontSize = 12.sp,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart),
                                                text = "Max Freq",
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd),
                                                text = "${cpu3_max_freq.value?.div(1000)}Mhz",
                                                textAlign = TextAlign.Center,
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart),
                                                text = "Min Freq",
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd),
                                                text = "${cpu3_min_freq.value?.div(1000)}Mhz",
                                                textAlign = TextAlign.Center,
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart),
                                                text = "Gov",
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd),
                                                text = "${cpu3_gov.value}",
                                                textAlign = TextAlign.Center,
                                                fontSize = 10.sp,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 5.dp),
                                        text = "CPU Big",
                                        fontSize = 12.sp,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart),
                                            text = "Max Freq",
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd),
                                            text = "${cpu2_max_freq.value?.div(1000)}Mhz",
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart),
                                            text = "Min Freq",
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd),
                                            text = "${cpu2_min_freq.value?.div(1000)}Mhz",
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart),
                                            text = "Gov",
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd),
                                            text = "${cpu2_gov.value}",
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 5.dp),
                                    text = "CPU Little",
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Max Freq",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${cpu1_max_freq.value?.div(1000)}Mhz",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Min Freq",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${cpu1_min_freq.value?.div(1000)}Mhz",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Gov",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${cpu1_gov.value}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 17.dp)
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 5.dp),
                                    text = "GPU",
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Max Freq",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${gpu_max_freq.value?.div(1000000)} Mhz",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Min Freq",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${gpu_min_freq.value?.div(1000000)} Mhz",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Gov",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${gpu_gov.value}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Def Pwr Level",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${gpu_def_power.value}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (hasAdrenoBoost) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart),
                                            text = "Adreno Boost",
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd),
                                            text = "${gpu_adreno_boost.value}",
                                            textAlign = TextAlign.Center,
                                            fontSize = 10.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 5.dp),
                                    text = "Thermal",
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Profile",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${thermal_profile.value}".replace(" (evaluation)", ""),
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 5.dp),
                                    text = "IO Sched",
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart),
                                        text = "Scheduler",
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd),
                                        text = "${io_sched.value}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    ElevatedCard (
                        colors = CardDefaults.elevatedCardColors(containerColor = run {
                            MaterialTheme.colorScheme.errorContainer
                        })
                    ){
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = stringResource(R.string.performance_mode_disabled),
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column (
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ){
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ){
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 22.dp)
                                        .align(Alignment.CenterStart),
                                    text = stringResource(R.string.manual_settings),
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                                        MaterialTheme.colorScheme.primary
                                    }),
                                    modifier = Modifier
                                        .padding(vertical = 5.dp, horizontal = 22.dp)
                                        .align(Alignment.CenterEnd)
                                        .clickable {
                                            resetProfile()
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
                            }
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
                                                text = "${cpu4_max_freq.value?.div(1000)} Mhz",
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
                                                text = "${cpu4_min_freq.value?.div(1000)} Mhz",
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
                                                text = "${cpu4_gov.value}",
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
                                            text = "${cpu3_max_freq.value?.div(1000)} Mhz",
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
                                            text = "${cpu3_min_freq.value?.div(1000)} Mhz",
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
                                            text = "${cpu3_gov.value}",
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
                                        text = "${cpu2_max_freq.value?.div(1000)} Mhz",
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
                                        text = "${cpu2_min_freq.value?.div(1000)} Mhz",
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
                                        text = "${cpu2_gov.value}",
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
                                            cpu_av_freq(context, 1), CPU_INFO_MAX
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
                                    text = "${cpu1_max_freq.value?.div(1000)} Mhz",
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
                                    text = "${cpu1_min_freq.value?.div(1000)} Mhz",
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
                                    text = "${cpu1_gov.value}",
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
                                    text = "${gpu_max_freq.value?.div(1000000)} Mhz",
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
                                    text = "${gpu_min_freq.value?.div(1000000)} Mhz",
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
                                    text = "${gpu_gov.value}",
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
                                    text = "${gpu_def_power.value}",
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
                                        text = "${gpu_adreno_boost.value}",
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
                                            getThermalString(context, profile_none.getInt("thermal", getDefThermalProfile(profile, gki_mode)), gki_mode))
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
                                    text = "${thermal_profile.value}",
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
                                            "${profile_none.getString("io_sched", getDefIOSched())}"
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
                                    text = "${io_sched.value}",
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: AIViewModel.AppInfo,
    onClickListener: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClickListener),
        headlineContent = { Text(app.label) },
        supportingContent = {
            Column {
                Text(app.packageName)
                FlowRow {
                    if (app.gameAi)
                        LabelText(label = "Game Ai")
                    if (app.oprofile.isNotEmpty() || app.governor.isNotEmpty() || app.thermal.isNotEmpty() || app.adreno.isNotEmpty()) {
                        if (app.gameMode)
                            LabelText(label = "Game")
                        LabelText(label = "Profile: ${app.oprofile}")
                        LabelText(label = "Gov: ${app.governor}")
                        LabelText(label = "T: ${app.thermal}")
                        LabelText(label = "Boost: ${app.adreno}")
                    }
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(app.packageInfo)
                    .crossfade(true)
                    .build(),
                contentDescription = app.label,
                modifier = Modifier
                    .padding(4.dp)
                    .width(48.dp)
                    .height(48.dp)
            )
        },
    )
}
