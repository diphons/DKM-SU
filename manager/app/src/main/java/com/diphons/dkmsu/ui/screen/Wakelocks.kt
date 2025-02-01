package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SearchAppBar
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.util.Utils.BOEFFLA_WL_BLOCKER
import com.diphons.dkmsu.ui.util.forceReloadWlList
import com.diphons.dkmsu.ui.util.genWlList
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.wakelockTimesFormat
import com.diphons.dkmsu.ui.viewmodel.WakelockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun WakelocksScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<WakelockViewModel>()
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    viewModel.sortAToZ = prefs.getBoolean("wakelock_sort_a_to_z", true)
    viewModel.sortZToA = prefs.getBoolean("wakelock_sort_z_to_a", false)

    if (forceReloadWlList) {
        if (RootUtils.runAndGetOutput("getprop init.dkmsvc.wl").contains("0")) {
            RootUtils.runCommand("rm -f /data/data/${context.packageName}/files/wakelock.json")
            RootUtils.runCommand("setprop init.dkmsvc.wl 1; dkmsvc wakelock file_bg")
        }
        forceReloadWlList = false
        genWlList = ""
        viewModel.emptyWakelock()
    }
    LaunchedEffect(Unit) {
        if (genWlList.isEmpty() || viewModel.wakelockList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchWakelockList()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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
    var wakelock_block by rememberSaveable {
        mutableStateOf(prefs.getString(SpfConfig.WAKELOCK_BLOCKER, ""))
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
                    title = {},
                    actions = {
                        SearchAppBar(
                            title = { Text("Wakelocks") },
                            searchText = viewModel.search,
                            onSearchTextChange = { viewModel.search = it },
                            onClearClick = { viewModel.search = "" },
                            onBackClick = onBack,
                            dropdownContent = {
                                var showDropdown by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showDropdown = true },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = stringResource(id = R.string.settings)
                                    )
                                    DropdownMenu(
                                        expanded = showDropdown,
                                        onDismissRequest = {
                                            showDropdown = false
                                        }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.module_sort_a_to_z))
                                            },
                                            trailingIcon = {
                                                Checkbox(
                                                    checked = viewModel.sortAToZ,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                viewModel.sortAToZ = !viewModel.sortAToZ
                                                viewModel.sortZToA = false
                                                prefs.edit()
                                                    .putBoolean(
                                                        "wakelock_sort_a_to_z",
                                                        viewModel.sortAToZ
                                                    )
                                                    .putBoolean("wakelock_sort_z_to_a", false)
                                                    .apply()
                                                scope.launch {
                                                    viewModel.fetchWakelockList()
                                                }
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.module_sort_z_to_a))
                                            },
                                            trailingIcon = {
                                                Checkbox(
                                                    checked = viewModel.sortZToA,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                viewModel.sortZToA = !viewModel.sortZToA
                                                viewModel.sortAToZ = false
                                                prefs.edit()
                                                    .putBoolean(
                                                        "wakelock_sort_z_to_a",
                                                        viewModel.sortZToA
                                                    )
                                                    .putBoolean("wakelock_sort_a_to_z", false)
                                                    .apply()
                                                scope.launch {
                                                    viewModel.fetchWakelockList()
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(if (scrollPos > 0.1f) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(hostState = snackBarHost) }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(innerPadding),
            onRefresh = {
                viewModel.fetchWakelockList()
            },
            isRefreshing = viewModel.isRefreshing
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = remember {
                    PaddingValues(16.dp)
                },
            ) {
                items(viewModel.wakelockList) { wakelock ->
                    val scope = rememberCoroutineScope()
                    var mode_update by rememberSaveable {
                        mutableStateOf(if (wakelock.block == 1) true else false)
                    }
                    ElevatedCard{
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp, 18.dp, 22.dp, 12.dp)
                        ){
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = wakelock.name,
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily
                                )

                                Text(
                                    text = "${stringResource(R.string.wakelock_active)}: ${wakelock.active}",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = "${stringResource(R.string.wakelock_abort)}: ${wakelock.abort}",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                Text(
                                    text = "${stringResource(R.string.wakelock_times)}: ${wakelockTimesFormat(wakelock.times.toLong())}",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                )

                                if (hasModule(BOEFFLA_WL_BLOCKER) && mode_update) {
                                    Text(
                                        text = "${stringResource(R.string.status)}: ${stringResource(R.string.wakelock_block)}",
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily
                                    )
                                }
                            }
                            if (hasModule(BOEFFLA_WL_BLOCKER)) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        modifier = Modifier.defaultMinSize(52.dp, 32.dp),
                                        enabled = true,
                                        onClick = {
                                            scope.launch {
                                                if (hasModule(BOEFFLA_WL_BLOCKER)) {
                                                    if (mode_update) {
                                                        wakelock_block = wakelock_block!!.replace(";${wakelock.name}", "")
                                                            .replace("${wakelock.name};", "").replace(wakelock.name, "")
                                                            .replace(";;", "")
                                                    } else if (wakelock_block!!.isEmpty())
                                                        wakelock_block = wakelock.name
                                                    else
                                                        wakelock_block = "$wakelock_block;${wakelock.name}"
                                                    prefs.edit()
                                                        .putString(SpfConfig.WAKELOCK_BLOCKER, wakelock_block)
                                                        .apply()
                                                    mode_update = !mode_update
                                                    setKernel(wakelock_block!!.replace(";;", ";"), BOEFFLA_WL_BLOCKER)
                                                }
                                            }
                                        },
                                        contentPadding = ButtonDefaults.TextButtonContentPadding
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(20.dp),
                                            imageVector = if (mode_update) Icons.Outlined.Delete else Icons.Outlined.Add,
                                            contentDescription = null
                                        )
                                        Text(
                                            modifier = Modifier.padding(start = 7.dp),
                                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                            text = if (mode_update) stringResource(R.string.wakelock_btn_unblock) else stringResource(R.string.wakelock_btn_block)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // fix last item shadow incomplete in LazyColumn
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

@Preview
@Composable
fun WakelocksItemPreview() {
}
