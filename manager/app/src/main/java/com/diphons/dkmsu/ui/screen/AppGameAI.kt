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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.cpu_av_gov
import com.diphons.dkmsu.ui.util.forceStopApp
import com.diphons.dkmsu.ui.util.getGameDef
import com.diphons.dkmsu.ui.util.getGameList
import com.diphons.dkmsu.ui.util.getGameNewList
import com.diphons.dkmsu.ui.util.getProfileInt
import com.diphons.dkmsu.ui.util.getProfileString
import com.diphons.dkmsu.ui.util.getThermalInt
import com.diphons.dkmsu.ui.util.getThermalString
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
    var gki_mode by rememberSaveable {
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

    fun loadData(){
        data = getGameList(context, packageName)
        game_mode = if (data.isEmpty()) 0 else strToInt(parserGameList(data, "gm"))
        oprofile = if (data.isEmpty()) 0 else strToInt(parserGameList(data, "op"))
        governor = if (data.isEmpty()) {
            if (gov_ref.contains("schedutil"))
                "schedutil"
            else if (gov_ref.contains("walt"))
                "walt"
            else if (gov_ref.contains("game_walt"))
                "game_walt"
            else if (gov_ref.contains("game"))
                "game"
            else
                readKernel(context, 1, CPU_GOV)
        } else
            parserGameList(data, "gov")
        thermal = if (data.isEmpty()) -1 else strToInt(parserGameList(data, "t"))
        adreno = if (data.isEmpty()) "0" else parserGameList(data, "gpu")
        if (data.isEmpty())
            data = "{#pkg: #$packageName#, #gm#: $game_mode, #op#: $oprofile, #gov#: #$governor#, #t#: $thermal, #gpu#: $adreno}"
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
    fun updateVisible(){
        if (!resetVisible) {
            resetTitletxt = if (getGameDef.contains(packageName)) context.getString(R.string.reset) else context.getString(R.string.remove)
            resetVisible = true
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
    var dialogAIEvent by remember {
        mutableStateOf<DialogAIEvent?>(value = null)
    }
    dialogAIEvent?.let { event ->
        when (event.type) {
            DialogAIType.Op -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        getdata = "#op#: $oprofile"
                        val getdataold = data
                        data =
                            data.replace(getdata, "#op#: ${getProfileInt(context, getValueDialog)}")
                        oprofile = getProfileInt(context, getValueDialog)

                        setData(getdataold, data)
                        updateVisible()
                    }
                    dialogAIEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
            DialogAIType.Gov -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        getdata = "#gov#: #$governor#"
                        val getdataold = data
                        data = data.replace(getdata, "#gov#: #$getValueDialog#")
                        governor = getValueDialog
                        setData(getdataold, data)
                        updateVisible()
                    }
                    dialogAIEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
            DialogAIType.Gpu -> {
                listDialog(
                    text = event.title,
                    value = event.value,
                    selected = event.selected
                ) {
                    if (getValueDialog.isNotEmpty()) {
                        getdata = "#gpu#: $adreno"
                        val getdataold = data
                        data = data.replace(
                            getdata,
                            "#gpu#: ${parseAdrenoBoost(context, getValueDialog)}"
                        )
                        adreno = getValueDialog
                        setData(getdataold, data)
                        updateVisible()
                    }
                    dialogAIEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
            DialogAIType.Thermal -> {
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
                    dialogAIEvent = null
                    getValueDialog = ""
                    getdata = ""
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopBar(
                onBack = { navigator.popBackStack() },
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false,
                visibleReset = resetVisible,
                titleReset = resetTitletxt,
                onClick = {
                    removeMode = true
                    removeData(data)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    headlineContent = { Text(appInfo.label) },
                    supportingContent = { Text(packageName) },
                    leadingContent = appIcon,
                )
            }

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

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogAIEvent = DialogAIEvent(
                                DialogAIType.Op,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogAIEvent = DialogAIEvent(
                                DialogAIType.Gov,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogAIEvent = DialogAIEvent(
                                DialogAIType.Thermal,
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogAIEvent = DialogAIEvent(
                                DialogAIType.Gpu,
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
        }
    }
}

data class DialogAIEvent(
    val type: DialogAIType,
    val title: String,
    val value: String,
    val selected: String
)

enum class DialogAIType {
    Op,
    Gov,
    Gpu,
    Thermal
}

@Composable
private fun AppAIInner(
    modifier: Modifier = Modifier,
    packageName: String,
    appLabel: String,
    appIcon: @Composable () -> Unit,
    gameMode: Int,
    ongameModeChange: (Int) -> Unit,
    oprofile: Int,
    oprofileModeChange: (String) -> Unit,
    gov: String,
    govModeChange: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        AppMenuBox(packageName) {
            ListItem(
                headlineContent = { Text(appLabel) },
                supportingContent = { Text(packageName) },
                leadingContent = appIcon,
            )
        }

        SwitchItem(
            icon = Icons.Filled.AdminPanelSettings,
            title = "Game Mode",//stringResource(id = R.string.superuser),
            checked = gameMode == 1,
            onCheckedChange = { ongameModeChange(if (it) 1 else 0) },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    oprofileModeChange(
                        getProfileString(context, oprofile)
                    )
                }
                .padding(vertical = 10.dp, horizontal = 22.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                text = "Profile Mode",
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                text = getProfileString(context, oprofile),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    govModeChange(gov)
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
                text = gov,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    borderVisible: Boolean,
    visibleReset: Boolean,
    titleReset: String,
    onClick: () -> Unit
) {
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
                Text(stringResource(R.string.game_ai))
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
    var game by remember { mutableStateOf(0) }
    AppAIInner(
        packageName = "icu.nullptr.test",
        appLabel = "Test",
        appIcon = { Icon(Icons.Filled.Android, null) },
        gameMode = game,
        ongameModeChange = {
            game = it
        },
        oprofile = 0,
        oprofileModeChange = {},
        gov = "",
        govModeChange = {}
    )
}

