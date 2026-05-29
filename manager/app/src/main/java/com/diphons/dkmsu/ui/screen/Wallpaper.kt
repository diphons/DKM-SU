package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.prepareColor
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.edit
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.runDialog
import kotlinx.coroutines.delay

/**
 * @author diphons
 * @date 2024/01/02.
 */
@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun WallpaperScreen(navigator: DestinationsNavigator) {
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
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
    val colorScheme = MaterialTheme.colorScheme
    var backgroundColor by rememberSaveable { mutableIntStateOf(prefs.getInt(SpfColors.COLOR_WALLPAPER, colorScheme.surfaceVariant.toArgb())) }

    val runDialog = rememberCustomDialog {
        runDialog(it)
    }
    var countFinish by rememberSaveable { mutableStateOf(false) }
    if (countFinish) {
        LaunchedEffect(Unit) {
            repeat(10) {
                delay(1000)
                if (setwall) {
                    setwall = false
                    countFinish = false
                    runDialog.hide()
                }
            }
            runDialog.hide()
            countFinish = false
        }
    }
    prepareColor(context)
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
            val screenHeight = LocalResources.current.displayMetrics.heightPixels / LocalDensity.current.density
            val boxHeight = (screenHeight * 0.75f).dp // 75% tinggi layar

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeight)
                    .padding(top = 8.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(backgroundColor))
                    .padding(bottom = 8.dp)
            ) {
                val painter = painterResource(id = R.drawable.wall_multi)
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boxHeight),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                    alpha = DefaultAlpha,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val colorItems = listOf(
                                Color(0xFF2E2E2E),
                                colorScheme.surfaceVariant,
                                colorScheme.primary,
                                colorScheme.error,
                                colorScheme.tertiary,
                                Color(0xFFC9CACC)
                            )
                            colorItems.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp, 50.dp)
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .clickable {
                                            backgroundColor = color.toArgb()
                                        }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                DIALOG_MODE = 1
                                CMD_MSG = "Applying wallpaper"
                                runDialog.show()
                                prefs.edit { putInt(SpfColors.COLOR_WALLPAPER, backgroundColor) }
                                bitmapWallpaper = getBitmapFromDrawable(context, R.drawable.wall_multi, backgroundColor)
                                SaveWallpaper(bitmapWallpaper)
                                setWallpaper(context)
                                countFinish = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp, 8.dp)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            border = BorderStroke(2.dp, colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Set Wallpaper",
                                color = colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge
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
            title = { Text("Wallpaper") },
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

@RequiresApi(Build.VERSION_CODES.P)
@Preview
@Composable
private fun WallpaperPreview() {
    WallpaperScreen(EmptyDestinationsNavigator)
}
