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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.edit
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.EARPIECE_GAIN
import com.diphons.dkmsu.ui.util.Utils.HEADPHONE_GAIN
import com.diphons.dkmsu.ui.util.Utils.MICROPHONE_GAIN
import com.diphons.dkmsu.ui.util.setKernel
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
fun SoundScreen(navigator: DestinationsNavigator) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    var seek_microfon by rememberSaveable {
                        mutableStateOf(prefs.getFloat(SpfConfig.SC_MICROFON, 0f))
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 5.dp),
                        text = stringResource(id = R.string.microphone_gain)
                    )
                    Box (
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 9.dp, end = 22.dp),
                    ){
                        Slider(
                            modifier = Modifier
                                .padding(end = 30.dp),
                            value = seek_microfon,
                            onValueChange = {
                                setKernel("${(it * 100).toInt() * 20 / 100}", MICROPHONE_GAIN)
                                prefs.edit { putFloat(SpfConfig.SC_MICROFON, it) }
                                seek_microfon = it
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.4f
                                )
                            ),
                            thumb = {
                                SliderDefaults.Thumb(interactionSource = remember {
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
                            text = "${(seek_microfon * 100).toInt() * 20 / 100}")
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    var seek_earfon by rememberSaveable {
                        mutableStateOf(prefs.getFloat(SpfConfig.SC_EARFON, 0f))
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 5.dp),
                        text = stringResource(id = R.string.earpiece_gain)
                    )
                    Box (
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 9.dp, end = 22.dp),
                    ){
                        Slider(
                            modifier = Modifier
                                .padding(end = 30.dp),
                            value = seek_earfon,
                            onValueChange = {
                                setKernel("${(it * 100).toInt() * 20 / 100}", EARPIECE_GAIN)
                                prefs.edit { putFloat(SpfConfig.SC_EARFON, it) }
                                seek_earfon = it
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.4f
                                )
                            ),
                            thumb = {
                                SliderDefaults.Thumb(interactionSource = remember {
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
                            text = "${(seek_earfon * 100).toInt() * 20 / 100}")
                    }

                    Spacer(modifier = Modifier.height(5.dp))
                    var switch_headfon by rememberSaveable {
                        mutableStateOf(prefs.getBoolean(SpfConfig.SC_SWITCH_HEADFON, false))
                    }
                    var seek_headfon by rememberSaveable {
                        mutableStateOf(prefs.getFloat(SpfConfig.SC_HEADFON, 0f))
                    }
                    var seek_headfon2 by rememberSaveable {
                        mutableStateOf(prefs.getFloat(SpfConfig.SC_HEADFON2, 0f))
                    }
                    SwitchItem(
                        title = stringResource(id = R.string.per_channel_controls),
                        checked = switch_headfon
                    ) {
                        prefs.edit { putBoolean("sc_switch_headfon", it) }
                        switch_headfon = it
                        if (!switch_headfon) {
                            seek_headfon2 = seek_headfon
                            prefs.edit { putFloat("sc_headfon2", seek_headfon2) }
                        }
                        setKernel("${(seek_headfon * 100).toInt() * 20 / 100} ${(seek_headfon2 * 100).toInt() * 20 / 100}", HEADPHONE_GAIN)
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 16.dp),
                        text = if (switch_headfon) stringResource(id = R.string.headphone_gain_left) else stringResource(id = R.string.headphone_gain)
                    )
                    Box (
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 9.dp, end = 22.dp),
                    ){
                        Slider(
                            modifier = Modifier
                                .padding(end = 30.dp),
                            value = seek_headfon,
                            onValueChange = {
                                prefs.edit { putFloat(SpfConfig.SC_HEADFON, it) }
                                if (!switch_headfon) {
                                    prefs.edit { putFloat(SpfConfig.SC_HEADFON2, it) }
                                    seek_headfon2 = it
                                }
                                seek_headfon = it
                                setKernel("${(seek_headfon * 100).toInt() * 20 / 100} ${(seek_headfon2 * 100).toInt() * 20 / 100}", HEADPHONE_GAIN)
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.4f
                                )
                            ),
                            thumb = {
                                SliderDefaults.Thumb(interactionSource = remember {
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
                            text = "${(seek_headfon * 100).toInt() * 20 / 100}")
                    }

                    if (switch_headfon) {
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp),
                            text = stringResource(id = R.string.headphone_gain_right)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Slider(
                                modifier = Modifier
                                    .padding(end = 30.dp),
                                value = seek_headfon2,
                                onValueChange = {
                                    prefs.edit { putFloat(SpfConfig.SC_HEADFON2, it) }
                                    seek_headfon2 = it
                                    setKernel("${(seek_headfon * 100).toInt() * 20 / 100} ${(seek_headfon2 * 100).toInt() * 20 / 100}", HEADPHONE_GAIN)
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
                                text = "${(seek_headfon2 * 100).toInt() * 20 / 100}"
                            )
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
            title = { Text(stringResource(R.string.sound_control)) },
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
