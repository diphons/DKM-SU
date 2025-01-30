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
import com.diphons.dkmsu.ui.util.cpu_av_gov
import com.diphons.dkmsu.ui.util.forceStopApp
import com.diphons.dkmsu.ui.util.getDefIOSched
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.getGPUMaxFreq
import com.diphons.dkmsu.ui.util.getGPUMinFreq
import com.diphons.dkmsu.ui.util.getGPUPath
import com.diphons.dkmsu.ui.util.getGameDef
import com.diphons.dkmsu.ui.util.getGameList
import com.diphons.dkmsu.ui.util.getGameNewList
import com.diphons.dkmsu.ui.util.getGpuAVFreq
import com.diphons.dkmsu.ui.util.getGpuAVGov
import com.diphons.dkmsu.ui.util.getListIOSched
import com.diphons.dkmsu.ui.util.getListPwrLevel
import com.diphons.dkmsu.ui.util.getProfileInt
import com.diphons.dkmsu.ui.util.getProfileString
import com.diphons.dkmsu.ui.util.getThermalInt
import com.diphons.dkmsu.ui.util.getThermalString
import com.diphons.dkmsu.ui.util.getfpsbyid
import com.diphons.dkmsu.ui.util.gpuMaxPwrLevel
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.launchApp
import com.diphons.dkmsu.ui.util.parseAdrenoBoost
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.reloadData
import com.diphons.dkmsu.ui.util.restartApp
import com.diphons.dkmsu.ui.util.setGameList
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.diphons.dkmsu.ui.util.thermalList
import com.diphons.dkmsu.ui.viewmodel.AIViewModel

