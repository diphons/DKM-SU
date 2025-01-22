package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diphons.dkmsu.R
import com.diphons.dkmsu.getKernelVersion
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.*
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
fun TouchScreen(navigator: DestinationsNavigator) {
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

            if (hasModule(DT2W) || hasModule(DT2W_LEGACY) || hasModule(TOUCH_SAMPLE) || hasModule(
                    HAPTIC_LEVEL) || (hasXiaomiDevice() && hasModule(getXMPath(context)))
            ) {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        if (hasModule(DT2W) || hasModule(DT2W_LEGACY) || hasXiaomiDevice()) {
                            var dt2w by rememberSaveable {
                                mutableStateOf(prefs.getBoolean(SpfConfig.DT2W, getDefDT2W()))
                            }
                            SwitchItem(
                                title = stringResource(id = R.string.dt2w),
                                checked = dt2w,
                                summary = stringResource(id = R.string.dt2w_summary)
                            ) {
                                if (getKernelVersion().isGKI()) {
                                    setXiaomiTouch(it, 14, context)
                                } else {
                                    if (it) {
                                        if (hasModule(DT2W))
                                            setKernel("1", DT2W)
                                        else
                                            setKernel("1", DT2W_LEGACY)
                                    } else {
                                        if (hasModule(DT2W))
                                            setKernel("0", DT2W)
                                        else
                                            setKernel("0", DT2W_LEGACY)
                                    }
                                }
                                prefs.edit().putBoolean(SpfConfig.DT2W, it).apply()
                                dt2w = it
                            }
                        }
                        if (hasModule(TOUCH_SAMPLE)) {
                            var touch_sample by rememberSaveable {
                                mutableStateOf(prefs.getBoolean(SpfConfig.TOUCH_SAMPLE, false))
                            }
                            SwitchItem(
                                title = stringResource(id = R.string.touch_sample),
                                checked = touch_sample,
                                summary = stringResource(id = R.string.touch_sample_sum)
                            ) {
                                if (it) {
                                    setKernel("1", TOUCH_SAMPLE)
                                } else {
                                    setKernel("0", TOUCH_SAMPLE)
                                }
                                prefs.edit().putBoolean(SpfConfig.TOUCH_SAMPLE, it).apply()
                                touch_sample = it
                            }
                        }
                        if (hasModule(HAPTIC_LEVEL)) {
                            var seek_haptic by rememberSaveable {
                                mutableStateOf(prefs.getFloat(SpfConfig.HAPTIC_LEVEL, 0.8f))
                            }
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 4.dp),
                                text = stringResource(id = R.string.seek_haptic)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 9.dp, end = 22.dp),
                            ) {
                                Slider(
                                    modifier = Modifier
                                        .padding(end = 50.dp),
                                    value = seek_haptic, onValueChange = {
                                        setKernel("${(it * 100).toInt()}", HAPTIC_LEVEL)
                                        prefs.edit().putFloat(SpfConfig.HAPTIC_LEVEL, it).apply()
                                        seek_haptic = it
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
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(bottom = 1.dp),
                                    text = "${(seek_haptic * 100).toInt()}%"
                                )
                            }
                        }
                    }
                }
            }

            if (hasXiaomiDevice() && hasModule(getXMPath(context))){
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 10.dp),
                            text = "Xiaomi Touch",
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        var switch_xt_game by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_GAME, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_game),
                            checked = switch_xt_game,
                            summary = stringResource(id = R.string.xt_game_sum)
                        ) {
                            setXiaomiTouch(it, 0, context)
                            prefs.edit().putBoolean(SpfConfig.XT_GAME, it).apply()
                            switch_xt_game = it
                        }
                        var switch_xt_active by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_ACTIVE, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_active),
                            checked = switch_xt_active
                        ) {
                            setXiaomiTouch(it, 1, context)
                            prefs.edit().putBoolean(SpfConfig.XT_ACTIVE, it).apply()
                            switch_xt_active = it
                        }
                        var switch_xt_up by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_UP, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_up),
                            checked = switch_xt_up,
                            summary = stringResource(id = R.string.xt_up_sum)
                        ) {
                            setXiaomiTouch(it, 2, context)
                            prefs.edit().putBoolean(SpfConfig.XT_UP, it).apply()
                            switch_xt_up = it
                        }
                        var switch_xt_tole by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_TOLE, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_tole),
                            checked = switch_xt_tole,
                        ) {
                            setXiaomiTouch(it, 3, context)
                            prefs.edit().putBoolean(SpfConfig.XT_TOLE, it).apply()
                            switch_xt_tole = it
                        }
                        var switch_xt_aim by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_AIM, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_aim),
                            checked = switch_xt_aim
                        ) {
                            setXiaomiTouch(it, 4, context)
                            prefs.edit().putBoolean(SpfConfig.XT_AIM, it).apply()
                            switch_xt_aim = it
                        }
                        var switch_xt_tap by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_TAP, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_tap),
                            checked = switch_xt_tap,
                            summary = stringResource(id = R.string.xt_tap_sum)
                        ) {
                            setXiaomiTouch(it, 5, context)
                            prefs.edit().putBoolean(SpfConfig.XT_TAP, it).apply()
                            switch_xt_tap = it
                        }
                        var switch_xt_exp by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_EXP, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_exp),
                            checked = switch_xt_exp
                        ) {
                            setXiaomiTouch(it, 6, context)
                            prefs.edit().putBoolean(SpfConfig.XT_EXP, it).apply()
                            switch_xt_exp = it
                        }
                        var switch_xt_edge by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_EDGE, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_edge),
                            checked = switch_xt_edge
                        ) {
                            setXiaomiTouch(it, 7, context)
                            prefs.edit().putBoolean(SpfConfig.XT_EDGE, it).apply()
                            switch_xt_edge = it
                        }
                        var switch_xt_grip by rememberSaveable {
                            mutableStateOf(prefs.getBoolean(SpfConfig.XT_GRIP, false))
                        }
                        SwitchItem(
                            title = stringResource(id = R.string.xt_grip),
                            checked = switch_xt_grip
                        ) {
                            setXiaomiTouch(it, 15, context)
                            prefs.edit().putBoolean(SpfConfig.XT_GRIP, it).apply()
                            switch_xt_grip = it
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
            title = { Text(stringResource(R.string.touch_mode)) },
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
    TouchScreen(EmptyDestinationsNavigator)
}