package com.diphons.dkmsu.ui.screen

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.core.text.isDigitsOnly
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.cpu_av_freq
import com.diphons.dkmsu.ui.util.getDefCIB
import com.diphons.dkmsu.ui.util.getMaxCluster
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.readKernel
import com.diphons.dkmsu.ui.util.stringToList
import com.diphons.dkmsu.ui.util.stringToList2
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * @author diphons
 * @date 2026/04/14.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CpuInputBoostScreen(
    navigator: DestinationsNavigator,
    packageName: String,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
    val defSpf = context.getSharedPreferences("cib", Context.MODE_PRIVATE)
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

    var getValueDialog by rememberSaveable {
        mutableStateOf("")
    }

    var primeCpu by rememberSaveable {
        mutableStateOf(getMaxCluster(context) > 2)
    }
    var dialogEventCIB by remember {
        mutableStateOf<DialogEventCIB?>(value = null)
    }
    var getValueInput by rememberSaveable { mutableStateOf("") }
    var cpu_freq_idle_big by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_idle_big", strToInt(readKernel(CPU_FREQ_IDLE_BIG))))
    }
    var cpu_freq_idle_little by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_idle_little", strToInt(readKernel(CPU_FREQ_IDLE_LITTLE))))
    }
    var cpu_freq_idle_prime by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_idle_prime", strToInt(readKernel(CPU_FREQ_IDLE_PRIME))))
    }
    var cpu_freq_min_big by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_min_big", strToInt(readKernel(CPU_FREQ_MIN_BIG))))
    }
    var cpu_freq_min_little by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_min_little", strToInt(readKernel(CPU_FREQ_MIN_LITTLE))))
    }
    var cpu_freq_min_prime by rememberSaveable {
        mutableIntStateOf(prefs.getInt("cpu_freq_min_prime", strToInt(readKernel(CPU_FREQ_MIN_PRIME))))
    }
    var input_boost_duration by rememberSaveable {
        mutableIntStateOf(prefs.getInt("input_boost_duration", strToInt(readKernel(INPUT_BOOST_DURATION))))
    }
    var input_boost_freq_big by rememberSaveable {
        mutableIntStateOf(prefs.getInt("input_boost_freq_big", strToInt(readKernel(INPUT_BOOST_FREQ_BIG))))
    }
    var input_boost_freq_little by rememberSaveable {
        mutableIntStateOf(prefs.getInt("input_boost_freq_little", strToInt(readKernel(INPUT_BOOST_FREQ_LITTLE))))
    }
    var input_boost_freq_prime by rememberSaveable {
        mutableIntStateOf(prefs.getInt("input_boost_freq_prime", strToInt(readKernel(INPUT_BOOST_FREQ_PRIME))))
    }
    var max_boost_freq_big by rememberSaveable {
        mutableIntStateOf(prefs.getInt("max_boost_freq_big", strToInt(readKernel(MAX_BOOST_FREQ_BIG))))
    }
    var max_boost_freq_little by rememberSaveable {
        mutableIntStateOf(prefs.getInt("max_boost_freq_little", strToInt(readKernel(MAX_BOOST_FREQ_LITTLE))))
    }
    var max_boost_freq_prime by rememberSaveable {
        mutableIntStateOf(prefs.getInt("max_boost_freq_prime", strToInt(readKernel(MAX_BOOST_FREQ_PRIME))))
    }
    var wake_boost_duration by rememberSaveable {
        mutableIntStateOf(prefs.getInt("wake_boost_duration", strToInt(readKernel(WAKE_BOOST_DURATION))))
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    var resetVisible by rememberSaveable {
        mutableStateOf(
            !defSpf.all.isEmpty() && (cpu_freq_idle_big != defSpf.getInt("cpu_freq_idle_big", strToInt(readKernel(CPU_FREQ_IDLE_BIG))) ||
                    cpu_freq_idle_little != defSpf.getInt("cpu_freq_idle_little", strToInt(readKernel(CPU_FREQ_IDLE_LITTLE))) ||
                    cpu_freq_idle_prime != defSpf.getInt("cpu_freq_idle_prime", strToInt(readKernel(CPU_FREQ_IDLE_PRIME))) ||
                    cpu_freq_min_big != defSpf.getInt("cpu_freq_min_big", strToInt(readKernel(CPU_FREQ_MIN_BIG))) ||
                    cpu_freq_min_little != defSpf.getInt("cpu_freq_min_little", strToInt(readKernel(CPU_FREQ_MIN_LITTLE))) ||
                    cpu_freq_min_prime != defSpf.getInt("cpu_freq_min_prime", strToInt(readKernel(CPU_FREQ_MIN_PRIME))) ||
                    input_boost_duration != defSpf.getInt("input_boost_duration", strToInt(readKernel(INPUT_BOOST_DURATION))) ||
                    input_boost_freq_big != defSpf.getInt("input_boost_freq_big", strToInt(readKernel(INPUT_BOOST_FREQ_BIG))) ||
                    input_boost_freq_little != defSpf.getInt("input_boost_freq_little", strToInt(readKernel(INPUT_BOOST_FREQ_LITTLE))) ||
                    input_boost_freq_prime != defSpf.getInt("input_boost_freq_prime", strToInt(readKernel(INPUT_BOOST_FREQ_PRIME))) ||
                    max_boost_freq_big != defSpf.getInt("max_boost_freq_big", strToInt(readKernel(MAX_BOOST_FREQ_BIG))) ||
                    max_boost_freq_little != defSpf.getInt("max_boost_freq_little", strToInt(readKernel(MAX_BOOST_FREQ_LITTLE))) ||
                    max_boost_freq_prime != defSpf.getInt("max_boost_freq_prime", strToInt(readKernel(MAX_BOOST_FREQ_PRIME))) ||
                    wake_boost_duration != defSpf.getInt("wake_boost_duration", strToInt(readKernel(WAKE_BOOST_DURATION))))
        )
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
    fun resetValue(){
        cpu_freq_idle_big = defSpf.getInt("cpu_freq_idle_big", strToInt(readKernel(CPU_FREQ_IDLE_BIG)))
        cpu_freq_idle_little = defSpf.getInt("cpu_freq_idle_little", strToInt(readKernel(CPU_FREQ_IDLE_LITTLE)))
        cpu_freq_idle_prime = defSpf.getInt("cpu_freq_idle_prime", strToInt(readKernel(CPU_FREQ_IDLE_PRIME)))
        cpu_freq_min_big = defSpf.getInt("cpu_freq_min_big", strToInt(readKernel(CPU_FREQ_MIN_BIG)))
        cpu_freq_min_little = defSpf.getInt("cpu_freq_min_little", strToInt(readKernel(CPU_FREQ_MIN_LITTLE)))
        cpu_freq_min_prime = defSpf.getInt("cpu_freq_min_prime", strToInt(readKernel(CPU_FREQ_MIN_PRIME)))
        input_boost_duration = defSpf.getInt("input_boost_duration", strToInt(readKernel(INPUT_BOOST_DURATION)))
        input_boost_freq_big = defSpf.getInt("input_boost_freq_big", strToInt(readKernel(INPUT_BOOST_FREQ_BIG)))
        input_boost_freq_little = defSpf.getInt("input_boost_freq_little", strToInt(readKernel(INPUT_BOOST_FREQ_LITTLE)))
        input_boost_freq_prime = defSpf.getInt("input_boost_freq_prime", strToInt(readKernel(INPUT_BOOST_FREQ_PRIME)))
        max_boost_freq_big = defSpf.getInt("max_boost_freq_big", strToInt(readKernel(MAX_BOOST_FREQ_BIG)))
        max_boost_freq_little = defSpf.getInt("max_boost_freq_little", strToInt(readKernel(MAX_BOOST_FREQ_LITTLE)))
        max_boost_freq_prime = defSpf.getInt("max_boost_freq_prime", strToInt(readKernel(MAX_BOOST_FREQ_PRIME)))
        wake_boost_duration = defSpf.getInt("wake_boost_duration", strToInt(readKernel(WAKE_BOOST_DURATION)))
        prefs.edit { putInt("cpu_freq_idle_big", cpu_freq_idle_big) }
        prefs.edit { putInt("cpu_freq_idle_little", cpu_freq_idle_little) }
        prefs.edit { putInt("cpu_freq_idle_prime", cpu_freq_idle_prime) }
        prefs.edit { putInt("cpu_freq_min_big", cpu_freq_min_big) }
        prefs.edit { putInt("cpu_freq_min_little", cpu_freq_min_little) }
        prefs.edit { putInt("cpu_freq_min_prime", cpu_freq_min_prime) }
        prefs.edit { putInt("input_boost_duration", input_boost_duration) }
        prefs.edit { putInt("input_boost_freq_big", input_boost_freq_big) }
        prefs.edit { putInt("input_boost_freq_little", input_boost_freq_little) }
        prefs.edit { putInt("input_boost_freq_prime", input_boost_freq_prime) }
        prefs.edit { putInt("max_boost_freq_big", max_boost_freq_big) }
        prefs.edit { putInt("max_boost_freq_little", max_boost_freq_little) }
        prefs.edit { putInt("max_boost_freq_prime", max_boost_freq_prime) }
        prefs.edit { putInt("wake_boost_duration", wake_boost_duration) }
        resetVisible = false
    }

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
                                                "0" -> {input_boost_duration = stringToInt(getValueInput)
                                                    prefs.edit { putInt("input_boost_duration", input_boost_duration) }}
                                                "1" -> {wake_boost_duration = stringToInt(getValueInput)
                                                    prefs.edit { putInt("wake_boost_duration", wake_boost_duration) }}
                                            }
                                            updateVisible()
                                        }
                                        dialogEventCIB = null
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

    var getModeSelect by rememberSaveable {
        mutableStateOf(0)
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
                        when (event.mode) {
                            0 -> {
                                var cpu: String
                                when (event.cpu) {
                                    4 -> {
                                        cpu = "elite"
                                    }

                                    3 -> {
                                        cpu = "prime"
                                        cpu_freq_idle_prime = strToInt(getValueDialog)
                                    }

                                    2 -> {
                                        cpu = "big"
                                        cpu_freq_idle_big = strToInt(getValueDialog)
                                    }

                                    else -> {
                                        cpu = "little"
                                        cpu_freq_idle_little = strToInt(getValueDialog)
                                    }
                                }

                                prefs.edit {
                                    putInt(
                                        "cpu_freq_idle_${cpu}",
                                        strToInt(getValueDialog)
                                    )
                                }
                            }
                            1 -> {
                                var cpu: String
                                when (event.cpu) {
                                    4 -> {
                                        cpu = "elite"
                                    }

                                    3 -> {
                                        cpu = "prime"
                                        cpu_freq_min_prime = strToInt(getValueDialog)
                                    }

                                    2 -> {
                                        cpu = "big"
                                        cpu_freq_min_big = strToInt(getValueDialog)
                                    }

                                    else -> {
                                        cpu = "little"
                                        cpu_freq_min_little = strToInt(getValueDialog)
                                    }
                                }

                                prefs.edit {
                                    putInt(
                                        "cpu_freq_min_${cpu}",
                                        strToInt(getValueDialog)
                                    )
                                }
                            }
                            2 -> {
                                var cpu: String
                                when (event.cpu) {
                                    4 -> cpu = "elite"
                                    3 -> {
                                        cpu = "prime"
                                        input_boost_freq_prime = strToInt(getValueDialog)
                                    }

                                    2 -> {
                                        cpu = "big"
                                        input_boost_freq_big = strToInt(getValueDialog)
                                    }

                                    else -> {
                                        cpu = "little"
                                        input_boost_freq_little = strToInt(getValueDialog)
                                    }
                                }

                                prefs.edit {
                                    putInt(
                                        "input_boost_freq_${cpu}",
                                        strToInt(getValueDialog)
                                    )
                                }
                            }
                            3 -> {
                                var cpu: String
                                when (event.cpu) {
                                    4 -> cpu = "elite"
                                    3 -> {
                                        cpu = "prime"
                                        max_boost_freq_prime = strToInt(getValueDialog)
                                    }

                                    2 -> {
                                        cpu = "big"
                                        max_boost_freq_big = strToInt(getValueDialog)
                                    }

                                    else -> {
                                        cpu = "little"
                                        max_boost_freq_little = strToInt(getValueDialog)
                                    }
                                }

                                prefs.edit {
                                    putInt(
                                        "max_boost_freq_${cpu}",
                                        strToInt(getValueDialog)
                                    )
                                }
                            }
                        }
                        updateVisible()
                    }
                    dialogEvent = null
                    getValueDialog = ""
                    getModeSelect = 0
                }
            }
            DialogType.Gpu -> {

            }
            DialogType.Io -> {

            }
            DialogType.Thermal -> {

            }
            DialogType.Tcp -> {

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
        val loc = when (cpu) {
            4 -> "elite"
            3 -> "prime"
            2 -> "big"
            else -> "little"
        }
        when (mode) {
            0 -> cpu_read =
                "${
                    prefs.getInt(
                        "cpu_freq_idle_${loc}",
                        strToInt(readKernel(CPU_INPUT_BOOST + "cpu_freq_idle_${loc}"))
                    )
                }"
            1 -> cpu_read =
                "${
                    prefs.getInt(
                        "cpu_freq_min_${loc}",
                        strToInt(readKernel(CPU_INPUT_BOOST + "cpu_freq_min_${loc}"))
                    )
                }"
            2 -> cpu_read =
                "${
                    prefs.getInt(
                        "input_boost_freq_${loc}",
                        strToInt(readKernel(CPU_INPUT_BOOST + "input_boost_freq_${loc}"))
                    )
                }"
            3 -> cpu_read =
                "${
                    prefs.getInt(
                        "max_boost_freq_${loc}",
                        strToInt(readKernel(CPU_INPUT_BOOST + "max_boost_freq_${loc}"))
                    )
                }"
        }
        cpu_read2 = reference
        dialogEvent = DialogEvent(DialogType.Cpu, cpu, mode, title, cpu_read2, cpu_read)
    }
    dialogEventCIB?.let { event ->
        when (event.type) {
            DialogTypeCIB.inputmode -> {
                inputDialog(
                    title = event.title,
                    value = event.value,
                    mode = event.selected
                ) {
                    dialogEventCIB = null
                    getValueDialog = ""
                }
            }
        }
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
                    title = { Text("CPU Input Boost") },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    },
                    actions = {
                        if (resetVisible) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = run {
                                    MaterialTheme.colorScheme.primary
                                }),
                                modifier = Modifier
                                    .padding(end = 14.dp)
                                    .clickable {
                                        resetValue()
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        getDefCIB(defSpf)
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (hasModule(CPU_FREQ_IDLE_LITTLE)) {
                ElevatedCard (
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 15.dp, bottom = 10.dp),
                        text = "CPU Freq Idle",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (primeCpu) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 0
                                    setCpu(
                                        "Choose CPU freq idle prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        "$cpu_freq_idle_prime"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "CPU Prime",
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodySmall
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
                                        text = "$cpu_freq_idle_prime",
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 0
                                setCpu(
                                    "Choose CPU freq idle big",
                                    2,
                                    getModeSelect,
                                    cpu_av_freq(context, 2),
                                    "$cpu_freq_idle_big"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Big",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$cpu_freq_idle_big",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 0
                                setCpu(
                                    "Choose CPU freq idle little",
                                    1,
                                    getModeSelect,
                                    cpu_av_freq(context, 1),
                                    "$cpu_freq_idle_little"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Little",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$cpu_freq_idle_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (hasModule(CPU_FREQ_MIN_LITTLE)) {
                ElevatedCard (
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 15.dp, bottom = 10.dp),
                        text = "CPU Freq Min",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (primeCpu) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 1
                                    setCpu(
                                        "Choose CPU freq min prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        "$cpu_freq_min_prime"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "CPU Prime",
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodySmall
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
                                        text = "$cpu_freq_min_prime",
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 1
                                setCpu(
                                    "Choose CPU freq min big",
                                    2,
                                    getModeSelect,
                                    cpu_av_freq(context, 2),
                                    "$cpu_freq_min_big"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Big",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$cpu_freq_min_big",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 1
                                setCpu(
                                    "Choose CPU freq min little",
                                    1,
                                    getModeSelect,
                                    cpu_av_freq(context, 1),
                                    "$cpu_freq_min_little"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Little",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$cpu_freq_min_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
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
                            dialogEventCIB = DialogEventCIB(
                                DialogTypeCIB.inputmode,
                                "${context.getString(R.string.set)} Input boost duration",
                                "${input_boost_duration}",
                                "0"
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "Input boost duration",
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
                                    text = "$input_boost_duration",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (hasModule(INPUT_BOOST_FREQ_LITTLE)) {
                ElevatedCard (
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 15.dp, bottom = 10.dp),
                        text = "Input Boost Freq",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (primeCpu) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 2
                                    setCpu(
                                        "Choose input boost freq prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        "$input_boost_freq_prime"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "CPU Prime",
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodySmall
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
                                        text = "$input_boost_freq_prime",
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                setCpu(
                                    "Choose input boost freq big",
                                    2,
                                    getModeSelect,
                                    cpu_av_freq(context, 2),
                                    "$input_boost_freq_big"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Big",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$input_boost_freq_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 2
                                setCpu(
                                    "Choose input boost freq little",
                                    1,
                                    getModeSelect,
                                    cpu_av_freq(context, 1),
                                    "$input_boost_freq_little"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Little",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$input_boost_freq_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (hasModule(MAX_BOOST_FREQ_LITTLE)) {

                ElevatedCard (
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 15.dp, bottom = 10.dp),
                        text = "Max Boost Freq",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (primeCpu) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    getModeSelect = 3
                                    setCpu(
                                        "Choose max boost freq prime",
                                        3,
                                        getModeSelect,
                                        cpu_av_freq(context, 3),
                                        "$max_boost_freq_prime"
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "CPU Prime",
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodySmall
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
                                        text = "$max_boost_freq_prime",
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 3
                                setCpu(
                                    "Choose max boost freq big",
                                    2,
                                    getModeSelect,
                                    cpu_av_freq(context, 2),
                                    "$max_boost_freq_big"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Big",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$max_boost_freq_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                getModeSelect = 3
                                setCpu(
                                    "Choose max boost freq little",
                                    1,
                                    getModeSelect,
                                    cpu_av_freq(context, 1),
                                    "$max_boost_freq_little"
                                )
                            }
                            .padding(vertical = 10.dp, horizontal = 22.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = "CPU Little",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.bodySmall
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
                                    text = "$max_boost_freq_little",
                                    fontSize = 15.sp,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (hasModule(WAKE_BOOST_DURATION)) {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dialogEventCIB = DialogEventCIB(
                                    DialogTypeCIB.inputmode,
                                    "${context.getString(R.string.set)} Wake boost duration",
                                    "$wake_boost_duration",
                                    "1"
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = "Wake boost duration",
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodySmall
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
                                        text = "$wake_boost_duration",
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.bodySmall,
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
}

data class DialogEventCIB(
    val type: DialogTypeCIB,
    val title: String,
    val value: String,
    val selected: String
)

enum class DialogTypeCIB {
    inputmode
}

@Preview
@Composable
private fun CIBPreview() {
}
