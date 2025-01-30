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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.CHG_CUR_MAX
import com.diphons.dkmsu.ui.util.Utils.DYNAMIC_CHARGING
import com.diphons.dkmsu.ui.util.getCurrentCharger
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.setChgMax
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.Utils.*
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
fun PowerScreen(navigator: DestinationsNavigator) {
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
                var chgDropdown by remember { mutableStateOf(false) }
                var chg_potition by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_MAX, 0))
                }
                var chg_dynamic by rememberSaveable {
                    mutableStateOf(prefs.getBoolean(SpfConfig.DYNAMIC_CHARGING, true))
                }
                var get_current_max by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_CUR_MAX, 0))
                }
                var chg_prof_0 by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_MAX_PROF_0, 0))
                }
                var chg_prof_1 by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_MAX_PROF_1, 0))
                }
                var chg_prof_2 by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_MAX_PROF_2, 0))
                }
                var chg_prof_3 by rememberSaveable {
                    mutableStateOf(prefs.getInt(SpfConfig.CHG_MAX_PROF_3, 0))
                }
                var chg_profile_name by rememberSaveable {
                    mutableStateOf(prefs.getString(SpfConfig.CHG_MAX_NAME, "Default"))
                }

                fun getCurChg() {
                    val check_profile_name = "$chg_profile_name"
                    if (check_profile_name.contains("Default") || check_profile_name.isEmpty() || get_current_max == 0) {
                        get_current_max = getCurrentCharger()
                        prefs.edit().putInt(SpfConfig.CHG_CUR_MAX, get_current_max).apply()
                    }
                }
                // Get chg max by current max
                fun getChgValue() {
                    chg_prof_0 = get_current_max
                    chg_prof_3 = get_current_max / 4
                    chg_prof_2 = get_current_max / 2
                    chg_prof_1 = chg_prof_2 + chg_prof_3
                }
                getCurChg()
                getChgValue()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    @Composable
                    fun chgDropdownItem(title: String, reason: Int) {
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
                                chg_profile_name = title
                                prefs.edit().putString(SpfConfig.CHG_MAX_NAME, chg_profile_name).apply()
                                if (title.contains("Default")) {
                                    getCurChg()
                                } else {
                                    get_current_max = reason
                                    prefs.edit().putInt(SpfConfig.CHG_CUR_MAX, reason).apply()
                                }
                                getChgValue()
                                chg_potition = 0
                                setChgMax(prefs, chg_prof_0, chg_potition)
                                chgDropdown = false
                            })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    ){
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterStart),
                            text = stringResource(R.string.charger_max),
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (!chg_dynamic || !hasModule(DYNAMIC_CHARGING)) {
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
                                            chgDropdown = true
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
                                            text = "$chg_profile_name",
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
                                    DropdownMenu(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        expanded = chgDropdown, onDismissRequest = {
                                            chgDropdown = false
                                        }) {
                                        chgDropdownItem("Default", reason = 0)
                                        chgDropdownItem("120 Watt", reason = 21500000)
                                        chgDropdownItem("90 Watt", reason = 16400000)
                                        chgDropdownItem("67 Watt", reason = 12200000)
                                        chgDropdownItem("33 Watt", reason = 6000000)
                                        chgDropdownItem("18 Watt", reason = 3300000)
                                    }
                                }
                            }
                        }
                    }
                    if (hasModule(DYNAMIC_CHARGING)) {
                        SwitchItem(
                            title = stringResource(R.string.charger_dynamic),
                            checked = chg_dynamic,
                            summary = if (chg_dynamic) stringResource(R.string.charger_dynamic_sum_on) else stringResource(
                                R.string.charger_dynamic_sum_off
                            )
                        ) {
                            prefs.edit().putBoolean(SpfConfig.DYNAMIC_CHARGING, it).apply()
                            chg_dynamic = it
                            if (it) {
                                setKernel("1", DYNAMIC_CHARGING)
                                setKernel("$get_current_max", CHG_CUR_MAX)
                            } else {
                                setKernel("0", DYNAMIC_CHARGING)
                                setChgMax(prefs, prefs.getInt(SpfConfig.CHG_MAX_CUR_PROF, get_current_max), chg_potition)
                            }
                        }
                    }
                    if (!chg_dynamic || !hasModule(DYNAMIC_CHARGING)) {
                        if (!hasModule(DYNAMIC_CHARGING)) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    top = 2.dp,
                                    bottom = 16.dp,
                                    end = 16.dp
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = run {
                                            if (chg_potition == 0)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.background
                                        }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(end = 5.dp)
                                        .border(
                                            if (chg_potition == 0)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                chg_potition = 0
                                                setChgMax(prefs, chg_prof_0, chg_potition)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            text = "${chg_prof_0 * 5.5 / 1000000} Watt",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = run {
                                            if (chg_potition == 1)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.background
                                        }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(start = 5.dp)
                                        .border(
                                            if (chg_potition == 1)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                chg_potition = 1
                                                setChgMax(prefs, chg_prof_1, chg_potition)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            text = "${chg_prof_1 * 5.5 / 1000000} Watt",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = run {
                                            if (chg_potition == 2)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.background
                                        }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(end = 5.dp)
                                        .border(
                                            if (chg_potition == 2)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                chg_potition = 2
                                                setChgMax(prefs, chg_prof_2, chg_potition)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            text = "${chg_prof_2 * 5.5 / 1000000} Watt",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = run {
                                            if (chg_potition == 3)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.background
                                        }),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .weight(1F)
                                        .padding(start = 5.dp)
                                        .border(
                                            if (chg_potition == 3)
                                                0.dp
                                            else
                                                1.dp,
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.4f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                chg_potition = 3
                                                setChgMax(prefs, chg_prof_3, chg_potition)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            text = "${chg_prof_3 * 5.5 / 1000000} Watt",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = stringResource(R.string.charger_profile_warning),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (hasModule(SKIP_THERMAL)) {
                ElevatedCard {
                    var skip_thermal by rememberSaveable {
                        mutableStateOf(prefs.getBoolean(SpfConfig.SKIP_THERMAL, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.skip_thermal),
                            checked = skip_thermal,
                            summary = stringResource(R.string.skip_thermal_summary)
                        ) {
                            prefs.edit().putBoolean(SpfConfig.SKIP_THERMAL, it).apply()
                            skip_thermal = it
                            if (it) {
                                setKernel("1", SKIP_THERMAL)
                            } else {
                                setKernel("0", SKIP_THERMAL)
                            }
                        }
                    }
                }
            }
            if (hasModule(FCHG_SYS)) {
                ElevatedCard {
                    var fast_chg by rememberSaveable {
                        mutableStateOf(prefs.getBoolean(SpfConfig.FAST_CHARGING, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.fast_charging),
                            checked = fast_chg,
                            summary = stringResource(R.string.fast_charging_sum)
                        ) {
                            prefs.edit().putBoolean(SpfConfig.FAST_CHARGING, it).apply()
                            fast_chg = it
                            if (it) {
                                setKernel("1", FCHG_SYS)
                            } else {
                                setKernel("0", FCHG_SYS)
                            }
                        }
                    }
                }
            }
            if (hasModule(NIGHT_CHARGING)) {
                ElevatedCard {
                    var night_chg by rememberSaveable {
                        mutableStateOf(prefs.getBoolean(SpfConfig.NIGHT_CHARGING, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.night_charging),
                            checked = night_chg,
                        ) {
                            prefs.edit().putBoolean(SpfConfig.NIGHT_CHARGING, it).apply()
                            night_chg = it
                            if (it) {
                                setKernel("1", NIGHT_CHARGING)
                            } else {
                                setKernel("0", NIGHT_CHARGING)
                            }
                        }
                    }
                }
            }
            if (hasModule(STEP_CHARGING)) {
                ElevatedCard {
                    var step_chg by rememberSaveable {
                        mutableStateOf(prefs.getBoolean(SpfConfig.STEP_CHARGING, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.step_charging),
                            checked = step_chg,
                            summary = stringResource(
                                R.string.step_charging_sum
                            )
                        ) {
                            prefs.edit().putBoolean(SpfConfig.STEP_CHARGING, it).apply()
                            step_chg = it
                            if (it) {
                                setKernel("1", STEP_CHARGING)
                            } else {
                                setKernel("0", STEP_CHARGING)
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
            title = { Text(stringResource(R.string.power_settings)) },
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
    SoundScreen(EmptyDestinationsNavigator)
}
