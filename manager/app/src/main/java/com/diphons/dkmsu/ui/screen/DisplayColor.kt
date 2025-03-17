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
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.setKernel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.DisplayColorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
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
    val display_rgb = 1f
    val display_sat = 0.6662165f
    val display_hue = 0f

    //Backup value
    var db_red by rememberSaveable {
        mutableStateOf(display_rgb)
    }
    var db_green by rememberSaveable {
        mutableStateOf(display_rgb)
    }
    var db_blue by rememberSaveable {
        mutableStateOf(display_rgb)
    }
    var db_sat by rememberSaveable {
        mutableStateOf(display_sat)
    }
    var db_val by rememberSaveable {
        mutableStateOf(display_sat)
    }
    var db_cont by rememberSaveable {
        mutableStateOf(display_sat)
    }
    var db_hue by rememberSaveable {
        mutableStateOf(display_hue)
    }
    var seek_red by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_RED, display_rgb))
    }
    var seek_green by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_GREEN, display_rgb))
    }
    var seek_blue by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_BLUE, display_rgb))
    }
    var seek_saturation by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_SATURATED, display_sat))
    }
    var seek_value by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_VALUE, display_sat))
    }
    var seek_contrast by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_CONTRAST, display_sat))
    }
    var seek_hue by rememberSaveable {
        mutableStateOf(prefs.getFloat(SpfConfig.DISPLAY_HUE, display_hue))
    }
    var display_amoled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SpfConfig.DISPLAY_AMOLED, false))
    }

    fun amoledColor(enabled: Boolean){
        if (enabled) {
            db_red = seek_red
            db_green = seek_green
            db_blue = seek_blue
            db_sat = seek_saturation
            db_val = seek_value
            db_cont = seek_contrast
            db_hue = seek_hue

            seek_red = 0.9040460f
            seek_green = 0.9040460f
            seek_blue = 0.9040460f
            seek_saturation = 0.71083844f
            seek_value = display_sat
            seek_contrast = 0.6870402f
            seek_hue = display_hue
        } else {
            seek_red = db_red
            seek_green = db_green
            seek_blue = db_blue
            seek_saturation = db_sat
            seek_value = db_val
            seek_contrast = db_cont
            seek_hue = db_hue
        }
        prefs.edit().putFloat(SpfConfig.DISPLAY_RED, display_rgb).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_GREEN, display_rgb).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_BLUE, display_rgb).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_SATURATED, display_sat).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_VALUE, display_sat).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_CONTRAST, display_sat).apply()
        prefs.edit().putFloat(SpfConfig.DISPLAY_HUE, display_hue).apply()

        setKernel("${(seek_red * 256).toInt()}", DISPLAY_RED)
        setKernel("${(seek_green * 256).toInt()}", DISPLAY_GREEN)
        setKernel("${(seek_blue * 256).toInt()}", DISPLAY_BLUE)
        setKernel("${(seek_saturation * 383).toInt()}", DISPLAY_SATURATED)
        setKernel("${(seek_value * 383).toInt()}", DISPLAY_VALUE)
        setKernel("${(seek_contrast * 383).toInt()}", DISPLAY_CONTRAST)
        setKernel("${(seek_hue * 1536).toInt()}", DISPLAY_HUE)
    }
    // Apply Amoled on load
    amoledColor(display_amoled)
    
    Scaffold(
        topBar = {
            TopBar(
                onBack = {
                    navigator.popBackStack()
                },
                onResetClick = {
                    seek_red = display_rgb
                    seek_green = display_rgb
                    seek_blue = display_rgb
                    seek_saturation = display_sat
                    seek_value = display_sat
                    seek_contrast = display_sat
                    seek_hue = display_hue
                    db_red = seek_red
                    db_green = seek_green
                    db_blue = seek_blue
                    db_sat = seek_saturation
                    db_val = seek_value
                    db_cont = seek_contrast
                    db_hue = seek_hue
                    display_amoled = false

                    prefs.edit().putFloat(SpfConfig.DISPLAY_RED, display_rgb).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_GREEN, display_rgb).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_BLUE, display_rgb).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_SATURATED, display_sat).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_VALUE, display_sat).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_CONTRAST, display_sat).apply()
                    prefs.edit().putFloat(SpfConfig.DISPLAY_HUE, display_hue).apply()
                    prefs.edit().putBoolean(SpfConfig.DISPLAY_AMOLED, display_amoled).apply()

                    setKernel("${(seek_red * 256).toInt()}", DISPLAY_RED)
                    setKernel("${(seek_green * 256).toInt()}", DISPLAY_GREEN)
                    setKernel("${(seek_blue * 256).toInt()}", DISPLAY_BLUE)
                    setKernel("${(seek_saturation * 383).toInt()}", DISPLAY_SATURATED)
                    setKernel("${(seek_value * 383).toInt()}", DISPLAY_VALUE)
                    setKernel("${(seek_contrast * 383).toInt()}", DISPLAY_CONTRAST)
                    setKernel("${(seek_hue * 1536).toInt()}", DISPLAY_HUE)
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
                            checked = display_amoled,
                            summary = stringResource(R.string.amoled_color_sum)
                        ) {
                            amoledColor(it)
                            prefs.edit().putBoolean(SpfConfig.DISPLAY_AMOLED, it).apply()
                            display_amoled = it
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
                                value = seek_red,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_RED, it).apply()
                                    seek_red = it
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
                                text = "${(seek_red * 256).toInt()}"
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
                                value = seek_green,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_GREEN, it).apply()
                                    seek_green = it
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
                                text = "${(seek_green * 256).toInt()}"
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
                                value = seek_blue,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_BLUE, it).apply()
                                    seek_blue = it
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
                                text = "${(seek_blue * 256).toInt()}"
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
                                value = seek_saturation,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_SATURATED, it).apply()
                                    seek_saturation = it
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
                                text = "${(seek_saturation * 383).toInt()}"
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
                                value = seek_value,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_VALUE, it).apply()
                                    seek_value = it
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
                                text = "${(seek_value * 383).toInt()}"
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
                                value = seek_contrast,
                                valueRange = 0.3364623f..1f,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_CONTRAST, it).apply()
                                    seek_contrast = it
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
                                text = "${(seek_contrast * 383).toInt()}"
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
                                value = seek_hue,
                                onValueChange = {
                                    prefs.edit().putFloat(SpfConfig.DISPLAY_HUE, it).apply()
                                    seek_hue = it
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
                                text = "${(seek_hue * 1536).toInt()}"
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
