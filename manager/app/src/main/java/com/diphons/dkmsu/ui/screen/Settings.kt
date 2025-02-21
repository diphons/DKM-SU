package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.IconSource
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.list.ListDialog
import com.maxkeppeler.sheets.list.models.ListOption
import com.maxkeppeler.sheets.list.models.ListSelection
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppProfileTemplateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FlashScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.diphons.dkmsu.BuildConfig
import com.diphons.dkmsu.Natives
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ksuApp
import com.diphons.dkmsu.ui.component.AboutDialog
import com.diphons.dkmsu.ui.component.ConfirmResult
import com.diphons.dkmsu.ui.component.DialogHandle
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.component.rememberConfirmDialog
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.rememberLoadingDialog
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.getBugreportFile
import com.diphons.dkmsu.ui.util.*
import com.diphons.dkmsu.ui.store.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author weishu
 * @date 2023/1/1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingScreen(navigator: DestinationsNavigator) {
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
        val aboutDialog = rememberCustomDialog {
            AboutDialog(it)
        }
        val loadingDialog = rememberLoadingDialog()
        val shrinkDialog = rememberConfirmDialog()
        val restoreDialog = rememberConfirmDialog()
        val backupDialog = rememberConfirmDialog()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

            val exportBugreportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/gzip")
            ) { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch(Dispatchers.IO) {
                    loadingDialog.show()
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        getBugreportFile(context).inputStream().use {
                            it.copyTo(output)
                        }
                    }
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(context.getString(R.string.log_saved))
                }
            }

            val profileTemplate = stringResource(id = R.string.settings_profile_template)
            ListItem(
                leadingContent = { Icon(Icons.Filled.Fence, profileTemplate) },
                headlineContent = { Text(profileTemplate) },
                supportingContent = { Text(stringResource(id = R.string.settings_profile_template_summary)) },
                modifier = Modifier.clickable {
                    navigator.navigate(AppProfileTemplateScreenDestination)
                }
            )

            var umountChecked by rememberSaveable {
                mutableStateOf(Natives.isDefaultUmountModules())
            }
            SwitchItem(
                icon = Icons.Filled.FolderDelete,
                title = stringResource(id = R.string.settings_umount_modules_default),
                summary = stringResource(id = R.string.settings_umount_modules_default_summary),
                checked = umountChecked
            ) {
                if (Natives.setDefaultUmountModules(it)) {
                    umountChecked = it
                }
            }

            if (Natives.version >= Natives.MINIMAL_SUPPORTED_SU_COMPAT) {
                var isSuDisabled by rememberSaveable {
                    mutableStateOf(!Natives.isSuEnabled())
                }
                SwitchItem(
                    icon = Icons.Filled.RemoveModerator,
                    title = stringResource(id = R.string.settings_disable_su),
                    summary = stringResource(id = R.string.settings_disable_su_summary),
                    checked = isSuDisabled,
                ) { checked ->
                    val shouldEnable = !checked
                    if (Natives.setSuEnabled(shouldEnable)) {
                        isSuDisabled = !shouldEnable
                    }
                }
            }

            var checkUpdate by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("check_update", true)
                )
            }
            SwitchItem(
                icon = Icons.Filled.Update,
                title = stringResource(id = R.string.settings_check_update),
                summary = stringResource(id = R.string.settings_check_update_summary),
                checked = checkUpdate
            ) {
                prefs.edit().putBoolean("check_update", it).apply()
                checkUpdate = it
            }

            var enableWebDebugging by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean("enable_web_debugging", false)
                )
            }
            SwitchItem(
                icon = Icons.Filled.DeveloperMode,
                title = stringResource(id = R.string.enable_web_debugging),
                summary = stringResource(id = R.string.enable_web_debugging_summary),
                checked = enableWebDebugging
            ) {
                prefs.edit().putBoolean("enable_web_debugging", it).apply()
                enableWebDebugging = it
            }

            val show_warn = rememberSaveable { mutableStateOf(prefs.getBoolean(SpfConfig.KSUD_MODE_WARN, false)) }
            var useOverlayFs by rememberSaveable {
                mutableStateOf(
                    prefs.getBoolean(SpfConfig.KSUD_MODE, false)
                )
            }

            val isManager = Natives.becomeManager(ksuApp.packageName)
            var showRebootDialog by remember { mutableStateOf(false) }
            var showWarningDialog by remember { mutableStateOf(false) }

            if (isManager) {
                SwitchItem(
                    icon = Icons.Filled.UnfoldMore,
                    title = stringResource(id = R.string.use_overlay_fs),
                    summary = stringResource(id = R.string.use_overlay_fs_summary),
                    checked = useOverlayFs
                ) {
                    if (!show_warn.value) {
                        showWarningDialog = true
                    }
                }

                if (showWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showWarningDialog = false },
                        title = { Text(stringResource(R.string.warning)) },
                        text = { Text(stringResource(R.string.warning_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showWarningDialog = false
                                prefs.edit().putBoolean(SpfConfig.KSUD_MODE, !useOverlayFs).apply()
                                useOverlayFs = !useOverlayFs
                                if (useOverlayFs) {
                                    moduleBackup()
                                } else {
                                    moduleMigration()
                                }
                                install()
                                showRebootDialog = true
                            }) {
                                Text(stringResource(R.string.proceed))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWarningDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

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

                val moduleRestore = stringResource(id = R.string.module_restore)
                val restoreMessage = stringResource(id = R.string.module_restore_message)
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Restore,
                            moduleRestore
                        )
                    },
                    headlineContent = { Text(moduleRestore) },
                    modifier = Modifier.clickable {
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
            }

            var showBottomsheet by remember { mutableStateOf(false) }

            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.BugReport,
                        stringResource(id = R.string.send_log)
                    )
                },
                headlineContent = { Text(stringResource(id = R.string.send_log)) },
                modifier = Modifier.clickable {
                    showBottomsheet = true
                }
            )
            if (showBottomsheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomsheet = false },
                    content = {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.CenterHorizontally)

                        ) {
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                                            val current = LocalDateTime.now().format(formatter)
                                            exportBugreportLauncher.launch("DKMSU_bugreport_${current}.tar.gz")
                                            showBottomsheet = false
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Save,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.save_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }

                                    )
                                }
                            }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            scope.launch {
                                                val bugreport = loadingDialog.withLoading {
                                                    withContext(Dispatchers.IO) {
                                                        getBugreportFile(context)
                                                    }
                                                }

                                                val uri: Uri =
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                                        bugreport
                                                    )

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    setDataAndType(uri, "application/gzip")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        context.getString(R.string.send_log)
                                                    )
                                                )
                                            }
                                        }
                                ) {
                                    Icon(
                                        Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.send_log),
                                        modifier = Modifier.padding(top = 16.dp),
                                        textAlign = TextAlign.Center.also {
                                            LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (useOverlayFs) {
                val shrink = stringResource(id = R.string.shrink_sparse_image)
                val shrinkMessage = stringResource(id = R.string.shrink_sparse_image_message)
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Compress,
                            shrink
                        )
                    },
                    headlineContent = { Text(shrink) },
                    modifier = Modifier.clickable {
                        scope.launch {
                            val result = shrinkDialog.awaitConfirm(title = shrink, content = shrinkMessage)
                            if (result == ConfirmResult.Confirmed) {
                                loadingDialog.withLoading {
                                    shrinkModules()
                                }
                            }
                        }
                    }
                )
            }

            val lkmMode = Natives.version >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && Natives.isLkmMode
            if (lkmMode) {
                UninstallItem(navigator) {
                    loadingDialog.withLoading(it)
                }
            }

            val about = stringResource(id = R.string.about)
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Filled.ContactPage,
                        about
                    )
                },
                headlineContent = { Text(about) },
                modifier = Modifier.clickable {
                    aboutDialog.show()
                }
            )
        }
    }
}

