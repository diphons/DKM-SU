package com.diphons.dkmsu.ui.screen

import android.content.Context
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diphons.dkmsu.R
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

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                        prefs.edit().putString(SpfConfig.DYNAMIC_FPS, refreshrete).apply()
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
                        prefs.edit().putString(SpfConfig.REFRESH_RATE, "$reason").apply()
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(stringResource(R.string.display_settings)) },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior,
        modifier = Modifier
            .graphicsLayer {
                shape = RoundedCornerShape(
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                )
                clip = true
            }
    )
}

@Preview
@Composable
private fun KernelPreview() {
    DisplayScreen(EmptyDestinationsNavigator)
}
