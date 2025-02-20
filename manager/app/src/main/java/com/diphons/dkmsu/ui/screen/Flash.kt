package com.diphons.dkmsu.ui.screen

import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.KeyEventBlocker
import com.diphons.dkmsu.ui.util.FlashResult
import com.diphons.dkmsu.ui.util.LkmSelection
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.flashModules
import com.diphons.dkmsu.ui.util.installBoot
import com.diphons.dkmsu.ui.util.reboot
import com.diphons.dkmsu.ui.util.restoreBoot
import com.diphons.dkmsu.ui.util.uninstallPermanently
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlashingStatus {
    FLASHING,
    SUCCESS,
    FAILED
}

/**
 * @author weishu
 * @date 2023/1/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>
fun FlashScreen(navigator: DestinationsNavigator, flashIt: FlashIt) {

    var text by rememberSaveable { mutableStateOf("") }
    var tempText: String
    val logContent = rememberSaveable { StringBuilder() }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }

    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var flashing by rememberSaveable {
        mutableStateOf(FlashingStatus.FLASHING)
    }

    BackHandler(enabled = flashing == FlashingStatus.FLASHING) {
        // Disable back button if flashing is running
    }

    LaunchedEffect(Unit) {
        if (text.isNotEmpty()) {
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            flashIt(flashIt, onStdout = {
                tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    text = tempText.substring(6)
                } else {
                    text += tempText
                }
                logContent.append(it).append("\n")
            }, onStderr = {
                logContent.append(it).append("\n")
            }).apply {
                if (code != 0) {
                    text += "Error code: $code.\n $err Please save and check the log.\n"
                }
                if (showReboot) {
                    text += "\n\n\n"
                    showFloatAction = true
                }
                flashing = if (code == 0) FlashingStatus.SUCCESS else FlashingStatus.FAILED
            }
        }
    }

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
                flashing,
                onBack = {
                    navigator.popBackStack()
                },
                onSave = {
                    scope.launch {
                        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        val date = format.format(Date())
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "KernelSU_install_log_${date}.log"
                        )
                        file.writeText(logContent.toString())
                        snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
                    }
                },
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false
            )
        },
        floatingActionButton = {
            if (showFloatAction) {
                // Reboot button (bottom left)
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                reboot()
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.reboot)) },
                    text = { Text(text = stringResource(R.string.reboot)) }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = text,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

@Parcelize
sealed class FlashIt : Parcelable {
    data class FlashBoot(val boot: Uri? = null, val lkm: LkmSelection, val ota: Boolean) :
        FlashIt()

    data class FlashModules(val uri: List<Uri>) : FlashIt()

    data object FlashRestore : FlashIt()

    data object FlashUninstall : FlashIt()
}

fun flashIt(
    flashIt: FlashIt,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return when (flashIt) {
        is FlashIt.FlashBoot -> installBoot(
            flashIt.boot,
            flashIt.lkm,
            flashIt.ota,
            onStdout,
            onStderr
        )

        is FlashIt.FlashModules -> flashModules(flashIt.uri, onStdout, onStderr)

        FlashIt.FlashRestore -> restoreBoot(onStdout, onStderr)

        FlashIt.FlashUninstall -> uninstallPermanently(onStdout, onStderr)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    status: FlashingStatus,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
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
            title = {
                Text(
                    stringResource(
                        when (status) {
                            FlashingStatus.FLASHING -> R.string.flashing
                            FlashingStatus.SUCCESS -> R.string.flash_success
                            FlashingStatus.FAILED -> R.string.flash_failed
                        }
                    )
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = { if (status != FlashingStatus.FLASHING) onBack() },
                    enabled = status != FlashingStatus.FLASHING
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            },
            actions = {
                IconButton(
                    onClick = { if (status != FlashingStatus.FLASHING) onSave() },
                    enabled = status != FlashingStatus.FLASHING
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = "Localized description"
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

@Preview
@Composable
fun InstallPreview() {
    InstallScreen(EmptyDestinationsNavigator)
}