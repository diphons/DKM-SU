package com.diphons.dkmsu.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.coroutines.launch
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.ConfirmResult
import com.diphons.dkmsu.ui.component.rememberConfirmDialog
import com.diphons.dkmsu.ui.component.rememberLoadingDialog
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.*

/**
 * @author rifsxd
 * @date 2025/1/14.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(navigator: DestinationsNavigator) {
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
    ) { paddingValues ->
        val loadingDialog = rememberLoadingDialog()
        val restoreDialog = rememberConfirmDialog()
        val backupDialog = rememberConfirmDialog()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var showRebootDialog by remember { mutableStateOf(false) }

            if (showRebootDialog) {
                AlertDialog(
                    onDismissRequest = { showRebootDialog = false },
                    title = { Text(stringResource(R.string.reboot_required)) },
                    text = { Text(stringResource(R.string.reboot_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showRebootDialog = false
                            reboot()
                        }) {
                            Text(stringResource(R.string.reboot))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRebootDialog = false }) {
                            Text(stringResource(R.string.later))
                        }
                    }
                )
            }

            val moduleBackup = stringResource(id = R.string.module_backup)
            val backupMessage = stringResource(id = R.string.module_backup_message)
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.Backup,
                        moduleBackup
                    )
                },
                headlineContent = { Text(moduleBackup) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val result = backupDialog.awaitConfirm(title = moduleBackup, content = backupMessage)
                        if (result == ConfirmResult.Confirmed) {
                            loadingDialog.withLoading {
                                moduleBackup()
                            }
                        }
                    }
                }
            )

            if (showRebootDialog) {
                AlertDialog(
                    onDismissRequest = { showRebootDialog = false },
                    title = { Text(stringResource(R.string.reboot_required)) },
                    text = { Text(stringResource(R.string.reboot_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showRebootDialog = false
                            reboot()
                        }) {
                            Text(stringResource(R.string.reboot))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRebootDialog = false }) {
                            Text(stringResource(R.string.later))
                        }
                    }
                )
            }

            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            var useOverlayFs by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("use_overlay_fs", false)
                )
            }

            val moduleRestore = stringResource(id = R.string.module_restore)
            val restoreMessage = stringResource(id = R.string.module_restore_message)

            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.Restore,
                        moduleRestore,
                        tint = if (useOverlayFs) androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                },
                headlineContent = { 
                    Text(
                        moduleRestore,
                        color = if (useOverlayFs) androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    ) 
                },
                modifier = Modifier.clickable(
                    enabled = !useOverlayFs,
                    onClick = {
                        scope.launch {
                            val result = restoreDialog.awaitConfirm(title = moduleRestore, content = restoreMessage)
                            if (result == ConfirmResult.Confirmed) {
                                loadingDialog.withLoading {
                                    moduleRestore()
                                    showRebootDialog = true
                                }
                            }
                        }
                    }
                )
            )

            HorizontalDivider(thickness = Dp.Hairline)

            val allowlistBackup = stringResource(id = R.string.allowlist_backup)
            val allowlistbackupMessage = stringResource(id = R.string.allowlist_backup_message)
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.Backup,
                        allowlistBackup
                    )
                },
                headlineContent = { Text(allowlistBackup) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val result = backupDialog.awaitConfirm(title = allowlistBackup, content = allowlistbackupMessage)
                        if (result == ConfirmResult.Confirmed) {
                            loadingDialog.withLoading {
                                allowlistBackup()
                            }
                        }
                    }
                }
            )

            val allowlistRestore = stringResource(id = R.string.allowlist_restore)
            val allowlistrestoreMessage = stringResource(id = R.string.allowlist_restore_message)
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.Restore,
                        allowlistRestore
                    )
                },
                headlineContent = { Text(allowlistRestore) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val result = restoreDialog.awaitConfirm(title = allowlistRestore, content = allowlistrestoreMessage)
                        if (result == ConfirmResult.Confirmed) {
                            loadingDialog.withLoading {
                                allowlistRestore()
                            }
                        }
                    }
                }
            )
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
            title = { Text(stringResource(R.string.backup_restore)) },
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
private fun BackupPreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
