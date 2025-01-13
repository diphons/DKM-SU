package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.*
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.*
import com.ramcosta.composedestinations.generated.destinations.TouchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SoundScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DisplayScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PowerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PerfmodeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MiscScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @author diphons
 * @date 2024/12/30.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun KernelScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    val gameai = MutableLiveData<Boolean>(prefs.getBoolean(SpfConfig.GAME_AI, true))
    val perf_potition = MutableLiveData<Int>(prefs.getInt(SpfConfig.PROFILE_MODE, 0))
    val perf_mode = MutableLiveData<Boolean>(prefs.getBoolean(SpfConfig.PERF_MODE, true))
    val perf_status = MutableLiveData<String>("")
    val perf_profile = MutableLiveData<String>("")
    val chg_potition = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_MAX, 0))
    val get_current_max = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_CUR_MAX, 0))
    val chg_dynamic = MutableLiveData<Boolean>(prefs.getBoolean(SpfConfig.DYNAMIC_CHARGING, true))
    val chg_prof_0 = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_MAX_PROF_0, 0))
    val chg_prof_1 = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_MAX_PROF_1, 0))
    val chg_prof_2 = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_MAX_PROF_2, 0))
    val chg_prof_3 = MutableLiveData<Int>(prefs.getInt(SpfConfig.CHG_MAX_PROF_3, 0))
    val chg_profile_name = MutableLiveData<String>(prefs.getString(SpfConfig.CHG_MAX_NAME, "Default"))
    val chg_profile = MutableLiveData<String>("")
    val chg_current = MutableLiveData<String>("")

    fun getCurChg() {
        val check_profile_name = "$chg_profile_name"
        if (check_profile_name.contains("Default") || check_profile_name.isEmpty() || get_current_max.value == 0) {
            get_current_max.value = getCurrentCharger()
            prefs.edit().putInt(SpfConfig.CHG_CUR_MAX, get_current_max.value!!).apply()
        }
    }
    // Get chg max by current max
    fun getChgValue() {
        chg_prof_0.value = get_current_max.value
        chg_prof_3.value = get_current_max.value?.div(4)
        chg_prof_2.value = get_current_max.value?.div(2)
        chg_prof_1.value = chg_prof_3.value?.let { chg_prof_2.value?.plus(it) }
    }

    @Composable
    fun loadData(){
        perf_status.value = if (hasModule(GAME_AI)) {
            if (!gameai.value!!) {
                if (perf_mode.value!!)
                    stringResource(R.string.enabled)
                else
                    stringResource(R.string.selinux_status_disabled)
            } else
                stringResource(R.string.game_ai)
        } else {
            if (perf_mode.value!!)
                stringResource(R.string.enabled)
            else
                stringResource(R.string.selinux_status_disabled)
        }
        perf_profile.value = if (perf_potition.value == 1)
            stringResource(R.string.performance)
        else if (perf_potition.value == 2)
            stringResource(R.string.game)
        else if (perf_potition.value == 4)
            stringResource(R.string.battery)
        else if (perf_potition.value == 5)
            "Manual"
        else
            stringResource(R.string.balance)

        getCurChg()
        getChgValue()
        chg_profile.value = if (chg_dynamic.value!!) {
            stringResource(R.string.charger_dynamic)
        } else {
            "${stringResource(R.string.power_profile)} : ${chg_profile_name.value}"
        }
        chg_current.value = if (chg_potition.value == 0)
            "${chg_prof_0.value!! * 5.5 / 1000000} Watt"
        else if (chg_potition.value == 3)
            "${chg_prof_3.value!! * 5.5 / 1000000} Watt"
        else if (chg_potition.value == 2)
            "${chg_prof_2.value!! * 5.5 / 1000000} Watt"
        else
            "${chg_prof_1.value!! * 5.5 / 1000000} Watt"
    }
    loadData()

    //Toast.makeText(context, "${getMinFreq(context, 3)} - ${getMaxFreq(context, 3)}", Toast.LENGTH_LONG).show()
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

            loadData()
            Row (modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ){
                ElevatedCard (
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row (
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp)
                        ){
                            Icon(
                                Icons.Filled.KeyboardArrowDown, "",
                            )
                        }
                        Column (
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    navigator.navigate(PerfmodeScreenDestination)
                                }
                                .padding(22.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ){
                            Text(
                                modifier = Modifier,
                                text = stringResource(R.string.performance_mode),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                modifier = Modifier,
                                text = "${stringResource(R.string.status)} : ${perf_status.value}",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!gameai.value!!) {
                                Text(
                                    modifier = Modifier,
                                    text = "${stringResource(R.string.power_profile)} : ${perf_profile.value}",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                ElevatedCard (
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ){
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown, "",
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    navigator.navigate(PowerScreenDestination)
                                }
                                .padding(22.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                modifier = Modifier,
                                text = stringResource(R.string.power_settings),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                modifier = Modifier,
                                text = "${chg_profile.value}",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!chg_dynamic.value!!) {
                                Text(
                                    modifier = Modifier,
                                    text = "${stringResource(R.string.current)} : ${chg_current.value}",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            ElevatedCard {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.navigate(DisplayScreenDestination)
                    }
                    .padding(23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (modifier = Modifier
                        .weight(3f)
                    ){
                        Text(
                            text = stringResource(R.string.display_settings),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.display_settings_summary),
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
            if (hasModule(DT2W) || hasModule(DT2W_LEGACY) || hasModule(TOUCH_SAMPLE) || hasModule(
                    HAPTIC_LEVEL) || hasXiaomiDevice()
            ) {
                ElevatedCard {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator.navigate(TouchScreenDestination)
                        }
                        .padding(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column (modifier = Modifier
                            .weight(3f)
                        ){
                            Text(
                                text = stringResource(R.string.touch_mode),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.touch_mode_sum),
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }
            if (hasModule(SOUND_CONTROL)) {
                ElevatedCard {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navigator.navigate(SoundScreenDestination)
                        }
                        .padding(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column (modifier = Modifier
                            .weight(3f)
                        ){
                            Text(
                                text = stringResource(R.string.sound_control),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.sound_control_summary),
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }
            ElevatedCard {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.navigate(MiscScreenDestination)
                    }
                    .padding(23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (modifier = Modifier
                        .weight(3f)
                    ){
                        Text(
                            text = stringResource(R.string.misc),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.misc_sum),
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
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
        title = { Text(stringResource(R.string.home_kernel_tittle)) },
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
    KernelScreen(EmptyDestinationsNavigator)
}