@Composable
fun UninstallItem(
    navigator: DestinationsNavigator,
    withLoading: suspend (suspend () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uninstallConfirmDialog = rememberConfirmDialog()
    val showTodo = {
        Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show()
    }
    val uninstallDialog = rememberUninstallDialog { uninstallType ->
        scope.launch {
            val result = uninstallConfirmDialog.awaitConfirm(
                title = context.getString(uninstallType.title),
                content = context.getString(uninstallType.message)
            )
            if (result == ConfirmResult.Confirmed) {
                withLoading {
                    when (uninstallType) {
                        UninstallType.TEMPORARY -> showTodo()
                        UninstallType.PERMANENT -> navigator.navigate(
                            FlashScreenDestination(FlashIt.FlashUninstall)
                        )
                        UninstallType.RESTORE_STOCK_IMAGE -> navigator.navigate(
                            FlashScreenDestination(FlashIt.FlashRestore)
                        )
                        UninstallType.NONE -> Unit
                    }
                }
            }
        }
    }
    val uninstall = stringResource(id = R.string.settings_uninstall)
    ListItem(
        leadingContent = {
            Icon(
                Icons.Filled.Delete,
                uninstall
            )
        },
        headlineContent = { Text(uninstall) },
        modifier = Modifier.clickable {
            uninstallDialog.show()
        }
    )
}

enum class UninstallType(val title: Int, val message: Int, val icon: ImageVector) {
    TEMPORARY(
        R.string.settings_uninstall_temporary,
        R.string.settings_uninstall_temporary_message,
        Icons.Filled.Delete
    ),
    PERMANENT(
        R.string.settings_uninstall_permanent,
        R.string.settings_uninstall_permanent_message,
        Icons.Filled.DeleteForever
    ),
    RESTORE_STOCK_IMAGE(
        R.string.settings_restore_stock_image,
        R.string.settings_restore_stock_image_message,
        Icons.AutoMirrored.Filled.Undo
    ),
    NONE(0, 0, Icons.Filled.Delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberUninstallDialog(onSelected: (UninstallType) -> Unit): DialogHandle {
    return rememberCustomDialog { dismiss ->
        val options = listOf(
            // UninstallType.TEMPORARY,
            UninstallType.PERMANENT,
            UninstallType.RESTORE_STOCK_IMAGE
        )
        val listOptions = options.map {
            ListOption(
                titleText = stringResource(it.title),
                subtitleText = if (it.message != 0) stringResource(it.message) else null,
                icon = IconSource(it.icon)
            )
        }

        var selection = UninstallType.NONE
        ListDialog(state = rememberUseCaseState(visible = true, onFinishedRequest = {
            if (selection != UninstallType.NONE) {
                onSelected(selection)
            }
        }, onCloseRequest = {
            dismiss()
        }), header = Header.Default(
            title = stringResource(R.string.settings_uninstall),
        ), selection = ListSelection.Single(
            showRadioButtons = false,
            options = listOptions,
        ) { index, _ ->
            selection = options[index]
        })
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
            title = { Text(stringResource(R.string.settings)) },
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
private fun SettingsPreview() {
    SettingScreen(EmptyDestinationsNavigator)
}
