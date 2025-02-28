package com.diphons.dkmsu.ui.screen

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.diphons.dkmsu.Natives
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.runDialog
import com.diphons.dkmsu.ui.popup.PowerMenu
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
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SwapScreenDestination
import com.ramcosta.composedestinations.generated.destinations.WakelocksScreenDestination

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
    lateinit var activityManager: ActivityManager

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
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

    activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
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
    val ufs_healt_info = MutableLiveData<Float>(1f)
    val ramSlider = MutableLiveData<Float>(1f)
    val swapSlider = MutableLiveData<Float>(1f)
    val batt_healt_info = MutableLiveData<Int>(getBattHealth())
    var ufs_popup by rememberSaveable { mutableStateOf(false) }

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
            "${stringResource(R.string.power_profile)} : ${
                if (chg_potition.value == 0)
                "${chg_prof_0.value!! * 5.5 / 1000000} Watt"
                else if (chg_potition.value == 3)
                    "${chg_prof_3.value!! * 5.5 / 1000000} Watt"
                else if (chg_potition.value == 2)
                    "${chg_prof_2.value!! * 5.5 / 1000000} Watt"
                else
                    "${chg_prof_1.value!! * 5.5 / 1000000} Watt"
            }"
        }
        batt_healt_info.value = getBattHealth()
        if (batt_healt_info.value!! > 100)
            batt_healt_info.value = 100
    }
    loadData()

    //Toast.makeText(context, "${getMinFreq(context, 3)} - ${getMaxFreq(context, 3)}", Toast.LENGTH_LONG).show()
    Scaffold(
        topBar = {
            TopBar(
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination)
                },
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val runDialog = rememberCustomDialog {
            runDialog(it)
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
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
                            Text(
                                modifier = Modifier,
                                text = "${stringResource(R.string.health)} : ${batt_healt_info.value}%",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            ElevatedCard (
                modifier = Modifier
                    .clickable {
                        navigator.navigate(SwapScreenDestination)
                    }
            ){
                var totalMem by rememberSaveable { mutableIntStateOf(0) }
                var availMem by rememberSaveable { mutableIntStateOf(0) }
                var usedMem by rememberSaveable { mutableIntStateOf(0) }
                var swapInfo by rememberSaveable { mutableStateOf("") }
                var swapTotal by rememberSaveable { mutableIntStateOf(prefs.getInt(SpfConfig.SWAP_SIZE, 0)) }
                var swapUsed by rememberSaveable { mutableIntStateOf(0) }

                fun reloadSwap(){
                    val info = ActivityManager.MemoryInfo().apply {
                        activityManager.getMemoryInfo(this)
                    }
                    totalMem = (info.totalMem / 1024 / 1024f).toInt()
                    availMem = (info.availMem / 1024 / 1024f).toInt()
                    usedMem = totalMem - availMem
                    swapInfo = KeepShellPublic.doCmdSync("free -m | grep Swap")
                    if (swapInfo.contains("Swap")) {
                        try {
                            val swapInfoStr = swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
                            if (Regex("[\\d]+[\\s]+[\\d]+").matches(swapInfoStr)) {
                                swapTotal = swapInfoStr.substring(0, swapInfoStr.indexOf(" ")).trim().toInt()
                                swapUsed = swapInfoStr.substring(swapInfoStr.indexOf(" ")).trim().toInt()
                            }
                        } catch (_: java.lang.Exception) {
                        }
                    }
                    ramSlider.value = usedMem.toFloat() / totalMem.toFloat()
                    swapSlider.value = swapUsed.toFloat() / swapTotal.toFloat()
                }

                LaunchedEffect(Unit) {
                    reloadSwap()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, bottom = 5.dp, end = 24.dp)
                ) {
                    if (hasModule(UFS_HEALTH)) {
                        ufs_healt_info.value = (getUFSHelat().toFloat() / 100)
                        if (getUFSHelat() < 10 && !ufs_popup) {
                            ufs_popup = true
                            DIALOG_MODE = 2
                            runDialog.show()
                        }
                        Text(
                            text = stringResource(R.string.storage),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.TopStart),
                                text = stringResource(R.string.ufs_health_info),
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.TopEnd),
                                text = "${getUFSHelat()}%",
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        ProgressIndicator(
                            modifier = Modifier
                                .padding(top = 4.dp),
                            progress = ufs_healt_info.value!!
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ){
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopStart),
                            text = stringResource(R.string.device_memory),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopEnd),
                            text = "${mbToGB(totalMem) + 1} GB",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopStart),
                            text = "$usedMem MB ${stringResource(R.string.used)}",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopEnd),
                            text = "$availMem MB ${stringResource(R.string.free)}",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ProgressIndicator(
                        modifier = Modifier
                            .padding(top = 4.dp),
                        progress = ramSlider.value!!
                    )
                    if (swapTotal > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ){
                            Text(
                                modifier = Modifier
                                    .align(Alignment.TopStart),
                                text = stringResource(R.string.zram_memory),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.TopEnd),
                                text = if (!swapRun) "${mbToGB(swapTotal)} GB" else stringResource(R.string.ram_recycle),
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (!swapRun) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.TopStart),
                                    text = "$swapUsed MB ${stringResource(R.string.used)}",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd),
                                    text = "${swapTotal - swapUsed} MB ${stringResource(R.string.free)}",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            ProgressIndicator(
                                modifier = Modifier
                                    .padding(top = 4.dp),
                                progress = swapSlider.value!!
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box (
                        modifier = Modifier
                            .fillMaxWidth()
                    ){
                        Icon(
                            Icons.Filled.KeyboardArrowDown, "",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                        )
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
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            forceReloadWlList = true
                            navigator.navigate(WakelocksScreenDestination)
                        }
                        .padding(23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (modifier = Modifier
                        .weight(3f)
                    ){
                        Text(
                            text = "Wakelocks",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.wakelock_sum),
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onSettingsClick: () -> Unit,
    borderVisible: Boolean
) {
    val context = LocalContext.current
    Box(modifier = Modifier
        .fillMaxWidth()
    ){
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
            title = { Text(stringResource(R.string.home_kernel_tittle)) },
            actions = {
                val isManager = Natives.becomeManager(ksuApp.packageName)
                var showDropdown by remember { mutableStateOf(false) }
                if (isManager) {
                    IconButton(onClick = {
                        if (Settings.canDrawOverlays(context))
                            PowerMenu(context).open()
                        else
                            showDropdown = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(id = R.string.reboot)
                        )

                        DropdownMenu(expanded = showDropdown, onDismissRequest = {
                            showDropdown = false
                        }) {

                            RebootDropdownItem(id = R.string.shutdown, reason = "shutdown")
                            RebootDropdownItem(id = R.string.reboot)

                            val pm =
                                LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                            @Suppress("DEPRECATION")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                RebootDropdownItem(
                                    id = R.string.reboot_userspace,
                                    reason = "userspace"
                                )
                            }
                            RebootDropdownItem(id = R.string.reboot_recovery, reason = "recovery")
                            RebootDropdownItem(
                                id = R.string.reboot_bootloader,
                                reason = "bootloader"
                            )
                            RebootDropdownItem(id = R.string.reboot_download, reason = "download")
                            RebootDropdownItem(id = R.string.reboot_edl, reason = "edl")
                        }
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(id = R.string.settings)
                    )
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

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    clipShape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(clipShape)
            .height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                .fillMaxHeight()
                .fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
                .fillMaxWidth(progress)
        )
    }
}

@Preview
@Composable
private fun KernelPreview() {
    KernelScreen(EmptyDestinationsNavigator)
}
