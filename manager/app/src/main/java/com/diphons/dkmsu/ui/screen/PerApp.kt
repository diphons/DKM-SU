package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SearchAppBar
import com.diphons.dkmsu.ui.store.SpfConfig
import com.diphons.dkmsu.ui.util.Utils.TOUCH_SAMPLE
import com.diphons.dkmsu.ui.util.getFPSCount
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.viewmodel.PerAppViewModel
import com.ramcosta.composedestinations.generated.destinations.AppPerProfileScreenDestination

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PerAppScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<PerAppViewModel>()
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
                            title = { Text(stringResource(R.string.per_app)) },
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
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(if (scrollPos > 0.1f) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background)
                        )
                    },
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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            onRefresh = {
                scope.launch { viewModel.fetchAppList() }
                //keepServiceMain(context)
            },
            isRefreshing = viewModel.isRefreshing
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                items(viewModel.appList, key = { it.packageName }) { app ->
                    AppItem(app) {
                        viewModel.refreshOnReturn = true
                        navigator.navigate(AppPerProfileScreenDestination(app))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: PerAppViewModel.AppInfo,
    onClickListener: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClickListener),
        headlineContent = { Text(app.label) },
        supportingContent = {
            Column {
                Text(app.packageName)
                FlowRow {
                    if (app.perApp) {
                        LabelText(label = "Per-App")
                        if (getFPSCount() > 1 && app.refreshRate.isNotEmpty())
                            LabelText(label = app.refreshRate)
                        if (app.fpsMoninor)
                            LabelText(label = "FPS Monitor")
                        if (hasModule(TOUCH_SAMPLE) && app.touchImprove)
                            LabelText(label = "Improve Touch")
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
