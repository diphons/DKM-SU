package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.system.Os
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.diphons.dkmsu.*
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.rememberConfirmDialog
import com.diphons.dkmsu.ui.util.*
import com.diphons.dkmsu.ui.util.module.LatestVersionInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.runDialog
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils.CMD_MSG
import com.diphons.dkmsu.ui.util.Utils.DIALOG_MODE
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val kernelVersion = getKernelVersion()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

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
    Scaffold(
        topBar = {
            TopBar(
                kernelVersion,
                onSettingsClick = {
                    navigator.navigate(SettingScreenDestination)
                },
                onInstallClick = {
                    navigator.navigate(InstallScreenDestination)
                },
                scrollBehavior = scrollBehavior,
                borderVisible = if (scrollPos > 0.1f) true else false
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val runDialog = rememberCustomDialog {
            runDialog(it)
        }
        var runGrant by rememberSaveable { mutableStateOf(false) }
        var count_finish by rememberSaveable { mutableStateOf(false) }
        if (!prefs.getBoolean(SpfConfig.PERMISSIONS, false)) {
            DIALOG_MODE = 3
            if (!runGrant) {
                runDialog.show()
                runGrant = true
                setPermissions(context)
                count_finish = true
            }
        }
        if (count_finish) {
            LaunchedEffect(Unit) {
                for (i in 1..200) {
                    delay(1000) // update once a second
                    if (CMD_MSG.contains("Finish")) {
                        runDialog.hide()
                        CMD_MSG = ""
                        count_finish = false
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isManager = Natives.becomeManager(ksuApp.packageName)
            val ksuVersion = if (isManager) Natives.version else null
            val lkmMode = ksuVersion?.let {
                if (it >= Natives.MINIMAL_SUPPORTED_KERNEL_LKM && kernelVersion.isGKI()) Natives.isLkmMode else null
            }

            StatusCard(kernelVersion, ksuVersion, lkmMode)
            if (isManager && Natives.requireNewKernel()) {
                WarningCard(
                    stringResource(id = R.string.require_kernel_version).format(
                        ksuVersion, Natives.MINIMAL_SUPPORTED_KERNEL
                    )
                )
            }
            if (ksuVersion != null && !rootAvailable()) {
                WarningCard(
                    stringResource(id = R.string.grant_root_failed)
                )
            }
            val checkUpdate =
                LocalContext.current.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
                    .getBoolean("check_update", true)
            if (checkUpdate) {
                UpdateCard()
            }
            //NextCard()
            InfoCard()
            IssueReportCard()
            //EXperimentalCard()
            Spacer(Modifier)
        }
    }
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }

    val currentVersionCode = getManagerVersion(context).second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.new_version_available).format(newVersionCode),
            MaterialTheme.colorScheme.outlineVariant
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title,
                    content = changelog,
                    markdown = true,
                    confirm = updateText
                )
            }
        }
    }
}