/**
 * @author diphons
 * @date 2025/1/25.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppGameAIScreen(
    navigator: DestinationsNavigator,
    appInfo: AIViewModel.AppInfo,
) {
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val prefs = context.getSharedPreferences("game_ai", Context.MODE_PRIVATE)

    val packageName = appInfo.packageName
    val perapp = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
    var data by rememberSaveable {
        mutableStateOf("")
    }
    var game_mode by rememberSaveable {
        mutableStateOf(0)
    }
    var oprofile by rememberSaveable {
        mutableStateOf(0)
    }
    val gov_ref by rememberSaveable {
        mutableStateOf(cpu_av_gov(context, 1))
    }
    var governor by rememberSaveable {
        mutableStateOf("")
    }
    var thermal by rememberSaveable {
        mutableStateOf(-1)
    }
    var adreno by rememberSaveable {
        mutableStateOf("")
    }
    val gki_mode by rememberSaveable {
        mutableStateOf(globalConfig.getBoolean(SpfConfig.GKI_MODE, false))
    }
    var getdata by rememberSaveable {
        mutableStateOf("")
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

    val getCPUGov by rememberSaveable {
        mutableStateOf(if (gov_ref.contains("schedutil"))
            "schedutil"
        else if (gov_ref.contains("walt"))
            "walt"
        else if (gov_ref.contains("game_walt"))
            "game_walt"
        else if (gov_ref.contains("game"))
            "game"
        else
            readKernel(context, 1, CPU_GOV))
    }
    val getThermal by rememberSaveable {
        mutableStateOf(if (gki_mode) 0 else -1)
    }

    fun loadData(){
        if (data.isEmpty()) {
            data = getGameList(context, packageName)
            if (data.isEmpty())
                data =
                    "{#pkg#: #$packageName#, #gm#: 0, #op#: 0, #gov#: #$getCPUGov#, #t#: $getThermal, #gpu#: 0}"
        }
        game_mode = strToInt(parserGameList(data, "gm"))
        oprofile = strToInt(parserGameList(data, "op"))
        governor = parserGameList(data, "gov")
        thermal = strToInt(parserGameList(data, "t"))
        adreno = parserGameList(data, "gpu")
    }
    loadData()

    var removeMode by rememberSaveable {
        mutableStateOf(false)
    }
    var getValueDialog by rememberSaveable {
        mutableStateOf("")
    }
    var resetTitletxt by rememberSaveable {
        mutableStateOf(if (getGameDef.contains(packageName)) context.getString(R.string.reset) else context.getString(R.string.remove))
    }
    var resetVisible by rememberSaveable {
        mutableStateOf(if (getGameNewList.contains(packageName)) true else false)
    }
    val resetVisible2 by rememberSaveable {
        mutableStateOf(globalConfig.getString(SpfConfig.PER_APP_LIST, "")!!.contains(packageName))
    }
    var getList by rememberSaveable {
        mutableStateOf(globalConfig.getString(SpfConfig.PER_APP_LIST, ""))
    }
    fun updateVisible(){
        if (!resetVisible) {
            resetTitletxt = if (getGameDef.contains(packageName)) context.getString(R.string.reset) else context.getString(R.string.remove)
            resetVisible = true
        }
        if (!getList!!.contains(packageName)) {
            if (getList!!.isEmpty())
                globalConfig.edit().putString(SpfConfig.PER_APP_LIST, packageName).apply()
            else
                globalConfig.edit().putString(SpfConfig.PER_APP_LIST, "$getList $packageName").apply()
        }
        getList = globalConfig.getString(SpfConfig.PER_APP_LIST, "")
    }

    fun setData(before: String, after: String){
        var dataSource = "${prefs.getString("game_ai_list", "")}"
        if (after.isNotEmpty()) {
            if (dataSource.isEmpty())
                dataSource = after
            else {
                if (dataSource.contains(before))
                    dataSource = dataSource.replace(before, after)
                else
                    dataSource = "$dataSource\n$after"
            }
            prefs.edit().putString("game_ai_list", dataSource).apply()
        }
        setGameList(context)

        // Reload after update data
        dataSource = "${prefs.getString("game_ai_list", "")}"
        getGameNewList = dataSource
    }
    fun removeData(data: String){
        var dataSource = "${prefs.getString("game_ai_list", "")}"
        if (data.isNotEmpty()) {
            dataSource = dataSource.replace(data, "")
            prefs.edit().putString("game_ai_list", dataSource).apply()
            setGameList(context)
            reloadData = true
        }
    }

    var showDropdown by remember { mutableStateOf(false) }
    var refreshrete by rememberSaveable { mutableStateOf(
        perapp.getString(SpfConfig.REFRESH_RATE, "Default")
    )}
    var fps_monitor by rememberSaveable {
        mutableStateOf(perapp.getBoolean(SpfConfig.MONITOR_MINI, false))
    }
    var touch_sample by rememberSaveable {
        mutableStateOf(perapp.getBoolean(SpfConfig.TOUCH_SAMPLE, false))
    }
    var seek_microfon by rememberSaveable {
        mutableStateOf(perapp.getFloat(SpfConfig.SC_MICROFON, 0f))
    }
    var seek_earfon by rememberSaveable {
        mutableStateOf(perapp.getFloat(SpfConfig.SC_EARFON, 0f))
    }
    var switch_headfon by rememberSaveable {
        mutableStateOf(perapp.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false))
    }
    var seek_headfon by rememberSaveable {
        mutableStateOf(perapp.getFloat(SpfConfig.SC_HEADFON, 0f))
    }
    var seek_headfon2 by rememberSaveable {
        mutableStateOf(perapp.getFloat(SpfConfig.SC_HEADFON2, 0f))
    }
    var io_sched by rememberSaveable {
        mutableStateOf(perapp.getString("io_sched", getDefIOSched()))
    }
    var gpu_max_freq by rememberSaveable {
        mutableStateOf(perapp.getInt("gpu_max_freq", getGPUMaxFreq(context)))
    }
    var gpu_min_freq by rememberSaveable {
        mutableStateOf(perapp.getInt("gpu_min_freq", getGPUMinFreq(context)))
    }
    var gpu_gov by rememberSaveable {
        mutableStateOf(perapp.getString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)))
    }
    var gpu_def_power by rememberSaveable {
        mutableStateOf(perapp.getInt("gpu_def_power", gpuMaxPwrLevel(context)))
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
                    val getDataold: String
                    if (getValueDialog.isNotEmpty()) {
                        if (event.mode == 1) {
                            getdata = "#op#: $oprofile"
                            getDataold = data
                            data =
                                data.replace(getdata, "#op#: ${getProfileInt(context, getValueDialog)}")
                            oprofile = getProfileInt(context, getValueDialog)
                        } else {
                            getdata = "#gov#: #$governor#"
                            getDataold = data
                            data = data.replace(getdata, "#gov#: #$getValueDialog#")
                            governor = getValueDialog
                        }
                        setData(getDataold, data)
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getdata = ""
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
                            perapp.edit().putInt("gpu_max_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 1) {
                            gpu_min_freq = strToInt(getValueDialog)
                            perapp.edit().putInt("gpu_min_freq", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 2) {
                            gpu_gov = getValueDialog
                            perapp.edit().putString("gpu_gov", getValueDialog).apply()
                        } else if (event.mode == 3) {
                            gpu_def_power = strToInt(getValueDialog)
                            perapp.edit().putInt("gpu_def_power", strToInt(getValueDialog)).apply()
                        } else if (event.mode == 4) {
                            getdata = "#gpu#: $adreno"
                            val getdataold = data
                            data = data.replace(
                                getdata,
                                "#gpu#: ${parseAdrenoBoost(context, getValueDialog)}"
                            )
                            adreno = getValueDialog
                            setData(getdataold, data)
                        }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getdata = ""
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
                        perapp.edit().putString("io_sched", getValueDialog).apply()
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
            DialogType.Thermal -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        getdata = "#t#: $thermal"
                        val getdataold = data
                        data = data.replace(
                            getdata,
                            "#t#: ${getThermalInt(context, getValueDialog, gki_mode)}"
                        )
                        thermal = getThermalInt(context, getValueDialog, gki_mode)
                        setData(getdataold, data)
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
        }
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
    fun resetValue(){
        perapp.edit().putString(SpfConfig.REFRESH_RATE, "Default").apply()
        perapp.edit().putBoolean(SpfConfig.MONITOR_MINI, false).apply()
        perapp.edit().putBoolean(SpfConfig.TOUCH_SAMPLE, false).apply()
        perapp.edit().putFloat(SpfConfig.SC_MICROFON, 0f).apply()
        perapp.edit().putFloat(SpfConfig.SC_EARFON, 0f).apply()
        perapp.edit().putBoolean(SpfConfig.SC_SWITCH_HEADFON, false).apply()
        perapp.edit().putFloat(SpfConfig.SC_HEADFON, 0f).apply()
        perapp.edit().putFloat(SpfConfig.SC_HEADFON2, 0f).apply()
        perapp.edit().putString("io_sched", getDefIOSched()).apply()
        perapp.edit().putInt("gpu_max_freq", getGPUMaxFreq(context)).apply()
        perapp.edit().putInt("gpu_min_freq", getGPUMinFreq(context)).apply()
        perapp.edit().putString("gpu_gov", readKernel(getGPUPath(context), GPU_GOV)).apply()
        perapp.edit().putInt("gpu_def_power", gpuMaxPwrLevel(context)).apply()
    }
    Scaffold(
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                appInfo = appInfo,
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false,
                visibleReset = resetVisible || resetVisible2,
                titleReset = resetTitletxt,
                onClick = {
                    removeMode = true
                    removeData(data)
                    if (getList!!.contains(" $packageName"))
                        getList = getList!!.replace(" $packageName", "")
                    else if (getList!!.contains("$packageName "))
                        getList = getList!!.replace("$packageName ", "")
                    else
                        getList = getList!!.replace(packageName, "")
                    globalConfig.edit().putString(SpfConfig.PER_APP_LIST, getList).apply()
                    resetValue()
                    RootUtils.runCommand("rm -fr /data/data/${context.packageName}/shared_prefs/$packageName.xml")
                    navigator.popBackStack()
                    removeMode = false
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
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp)
                ) {
                    SwitchItem(
                        title = stringResource(id = R.string.game_mode),
                        checked = game_mode == 1,
                        onCheckedChange = {
                            if (removeMode)
                                game_mode = if (it) 1 else 0
                            else {
                                getdata = "#gm#: $game_mode"
                                val getdataold = data
                                game_mode = if (it) 1 else 0
                                data =
                                    data.replace(getdata, "#gm#: $game_mode")

                                setData(getdataold, data)
                                updateVisible()
                            }
                        },
                    )
                }
            }

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
                            perapp.edit().putString(SpfConfig.REFRESH_RATE, "$reason").apply()
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
                        perapp.edit().putBoolean(SpfConfig.MONITOR_MINI, it).apply()
                        fps_monitor = it
                        updateVisible()
                    }
                }
            }

            if (hasModule(TOUCH_SAMPLE)) {
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
                            perapp.edit().putBoolean(SpfConfig.TOUCH_SAMPLE, it).apply()
                            touch_sample = it
                            updateVisible()
                        }
                    }
                }
            }

            ElevatedCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEvent = DialogEvent(
                                DialogType.Cpu, 0, 1,
                                "Profile Mode",
                                "${context.getString(R.string.balance)} ${context.getString(R.string.performance)} ${context.getString(R.string.game)} ${context.getString(R.string.battery)}",
                                getProfileString(context, oprofile))

                        }
                        .padding(vertical = 10.dp, horizontal = 22.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterStart),
                        text = "Profile Mode"
                    )
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterEnd),
                        text = getProfileString(context, oprofile),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ElevatedCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEvent = DialogEvent(
                                DialogType.Cpu, 1, 0,
                                "CPU Governor",
                                cpu_av_gov(context, 1),
                                governor)
                        }
                        .padding(vertical = 10.dp, horizontal = 22.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterStart),
                        text = "CPU Governor"
                    )
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterEnd),
                        text = governor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            ElevatedCard {
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
                            setGpu("Choose GPU Max Freq", 0)
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
                            setGpu("Choose GPU Min Freq", 1)
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
                            setGpu("Choose GPU Governor", 2)
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
                            setGpu("Choose GPU Default Power Level", 3)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEvent = DialogEvent(
                                DialogType.Gpu, 0, 4,
                                "Adreno Boost",
                                "${context.getString(R.string.off)} ${context.getString(R.string.low)} ${context.getString(R.string.medium)} ${context.getString(R.string.high)}",
                                parseAdrenoBoost(context, strToInt(adreno))
                            )
                        }
                        .padding(vertical = 10.dp, horizontal = 22.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterStart),
                        text = "Adreno Boost"
                    )
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterEnd),
                        text = parseAdrenoBoost(context, strToInt(adreno)),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ElevatedCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEvent = DialogEvent(
                                DialogType.Thermal, 0, 0,
                                "Thermal",
                                thermalList(context, gki_mode),
                                getThermalString(context, thermal, gki_mode))
                        }
                        .padding(vertical = 10.dp, horizontal = 22.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterStart),
                        text = "Thermal"
                    )
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterEnd),
                        text = getThermalString(context, thermal, gki_mode),
                        textAlign = TextAlign.Center
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
                            dialogEvent = DialogEvent(
                                DialogType.Io, 0, 0,
                                "Choose IO Scheduler",
                                getListIOSched(),
                                "${perapp.getString("io_sched", getDefIOSched())}"
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
                                    perapp.edit().putFloat(SpfConfig.SC_MICROFON, it).apply()
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
                                    perapp.edit().putFloat(SpfConfig.SC_EARFON, it).apply()
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
                            perapp.edit().putBoolean("sc_switch_headfon", it).apply()
                            switch_headfon = it
                            if (!switch_headfon) {
                                seek_headfon2 = seek_headfon
                                perapp.edit()
                                    .putFloat("sc_headfon2", seek_headfon2)
                                    .apply()
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
                                    perapp.edit().putFloat(SpfConfig.SC_HEADFON, it).apply()
                                    if (!switch_headfon) {
                                        perapp.edit()
                                            .putFloat(SpfConfig.SC_HEADFON2, it)
                                            .apply()
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
                                        perapp.edit()
                                            .putFloat(SpfConfig.SC_HEADFON2, it)
                                            .apply()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit,
    appInfo: AIViewModel.AppInfo,
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
private fun AppGameAIPreview() {
}

