package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.edit
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.popup.FPSMonitor
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.RootUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.checkFPS
import com.diphons.dkmsu.ui.util.checkRefresRate
import com.diphons.dkmsu.ui.util.getBoard
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.getfpsbyid
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.setProfile
import com.diphons.dkmsu.ui.util.setRefreshRate
import com.ramcosta.composedestinations.generated.destinations.DisplayColorScreenDestination

/**
 * @author diphons
 * @date 2024/01/02.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DisplayScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

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
    Scaffold(
        topBar = {
            TopBar(
                onBack = {
                    navigator.popBackStack()
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
            val context = LocalContext.current
            val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

            ElevatedCard {
                var showDropdown by remember { mutableStateOf(false) }
                var refreshrete by rememberSaveable { mutableStateOf(
                    prefs.getString(SpfConfig.REFRESH_RATE, "60")
                )}
                var infinity_display by rememberSaveable { mutableStateOf(
                    prefs.getString(SpfConfig.DYNAMIC_FPS, "")
                )}
                if (getFPSCount() > 1) {
                    if (!checkRefresRate().contains("$refreshrete"))
                        refreshrete = checkRefresRate()
                    if (infinity_display!!.isEmpty() && refreshrete!!.contains("finity")) {
                        prefs.edit { putString(SpfConfig.DYNAMIC_FPS, refreshrete) }
                        infinity_display = "infinity"
                    }
                } else
                    refreshrete = getfpsbyid(1)

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
                        refreshrete = if (reason == 0) "infinity" else "$reason"
                        prefs.edit { putString(SpfConfig.REFRESH_RATE, "$reason") }
                        setRefreshRate(reason)
                        showDropdown = false
                    })
                }

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
                    ){
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
                                            .padding(top = 9.dp, bottom = 9.dp, start = 15.dp, end = 32.dp),
                                        text = "$refreshrete${if (refreshrete!!.contains("inity")) "" else " Hz"}",
                                        fontSize = 16.sp,
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center
                                    )

                                    Row (
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 6.dp)
                                    ){
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
                                        if (infinity_display!!.contains("finity"))
                                            RefreshRateDropdownItem("Infinity", reason = 0)
                                        for (i in 1 .. getFPSCount()) {
                                            RefreshRateDropdownItem("${getfpsbyid(i)} Hz", reason = strToInt(getfpsbyid(i)))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier
                        .height(10.dp))

                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                    ){
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = stringResource(R.string.refresh_rate_label),
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = stringResource(R.string.refresh_rate_label2),
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (hasModule(DISPLAY_RED)) {
                ElevatedCard {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator.navigate(DisplayColorScreenDestination)
                        }
                        .padding(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column (modifier = Modifier
                            .weight(3f)
                        ){
                            Text(
                                text = stringResource(R.string.color_calibration),
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.color_calibration_sum),
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }

            var fps_monitor by rememberSaveable {
                mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI, false))
            }
            var fps_monitor_mode by rememberSaveable {
                mutableStateOf(prefs.getInt(SpfConfig.MONITOR_MINI_MODE, 2))
            }
            var showFPSDropdown by remember { mutableStateOf(false) }
            @Composable
            fun FPSMonitorDropdownItem(title: String, reason: Int) {
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
                        fps_monitor_mode = reason
                        prefs.edit { putInt(SpfConfig.MONITOR_MINI_MODE, reason) }
                        showFPSDropdown = false
                        if (prefs.getBoolean(SpfConfig.MONITOR_MINI, false)) {
                            FPSMonitor(context).hidePopupWindow()
                            FPSMonitor(context).showPopupWindow()
                        }
                    })
            }

            ElevatedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 5.dp),
                    ){
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = stringResource(R.string.fps_monitor),
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
                                        showFPSDropdown = true
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(top = 9.dp, bottom = 9.dp, start = 15.dp, end = 32.dp),
                                        text = if (fps_monitor_mode == 0) stringResource(R.string.fps_only)
                                        else if (fps_monitor_mode == 1)
                                            stringResource(R.string.fps_scene)
                                        else
                                            stringResource(R.string.fps_oplus),
                                        fontSize = 16.sp,
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center
                                    )

                                    Row (
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 6.dp)
                                    ){
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown, "",
                                        )
                                    }
                                }
                                DropdownMenu(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    expanded = showFPSDropdown, onDismissRequest = {
                                        showFPSDropdown = false
                                    }) {
                                    FPSMonitorDropdownItem(stringResource(R.string.fps_only), reason = 0)
                                    FPSMonitorDropdownItem(stringResource(R.string.fps_scene), reason = 1)
                                    FPSMonitorDropdownItem(stringResource(R.string.fps_oplus), reason = 2)
                                }
                            }
                        }
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 5.dp),
                        text = if (fps_monitor_mode == 0)
                            stringResource(R.string.fps_only_info)
                        else if (fps_monitor_mode == 1)
                            stringResource(R.string.fps_scene_info)
                        else
                            stringResource(R.string.fps_oplus_info),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                    SwitchItem(
                        title = stringResource(R.string.fps_monitor_show),
                        checked = fps_monitor,
                        summary = if (fps_monitor)
                            stringResource(R.string.fps_monitor_show_disable)
                        else
                            stringResource(R.string.fps_monitor_show_enable)
                    ) {
                        if (it)
                            FPSMonitor(context).showPopupWindow()
                        else
                            FPSMonitor(context).hidePopupWindow()
                        prefs.edit { putBoolean(SpfConfig.MONITOR_MINI, it) }
                        fps_monitor = it
                    }
                    if (fps_monitor_mode == 2) {
                        var fps_monitor_small by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI_SMALL, false))
                        }
                        SwitchItem(
                            title = stringResource(R.string.fps_small_view),
                            checked = fps_monitor_small,
                            summary = if (fps_monitor_small)
                                stringResource(R.string.fps_small_view_disable)
                            else
                                stringResource(R.string.fps_small_view_enable)
                        ) {
                            prefs.edit { putBoolean(SpfConfig.MONITOR_MINI_SMALL, it) }
                            fps_monitor_small = it
                        }
                        if (fps_monitor_small) {
                            var fps_monitor_ram by rememberSaveable {
                                mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI_RAM, true))
                            }
                            var fps_monitor_bat by rememberSaveable {
                                mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI_BATT, true))
                            }
                            var fps_monitor_temp by rememberSaveable {
                                mutableStateOf(prefs.getBoolean(SpfConfig.MONITOR_MINI_TEMP, true))
                            }
                            SwitchItem(
                                title = stringResource(R.string.fps_battery_view),
                                checked = fps_monitor_bat,
                                summary = stringResource(R.string.fps_battery_view_sum)
                            ) {
                                prefs.edit { putBoolean(SpfConfig.MONITOR_MINI_BATT, it) }
                                fps_monitor_bat = it
                            }
                            SwitchItem(
                                title = stringResource(R.string.fps_temp_view),
                                checked = fps_monitor_temp,
                                summary = stringResource(R.string.fps_temp_view_sum)
                            ) {
                                prefs.edit { putBoolean(SpfConfig.MONITOR_MINI_TEMP, it) }
                                fps_monitor_temp = it
                            }
                            SwitchItem(
                                title = stringResource(R.string.fps_ram_view),
                                checked = fps_monitor_ram,
                                summary = stringResource(R.string.fps_ram_view_sum)
                            ) {
                                prefs.edit { putBoolean(SpfConfig.MONITOR_MINI_RAM, it) }
                                fps_monitor_ram = it
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
    borderVisible: Boolean
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
            title = { Text(stringResource(R.string.display_settings)) },
            navigationIcon = {
                IconButton(
                    onClick = onBack
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
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

@Preview
@Composable
private fun KernelPreview() {
    DisplayScreen(EmptyDestinationsNavigator)
}