@Composable
fun RebootDropdownItem(@StringRes id: Int, reason: String = "") {
    DropdownMenuItem(text = {
        Text(stringResource(id))
    }, onClick = {
        reboot(reason)
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    kernelVersion: KernelVersion,
    onInstallClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_dkm_su),
                        contentDescription = null
                    )
                    Column(Modifier.padding(start = 5.dp)) {
                        Text(
                            stringResource(R.string.app_name_long)
                        )
                    }
                }
            },
            actions = {
                /*
             * Hide GKI Feature on Spoofed Kernel when SuSFS available
             */
                val realGKI: Boolean = if (getSuSFS() == "Supported")
                    if (getSuSFSVariant() == "GKI")
                        true
                    else
                        false
                else
                    true
                if (kernelVersion.isGKI() && realGKI) {
                    IconButton(onClick = onInstallClick) {
                        Icon(
                            imageVector = Icons.Filled.InstallMobile,
                            contentDescription = stringResource(id = R.string.install)
                        )
                    }
                }

                val isManager = Natives.becomeManager(ksuApp.packageName)
                var showDropdown by remember { mutableStateOf(false) }
                if (isManager) {
                    IconButton(onClick = {
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
private fun StatusCard(
    kernelVersion: KernelVersion,
    ksuVersion: Int?,
    lkmMode: Boolean?
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = run {
            if (ksuVersion != null) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        })
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                ksuVersion != null -> {
                    val safeMode = when {
                        Natives.isSafeMode -> " [${stringResource(id = R.string.safe_mode)}]"
                        else -> ""
                    }

                    val workingMode = when (lkmMode) {
                        null -> " <LTS>"
                        true -> " <LKM>"
                        else -> " <GKI>"
                    }

                    val workingText =
                        "${stringResource(id = R.string.home_working)}$workingMode$safeMode"
                    val painter = painterResource(id = R.drawable.wall_multi)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .height(130.dp)
                            .width(59.dp)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {

                            },
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        alpha = DefaultAlpha,
                    )
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = workingText,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(1.dp))
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                        ){
                            val isSUS_SU = getSuSFSFeatures() == "CONFIG_KSU_SUSFS_SUS_SU"
                            val susSUMode = if (isSUS_SU) "| SuS SU mode: ${susfsSUS_SU_Mode()}" else ""
                            val suSFS = getSuSFS()
                            val dkmsvc = getSVCVersion()
                            Column {
                                Text(
                                    text = stringResource(R.string.home_working_version),
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = stringResource(R.string.home_superuser_count), fontSize = 12.sp, style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = stringResource(R.string.home_module_count),
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = stringResource(R.string.home_susfs),
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (dkmsvc.isNotEmpty()) {
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = "DkmSvc",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = ": $ksuVersion",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = ": ${getSuperuserCount()}",
                                    fontSize = 13.sp, style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = ": ${getModuleCount()}",
                                    fontSize = 13.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(1.dp))
                                if (suSFS == "Supported") {
                                    Text(
                                        text = ": ${getSuSFSVersion()} (${if (workingMode.contains("GKI")) "GKI" else getSuSFSVariant()}) $susSUMode",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = suSFS,
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (dkmsvc.isNotEmpty()) {
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = ": $dkmsvc",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                kernelVersion.isGKI() -> {
                    Icon(Icons.Filled.AutoFixHigh, stringResource(R.string.home_not_installed))
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_not_installed),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_click_to_install),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Icon(Icons.Filled.Dangerous, stringResource(R.string.home_failure))
                    Column(Modifier.padding(start = 20.dp)) {
                        Text(
                            text = stringResource(R.string.home_failure),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_failure_tip),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)
        ) {
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (Build.BRAND != Build.MANUFACTURER) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}

@Composable
private fun InfoCard() {
    val context = LocalContext.current

    val prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    val useOverlayFs = MutableLiveData<Boolean>(prefs.getBoolean(SpfConfig.KSUD_MODE, false))

    val isManager = Natives.becomeManager(ksuApp.packageName)

    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String, icon: Any? = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        when (icon) {
                            is ImageVector -> Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                            is Painter -> Icon(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = content,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
            }

            InfoCardItem(
                label = stringResource(R.string.home_device),
                content = getDeviceInfo()
            )

            Spacer(Modifier.height(4.dp))
            InfoCardItem(
                label = stringResource(R.string.home_kernel),
                content = uname.release
            )

            Spacer(Modifier.height(4.dp))
            InfoCardItem(
                label = stringResource(R.string.home_android),
                content = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"

            )

            Spacer(Modifier.height(4.dp))
            val managerVersion = getManagerVersion(context)
            InfoCardItem(
                label = stringResource(R.string.home_manager_version),
                content = "${managerVersion.first} (${managerVersion.second})"
            )

            Spacer(Modifier.height(4.dp))
            InfoCardItem(
                label = stringResource(R.string.home_selinux_status),
                content = getSELinuxStatus()
            )
            if (isManager) {
                Spacer(Modifier.height(4.dp))
                InfoCardItem(
                    label = stringResource(R.string.home_module_mount),
                    content = if (useOverlayFs.value!!) {
                        stringResource(R.string.home_overlayfs_mount)
                    } else {
                        stringResource(R.string.home_magic_mount)
                    },
                )
            }
        }
    }
}

@Composable
fun IssueReportCard() {
    val uriHandler = LocalUriHandler.current
    val githubIssueUrl = stringResource(R.string.issue_report_github_link)
    val telegramUrl = stringResource(R.string.issue_report_telegram_link)

    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.issue_report_title),
                    fontSize = 15.sp,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body),
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.issue_report_body_2),
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { uriHandler.openUri(githubIssueUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = stringResource(R.string.issue_report_github),
                    )
                }
                IconButton(onClick = { uriHandler.openUri(telegramUrl) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_telegram),
                        contentDescription = stringResource(R.string.issue_report_telegram),
                    )
                }
            }
        }
    }
}

fun getManagerVersion(context: Context): Pair<String, Long> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return Pair(packageInfo.versionName!!, versionCode)
}

@Preview
@Composable
private fun StatusCardPreview() {
    Column {
        StatusCard(KernelVersion(5, 10, 101), 1, null)
        StatusCard(KernelVersion(5, 10, 101), 20000, true)
        StatusCard(KernelVersion(5, 10, 101), null, true)
        StatusCard(KernelVersion(4, 10, 101), null, false)
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column {
        WarningCard(message = "Warning message")
        WarningCard(
            message = "Warning message ",
            MaterialTheme.colorScheme.outlineVariant,
            onClick = {})
    }
}
