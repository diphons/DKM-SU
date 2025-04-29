package com.diphons.dkmsu.ui.screen

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.text.isDigitsOnly
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.DialogSwap
import com.diphons.dkmsu.ui.component.DialogSwapLoad
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.SwapUtils
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.getSwapPosition
import com.diphons.dkmsu.ui.util.getSwapSize
import com.diphons.dkmsu.ui.util.mbToGB
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import java.util.Timer
import java.util.TimerTask

/**
 * @author diphons
 * @date 2025/02/25.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SwapScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    lateinit var activityManager: ActivityManager
    val swapUtils = SwapUtils()
    val myHandler = Handler(Looper.getMainLooper())

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager

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

    val info = ActivityManager.MemoryInfo().apply {
        activityManager.getMemoryInfo(this)
    }
    var swapEnable by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.SWAP_ENABLE, false))
    }
    var totalMem by rememberSaveable { mutableIntStateOf((info.totalMem / 1024 / 1024f).toInt()) }
    var swapInfo by rememberSaveable { mutableStateOf("") }
    var swapTotal by rememberSaveable { mutableIntStateOf(0) }
    var getSwapPos by rememberSaveable { mutableFloatStateOf(0f) }
    var swapSlider by rememberSaveable { mutableFloatStateOf(getSwapPosition(prefs.getInt(SpfConfig.SWAP_SIZE, getSwapSize(0f, totalMem)), totalMem)) }
    var swapAlgorithmCurr by rememberSaveable { mutableStateOf(swapUtils.compAlgorithm) }
    var swapAlgorithm by rememberSaveable { mutableStateOf(prefs.getString(SpfConfig.SWAP_ALGORITHM, swapAlgorithmCurr)) }
    var swapBoot by rememberSaveable { mutableStateOf(prefs.getBoolean(SpfConfig.SWAP_BOOT, false)) }

    fun loadSwap(){
        totalMem = (info.totalMem / 1024 / 1024f).toInt()
        swapInfo = KeepShellPublic.doCmdSync("free -m | grep Swap")
        if (swapInfo.contains("Swap")) {
            try {
                val swapInfoStr = swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
                if (Regex("[\\d]+[\\s]+[\\d]+").matches(swapInfoStr)) {
                    swapTotal = swapInfoStr.substring(0, swapInfoStr.indexOf(" ")).trim().toInt()
                }
            } catch (_: java.lang.Exception) {
            }
        }
        if (!swapBoot)
            swapEnable = swapTotal > 0

        getSwapPos = if (swapTotal > 0)
            getSwapPosition(swapTotal, totalMem)
        else
            getSwapPosition(prefs.getInt(SpfConfig.SWAP_SIZE, 1024), totalMem)
        swapSlider = getSwapPos

        if (swapEnable && prefs.getInt(SpfConfig.SWAP_SIZE, 0) == 0)
            prefs.edit { putInt(SpfConfig.SWAP_SIZE, swapTotal) }
    }

    LaunchedEffect(Unit) {
        loadSwap()
    }
    val runDialog = rememberCustomDialog {
        DialogSwap(it)
    }
    val runDialogLoad = rememberCustomDialog {
        DialogSwapLoad()
    }

    var getValueDialog by rememberSaveable {
        mutableStateOf("")
    }

    var dialogEventSwap by remember {
        mutableStateOf<DialogEventSwap?>(value = null)
    }
    var getValueInput by rememberSaveable { mutableStateOf("") }
    var swappiness by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.SWAPPINESS, readKernel(VM_SWAPPINESS)))
    }
    var dirtyRatio by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.DIRTY_RATIO, readKernel(VM_DIRTY_RATIO)))
    }
    var dirtyBackgroundRatio by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.DIRTY_BACK_RATIO, readKernel(VM_DIRTY_BACK_RATIO)))
    }
    var dirtyWritebackCentisecs by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.DIRTY_WRITE_CENTISECS, readKernel(VM_DIRTY_WRITE_CENTISECS)))
    }
    var dirtyExpireCentisecs by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.DIRTY_EXPIRE_CENTISECS, readKernel(VM_DIRTY_EXPIRE_CENTISECS)))
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    @Composable
    fun inputDialog(
        title: String,
        value: String,
        mode: String,
        onDismissRequest: () -> Unit
    ){
        getValueInput = value
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
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ){
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = getValueInput,
                            onValueChange = {
                                if (it.isEmpty() || it.isDigitsOnly())
                                    getValueInput = it
                            },
                            placeholder = {
                                Text(
                                    text = "0"
                                )
                            },
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                }
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = run {
                                    MaterialTheme.colorScheme.primary
                                }),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .clickable {
                                        if (getValueInput.isNotEmpty()) {
                                            when (mode) {
                                                "0" -> {swappiness = getValueInput
                                                    prefs.edit { putString(SpfConfig.SWAPPINESS, swappiness) }
                                                    setKernel(swappiness!!, VM_SWAPPINESS)}
                                                "1" -> {dirtyRatio = getValueInput
                                                    prefs.edit { putString(SpfConfig.DIRTY_RATIO, dirtyRatio) }
                                                    setKernel(dirtyRatio!!, VM_DIRTY_RATIO)}
                                                "2" -> {dirtyBackgroundRatio = getValueInput
                                                    prefs.edit { putString(SpfConfig.DIRTY_BACK_RATIO, dirtyBackgroundRatio) }
                                                    setKernel(dirtyBackgroundRatio!!, VM_DIRTY_BACK_RATIO)}
                                                "3" -> {dirtyWritebackCentisecs = getValueInput
                                                    prefs.edit { putString(SpfConfig.DIRTY_WRITE_CENTISECS, dirtyWritebackCentisecs) }
                                                    setKernel(dirtyWritebackCentisecs!!, VM_DIRTY_WRITE_CENTISECS)}
                                                "4" -> {dirtyExpireCentisecs = getValueInput
                                                    prefs.edit { putString(SpfConfig.DIRTY_EXPIRE_CENTISECS, dirtyExpireCentisecs) }
                                                    setKernel(dirtyExpireCentisecs!!, VM_DIRTY_EXPIRE_CENTISECS)}
                                            }
                                        }
                                        dialogEventSwap = null
                                        getValueInput = ""
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp, horizontal = 25.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.apply),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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

    dialogEventSwap?.let { event ->
        when (event.type) {
            DialogTypeSwap.algorithm -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        swapAlgorithm = getValueDialog
                    }
                    dialogEventSwap = null
                    getValueDialog = ""
                }
            }
            DialogTypeSwap.inputmode -> {
                inputDialog(
                    title = event.title,
                    value = event.value,
                    mode = event.selected
                ) {
                    dialogEventSwap = null
                    getValueDialog = ""
                }
            }
        }
    }
    fun zramOffAwait(): Timer {
        val timer = Timer()
        val totalUsed = swapUtils.zramUsedSize
        val startTime = System.currentTimeMillis()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentUsed = swapUtils.zramUsedSize
                val avgSpeed = (totalUsed - currentUsed).toFloat() / (System.currentTimeMillis() - startTime) * 1000
                val tipStr = StringBuilder()
                tipStr.append(
                    String.format("${context.getString(R.string.ram_recycle)} ${currentUsed}/${totalUsed}MB (%.1fMB/s).", avgSpeed) +
                    if (avgSpeed > 0)
                        " ${context.getString(R.string.ram_approximately)} " + (currentUsed / avgSpeed).toInt() + context.getString(R.string.second)
                    else
                        " ${context.getString(R.string.please_wait)}~"
                )
                CMD_MSG = tipStr.toString()
                myHandler.post {
                    runDialogLoad.show()
                }
            }
        }, 0, 1000)
        return timer
    }

    val stop = Thread {
        if (swapTotal > 0) {
            val timer = zramOffAwait()
            swapUtils.zramOff()
            timer.cancel()
            swapTotal = 0
        }
        myHandler.post {
            runDialogLoad.hide()
        }
    }

    val run = Thread {
        var timer: Timer
        if (swapTotal > 0) {
            timer = zramOffAwait()
            swapUtils.zramOff()
            timer.cancel()
        }
        myHandler.post {
            runDialogLoad.hide()
        }
        swapTotal = getSwapSize(swapSlider, totalMem)
        prefs.edit { putInt(SpfConfig.SWAP_SIZE, swapTotal) }
        prefs.edit { putString(SpfConfig.SWAP_ALGORITHM, swapAlgorithm) }
        getSwapPos = swapSlider
        swapAlgorithmCurr = swapAlgorithm!!
        CMD_MSG = "${context.getString(R.string.set)} ZRAM ${context.getString(R.string.to)} ${mbToGB(swapTotal)} GB"
        myHandler.post {
            runDialog.show()
        }
        swapUtils.resizeZram(prefs.getInt(SpfConfig.SWAP_SIZE, getSwapSize(swapSlider, totalMem)), swapAlgorithm!!)
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                CMD_MSG = "${context.getString(R.string.set)} ZRAM ${context.getString(R.string.finish)}"
                myHandler.post {
                    runDialog.show()
                }
            }
        }, 0, 1000)
        timer.cancel()
        runDialog.hide()
    }

    fun applySwap(){
        Thread(run).start()
    }
    Scaffold(
        topBar = {
            val onBack: () -> Unit = {
                navigator.popBackStack()
            }
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                if (scrollPos > 0.1f) {
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
                    title = { Text(stringResource(R.string.memory)) },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    },
                    actions = {
                        Switch(
                            modifier = Modifier
                                .padding(end = 10.dp),
                            checked = swapBoot,
                            onCheckedChange = {
                                swapBoot = it
                                prefs.edit { putBoolean(SpfConfig.SWAP_BOOT, it) }
                                if (it && swapEnable && swapTotal <= 0)
                                    applySwap()
                            }
                        )
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(if (scrollPos > 0.1f) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background),
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
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        floatingActionButton = {
            val strApply = stringResource(id = R.string.apply)
            if (swapEnable && (swapSlider != getSwapPos || swapAlgorithm != swapAlgorithmCurr)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        applySwap()
                    },
                    icon = { Icon(Icons.Filled.Check, strApply) },
                    text = { Text(text = strApply) },
                )
            }
        }
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
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                swapEnable = !swapEnable
                                prefs.edit { putBoolean(SpfConfig.SWAP_ENABLE, swapEnable) }
                                if (swapEnable)
                                    applySwap()
                                else
                                    Thread(stop).start()
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "ZRAM",
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                            checked = swapEnable,
                            onCheckedChange = {
                                swapEnable = it
                                prefs.edit { putBoolean(SpfConfig.SWAP_ENABLE, it) }
                                if (it)
                                    applySwap()
                                else
                                    Thread(stop).start()
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dialogEventSwap = DialogEventSwap(
                                    DialogTypeSwap.algorithm,
                                    "${context.getString(R.string.choose)} ${context.getString(R.string.ram_algorithm)}",
                                    swapUtils.compAlgorithmOptions,
                                    swapAlgorithm!!
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = stringResource(R.string.ram_algorithm),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = swapAlgorithm!!,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 5.dp, end = 16.dp),
                    ){
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopStart),
                            text = stringResource(R.string.size),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopEnd),
                            text = "${mbToGB(getSwapSize(swapSlider, totalMem))} GB",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 9.dp, end = 9.dp),
                    ) {
                        Slider(
                            valueRange = 0f..0.3f,
                            steps = 2,
                            value = swapSlider,
                            onValueChange = {
                                swapSlider = it
                                if (!swapEnable)
                                    prefs.edit { putInt(SpfConfig.SWAP_SIZE, getSwapSize(swapSlider, totalMem)) }
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
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp),
                        text = "● ${stringResource(R.string.device_memory)}: ${mbToGB(totalMem) + 1} GB",
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (swapTotal > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 8.dp, end = 16.dp),
                            text = "● ${stringResource(R.string.zram_memory)}: ${if (!swapRun) "${mbToGB(swapTotal)} GB" else stringResource(R.string.ram_recycle)}",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEventSwap = DialogEventSwap(
                                DialogTypeSwap.inputmode,
                                "${context.getString(R.string.set)} swappiness",
                                swappiness!!,
                                "0"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Swappiness",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$swappiness%",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEventSwap = DialogEventSwap(
                                DialogTypeSwap.inputmode,
                                "${context.getString(R.string.set)} Dirty ratio",
                                dirtyRatio!!,
                                "1"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Dirty ratio",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dirtyRatio!!,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEventSwap = DialogEventSwap(
                                DialogTypeSwap.inputmode,
                                "${context.getString(R.string.set)} Dirty background ratio",
                                dirtyBackgroundRatio!!,
                                "2"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Dirty background ratio",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dirtyBackgroundRatio!!,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEventSwap = DialogEventSwap(
                                DialogTypeSwap.inputmode,
                                "${context.getString(R.string.set)} Dirty writeback centisecs",
                                dirtyWritebackCentisecs!!,
                                "3"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Dirty writeback centisecs",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dirtyWritebackCentisecs!!,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogEventSwap = DialogEventSwap(
                                DialogTypeSwap.inputmode,
                                "${context.getString(R.string.set)} Dirty expire centisecs",
                                dirtyExpireCentisecs!!,
                                "4"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Dirty expire centisecs",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd),
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dirtyExpireCentisecs!!,
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DialogEventSwap(
    val type: DialogTypeSwap,
    val title: String,
    val value: String,
    val selected: String
)

enum class DialogTypeSwap {
    algorithm,
    inputmode
}

@Preview
@Composable
private fun SwaplPreview() {
    SoundScreen(EmptyDestinationsNavigator)
}
