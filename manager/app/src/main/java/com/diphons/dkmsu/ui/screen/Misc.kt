package com.diphons.dkmsu.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.runDialog
import com.diphons.dkmsu.ui.uibench.RenderingJitter
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.util.RootUtils
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.Utils
import com.diphons.dkmsu.ui.util.Utils.*
import com.diphons.dkmsu.ui.util.getKnVersion
import com.diphons.dkmsu.ui.util.getSpoofKnVersion
import com.diphons.dkmsu.ui.util.getSuSFS
import com.diphons.dkmsu.ui.util.getStartedSVC
import com.diphons.dkmsu.ui.util.hasJoyose
import com.diphons.dkmsu.ui.util.hasModule
import com.diphons.dkmsu.ui.util.install_dkmsvc
import com.diphons.dkmsu.ui.util.isInternetAvailable
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.diphons.dkmsu.ui.util.setKernel
import com.diphons.dkmsu.ui.util.startPIFWorker
import com.diphons.dkmsu.ui.util.startSVC
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.coroutines.delay

/**
 * @author diphons
 * @date 2024/12/30.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun MiscScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val context = LocalContext.current
    val globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)
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
            ElevatedCard {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(context, RenderingJitter::class.java))
                    }
                    .padding(23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (modifier = Modifier
                        .weight(3f)
                    ){
                        Text(
                            text = stringResource(R.string.jitter_rendering),
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.jitter_rendering_sum),
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
            var dkm_service by rememberSaveable { mutableStateOf(
                if (getStartedSVC())
                    "Started"
                else
                    "Not Started"
            ) }
            var count_finish by rememberSaveable { mutableStateOf(false) }
            if (count_finish) {
                LaunchedEffect(Unit) {
                    for (i in 1..200) {
                        delay(1000) // update once a second
                        if (getStartedSVC()) {
                            count_finish = false
                            dkm_service = "Started"
                            CMD_MSG = "DKM Service Started"
                            runDialog.hide()
                        }
                    }
                }
            }
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(23.dp)
                ){
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterStart)
                    ){
                        Text(
                            text = "DKM Service",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            modifier = Modifier
                                .padding(top = 5.dp),
                            text = "${stringResource(R.string.status)} : $dkm_service",
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = run {
                            MaterialTheme.colorScheme.primary
                        }),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable {
                                DIALOG_MODE = 1
                                if (dkm_service.contains("Not"))
                                    CMD_MSG = "Starting DKM Service"
                                else
                                    CMD_MSG = "Restarting DKM Service"
                                runDialog.show()
                                RootUtils.runCommand("setprop init.dkmsvc.exit 1")
                                dkm_service = "Restarting"
                                install_dkmsvc()
                                RootUtils.runCommand("setprop init.dkmsvc.exit 0; setprop init.susfs \"0\"")
                                count_finish = true
                                startSVC()
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (dkm_service.contains("Not")) "Start" else "Restart",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            if (hasModule(PIF_UPDATER)) {
                var pif_finish by rememberSaveable { mutableStateOf(false) }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(23.dp)
                    ){
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterStart),
                            text = "PIF Updater",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = run {
                                MaterialTheme.colorScheme.primary
                            }),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable {
                                    CMD_MSG = ""
                                    DIALOG_MODE = 0
                                    pif_finish = true
                                    runDialog.show()
                                    if (isInternetAvailable(context))
                                        startPIFWorker(context)
                                    else
                                        AV_INTERNET = false
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp, horizontal = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.start),
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            if (hasModule(BLOCK_JOYOSE) && hasJoyose()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    var block_joyose by rememberSaveable {
                        mutableStateOf(globalConfig.getBoolean(SpfConfig.BLOCK_JOYOSE, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(id = R.string.joyose),
                            checked = block_joyose,
                            summary = stringResource(id = R.string.joyose_sum),
                            fontSize = 15.sp,
                            fontSizeSum = 13.sp
                        ) {
                            if (it)
                                setKernel("1", BLOCK_JOYOSE)
                            else
                                setKernel("0", BLOCK_JOYOSE)
                            globalConfig.edit { putBoolean(SpfConfig.BLOCK_JOYOSE, it) }
                            block_joyose = it
                        }
                    }
                }
            }

            val spoofDefault by rememberSaveable {
                mutableStateOf(globalConfig.getString(SpfConfig.SPOOF_DEFAULT, spoof_default()))
            }
            if (!spoofDefault!!.contains("encrypted")) {
                if (globalConfig.getString(SpfConfig.SPOOF_DEFAULT, "")!!.isEmpty())
                    globalConfig.edit { putString(SpfConfig.SPOOF_DEFAULT, spoofDefault) }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    var spoofEncrypt by rememberSaveable {
                        mutableStateOf(globalConfig.getBoolean(SpfConfig.SPOOF_ENCRYPT, false))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                    ) {
                        SwitchItem(
                            title = stringResource(id = R.string.spoof_encrypt),
                            checked = spoofEncrypt,
                            summary = stringResource(id = R.string.spoof_encrypt_sum),
                            fontSize = 15.sp,
                            fontSizeSum = 13.sp
                        ) {
                            if (it)
                                RootUtils.runCommand("resetprop ro.crypto.state encrypted")
                            else {
                                if (spoofDefault!!.contains("null"))
                                    RootUtils.runCommand("resetprop ro.crypto.state ''")
                                else
                                    RootUtils.runCommand("resetprop ro.crypto.state $spoofDefault")
                            }
                            globalConfig.edit { putBoolean(SpfConfig.SPOOF_ENCRYPT, it) }
                            spoofEncrypt = it
                        }
                    }
                }
            }
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                var selinuxSwitch by rememberSaveable {
                    mutableStateOf(globalConfig.getBoolean(SpfConfig.SELINUX_MODE, !isSELinuxActive()))
                }
                var selinuxOnBoot by rememberSaveable {
                    mutableStateOf(globalConfig.getBoolean(SpfConfig.SELINUX_ONBOOT, false))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, top = 10.dp)
                ) {
                    SwitchItem(
                        title = stringResource(id = R.string.selinux_permissive),
                        checked = selinuxSwitch,
                        summary = stringResource(id = R.string.selinux_permissive_sum),
                        fontSize = 15.sp,
                        fontSizeSum = 13.sp
                    ) {
                        globalConfig.edit { putBoolean(SpfConfig.SELINUX_MODE, it) }
                        selinuxSwitch = it
                        activateSELinux(!it, context)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, bottom = 10.dp)
                ) {
                    SwitchItem(
                        title = stringResource(id = R.string.apply_onboot),
                        checked = selinuxOnBoot,
                        summary = stringResource(id = R.string.selinux_onboot),
                        fontSize = 15.sp,
                        fontSizeSum = 13.sp
                    ) {
                        globalConfig.edit { putBoolean(SpfConfig.SELINUX_ONBOOT, it) }
                        selinuxOnBoot = it
                    }
                }
            }
            if (getSuSFS() == "Supported") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    var kernel_spoof by rememberSaveable { mutableStateOf("${globalConfig.getString(SpfConfig.SPOOF_KERNEL, "")}") }
                    var uname by rememberSaveable { mutableStateOf(getSpoofKnVersion()) }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(23.dp),
                    ){
                        Text(
                            modifier = Modifier,
                            text = "Kernel Version",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            modifier = Modifier,
                            text = uname,
                            fontSize = 13.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            modifier = Modifier,
                            text = "Spoof Kernel Version",
                            fontSize = 15.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = kernel_spoof,
                            onValueChange = {
                                kernel_spoof = it
                            },
                            maxLines = 1,
                            placeholder = {
                                Text(
                                    text = "default"
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (kernel_spoof.contains(" ")) {
                                        CMD_MSG = context.getString(R.string.susfs_not_alowed)
                                        runDialog.show()
                                    } else {
                                        globalConfig.edit { putString(SpfConfig.SPOOF_KERNEL, kernel_spoof) }
                                        runSVCWorker(context, "susfs1")
                                        uname = "$kernel_spoof${getKnVersion()}"
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ){
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = run {
                                    MaterialTheme.colorScheme.primary
                                }),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .clickable {
                                        kernel_spoof = ""
                                        globalConfig.edit { putString(SpfConfig.SPOOF_KERNEL, kernel_spoof) }
                                        runSVCWorker(context, "susfs1")
                                        uname = getSpoofKnVersion()
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.reset),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = run {
                                    MaterialTheme.colorScheme.primary
                                }),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .clickable {
                                        if (kernel_spoof.contains(" ")) {
                                            CMD_MSG = context.getString(R.string.susfs_not_alowed)
                                            runDialog.show()
                                        } else {
                                            globalConfig.edit { putString(SpfConfig.SPOOF_KERNEL, kernel_spoof) }
                                            runSVCWorker(context, "susfs1")
                                            uname = "$kernel_spoof${getKnVersion()}"
                                        }
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.apply),
                                        style = MaterialTheme.typography.titleSmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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
            title = { Text(stringResource(R.string.misc)) },
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
    MiscScreen(EmptyDestinationsNavigator)
}
