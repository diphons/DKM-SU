package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.setKernel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator

/**
 * @author diphons
 * @date 2024/01/04.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DisplayColorScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
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
    //Default value
    val displayRgb = 1f
    val displaySat = 0.6662165f
    val displayHue = 0f
    val displayAmoledRgb = 0.9040460f
    val displayAmoledSat = 0.71083844f
    val displayAmoledCont = 0.6870402f

    var seekRed by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_RED, displayRgb))
    }
    var seekGreen by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_GREEN, displayRgb))
    }
    var seekBlue by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_BLUE, displayRgb))
    }
    var seekSaturation by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_SATURATED, displaySat))
    }
    var seekValue by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_VALUE, displaySat))
    }
    var seekContrast by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_CONTRAST, displaySat))
    }
    var seekHue by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_HUE, displayHue))
    }
    var displayAmoled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.DISPLAY_AMOLED, false))
    }
    var onLoad by remember {mutableStateOf(false)}

    fun amoledColor(enabled: Boolean){
        if (enabled) {
            seekRed = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_RED, displayAmoledRgb)
            seekGreen = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_GREEN, displayAmoledRgb)
            seekBlue = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_BLUE, displayAmoledRgb)
            seekSaturation = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_SATURATED, displayAmoledSat)
            seekValue = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_VALUE, displaySat)
            seekContrast = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_CONTRAST, displayAmoledCont)
            seekHue = prefs.getFloat(SpfConfig.DISPLAY_AMOLED_HUE, displayHue)
        } else {
            seekRed = prefs.getFloat(SpfConfig.DISPLAY_RED, displayRgb)
            seekGreen = prefs.getFloat(SpfConfig.DISPLAY_GREEN, displayRgb)
            seekBlue = prefs.getFloat(SpfConfig.DISPLAY_BLUE, displayRgb)
            seekSaturation = prefs.getFloat(SpfConfig.DISPLAY_SATURATED, displaySat)
            seekValue = prefs.getFloat(SpfConfig.DISPLAY_VALUE, displaySat)
            seekContrast = prefs.getFloat(SpfConfig.DISPLAY_CONTRAST, displaySat)
            seekHue = prefs.getFloat(SpfConfig.DISPLAY_HUE, displayHue)
        }

        setKernel("${(seekRed * 256).toInt()}", DISPLAY_RED)
        setKernel("${(seekGreen * 256).toInt()}", DISPLAY_GREEN)
        setKernel("${(seekBlue * 256).toInt()}", DISPLAY_BLUE)
        setKernel("${(seekSaturation * 383).toInt()}", DISPLAY_SATURATED)
        setKernel("${(seekValue * 383).toInt()}", DISPLAY_VALUE)
        setKernel("${(seekContrast * 383).toInt()}", DISPLAY_CONTRAST)
        setKernel("${(seekHue * 1536).toInt()}", DISPLAY_HUE)
    }
    // Apply Amoled on load
    if (!onLoad) {
        onLoad = true
        amoledColor(displayAmoled)
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = {
                    navigator.popBackStack()
                },
                onResetClick = {
                    seekRed = if (displayAmoled) displayAmoledRgb else displayRgb
                    seekGreen = if (displayAmoled) displayAmoledRgb else displayRgb
                    seekBlue = if (displayAmoled) displayAmoledRgb else displayRgb
                    seekSaturation = if (displayAmoled) displayAmoledSat else displaySat
                    seekValue = displaySat
                    seekContrast = if (displayAmoled) displayAmoledCont else displaySat
                    seekHue = displayHue

                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_RED else SpfConfig.DISPLAY_RED, seekRed) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_GREEN else SpfConfig.DISPLAY_GREEN, seekGreen) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_BLUE else SpfConfig.DISPLAY_BLUE, seekBlue) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_SATURATED else SpfConfig.DISPLAY_SATURATED, seekSaturation) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_VALUE else SpfConfig.DISPLAY_VALUE, seekValue) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_CONTRAST else SpfConfig.DISPLAY_CONTRAST, seekContrast) }
                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_HUE else SpfConfig.DISPLAY_HUE, seekHue) }

                    setKernel("${(seekRed * 256).toInt()}", DISPLAY_RED)
                    setKernel("${(seekGreen * 256).toInt()}", DISPLAY_GREEN)
                    setKernel("${(seekBlue * 256).toInt()}", DISPLAY_BLUE)
                    setKernel("${(seekSaturation * 383).toInt()}", DISPLAY_SATURATED)
                    setKernel("${(seekValue * 383).toInt()}", DISPLAY_VALUE)
                    setKernel("${(seekContrast * 383).toInt()}", DISPLAY_CONTRAST)
                    setKernel("${(seekHue * 1536).toInt()}", DISPLAY_HUE)
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
            ElevatedCard {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(R.string.amoled_color),
                            checked = displayAmoled,
                            summary = stringResource(R.string.amoled_color_sum)
                        ) {
                            amoledColor(it)
                            prefs.edit { putBoolean(SpfConfig.DISPLAY_AMOLED, it) }
                            displayAmoled = it
                        }
                        Spacer(
                            modifier = Modifier
                                .height(10.dp)
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 5.dp),
                            text = stringResource(R.string.rgb_adjustments)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 7.dp, bottom = 1.dp),
                                text = "R",
                                color = Color.Red
                            )
                            Slider(
                                modifier = Modifier
                                    .padding(start = 30.dp, end = 40.dp),
                                value = seekRed,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_RED else SpfConfig.DISPLAY_RED, it) }
                                    seekRed = it
                                    setKernel("${(it * 256).toInt()}", DISPLAY_RED)
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
                                text = "${(seekRed * 256).toInt()}"
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 7.dp, bottom = 1.dp),
                                text = "G",
                                color = Color.Green
                            )
                            Slider(
                                modifier = Modifier
                                    .padding(start = 30.dp, end = 40.dp),
                                value = seekGreen,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_GREEN else SpfConfig.DISPLAY_GREEN, it) }
                                    seekGreen = it
                                    setKernel("${(it * 256).toInt()}", DISPLAY_GREEN)
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
                                text = "${(seekGreen * 256).toInt()}"
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 7.dp, bottom = 1.dp),
                                text = "B",
                                color = Color.Blue
                            )
                            Slider(
                                modifier = Modifier
                                    .padding(start = 30.dp, end = 40.dp),
                                value = seekBlue,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_BLUE else SpfConfig.DISPLAY_BLUE, it) }
                                    seekBlue = it
                                    setKernel("${(it * 256).toInt()}", DISPLAY_BLUE)
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
                                text = "${(seekBlue * 256).toInt()}"
                            )
                        }

                        //Saturated Main
                        Spacer(
                            modifier = Modifier
                                .height(16.dp)
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 5.dp),
                            text = stringResource(R.string.display_saturated)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Slider(
                                modifier = Modifier
                                    .padding(end = 40.dp),
                                value = seekSaturation,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_SATURATED else SpfConfig.DISPLAY_SATURATED, it) }
                                    seekSaturation = it
                                    setKernel("${(it * 383).toInt()}", DISPLAY_SATURATED)
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
                                text = "${(seekSaturation * 383).toInt()}"
                            )
                        }
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 5.dp),
                            text = stringResource(R.string.display_value)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Slider(
                                modifier = Modifier
                                    .padding(end = 40.dp),
                                value = seekValue,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_VALUE else SpfConfig.DISPLAY_VALUE, it) }
                                    seekValue = it
                                    setKernel("${(it * 383).toInt()}", DISPLAY_VALUE)
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
                                text = "${(seekValue * 383).toInt()}"
                            )
                        }
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 5.dp),
                            text = stringResource(R.string.display_contrast)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Slider(
                                modifier = Modifier
                                    .padding(end = 40.dp),
                                value = seekContrast,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_CONTRAST else SpfConfig.DISPLAY_CONTRAST, it) }
                                    seekContrast = it
                                    setKernel("${(it * 383).toInt()}", DISPLAY_CONTRAST)
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
                                text = "${(seekContrast * 383).toInt()}"
                            )
                        }
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 5.dp),
                            text = stringResource(R.string.display_hue)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 9.dp, end = 22.dp),
                        ) {
                            Slider(
                                modifier = Modifier
                                    .padding(end = 40.dp),
                                value = seekHue,
                                onValueChange = {
                                    prefs.edit { putFloat(if (displayAmoled) SpfConfig.DISPLAY_AMOLED_HUE else SpfConfig.DISPLAY_HUE, it) }
                                    seekHue = it
                                    setKernel("${(it * 1536).toInt()}", DISPLAY_HUE)
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
                                text = "${(seekHue * 1536).toInt()}"
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
    onResetClick: () -> Unit,
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
            title = { Text(stringResource(R.string.color_calibration)) },
            navigationIcon = {
                IconButton(
                    onClick = onBack
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            },
            actions = {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = run {
                        MaterialTheme.colorScheme.primary
                    }),
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable {
                            onResetClick()
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 6.dp, horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reset),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
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

@Preview
@Composable
private fun KernelPreview() {
    DisplayColorScreen(EmptyDestinationsNavigator)
}
