package com.diphons.dkmsu.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.component.SwitchItem
import com.diphons.dkmsu.ui.component.rememberCustomDialog
import com.diphons.dkmsu.ui.component.runDialog
import com.diphons.dkmsu.ui.util.LocalSnackbarHost
import com.diphons.dkmsu.ui.store.*
import com.diphons.dkmsu.ui.util.applySUSList
import com.diphons.dkmsu.ui.util.getSUSList
import com.diphons.dkmsu.ui.util.getSUSListReset
import com.diphons.dkmsu.ui.util.runSVCWorker
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator

/**
 * @author diphons
 * @date 2024/12/30.
 */
@SuppressLint("SdCardPath")
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuSFSScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current

    val context = LocalContext.current
    val globalConfig = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE)

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard (
                modifier = Modifier
                    .fillMaxWidth()
            ){
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp)
                ){
                    var susfsLog by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_LOG, true)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_log),
                        checked = susfsLog,
                        fontSize = 15.sp
                    ) {
                        susfsLog = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_LOG, it).apply()
                        runSVCWorker(context, "susfs2")
                    }
                    var hidecustomrom by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_CUSROM, false)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_cusrom),
                        checked = hidecustomrom,
                        fontSize = 15.sp
                    ) {
                        hidecustomrom = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_CUSROM, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs3")
                    }
                    var hidegapps by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_GAPPS, false)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_gapps),
                        checked = hidegapps,
                        fontSize = 15.sp
                    ) {
                        hidegapps = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_GAPPS, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs4")
                    }
                    var hiderevanced by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_REVANCED, true)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_revanced),
                        checked = hiderevanced,
                        fontSize = 15.sp
                    ) {
                        hiderevanced = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_REVANCED, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs5")
                    }

                    var hidecmdline by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_CMDLINE, false)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_cmdline),
                        checked = hidecmdline,
                        fontSize = 15.sp
                    ) {
                        hidecmdline = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_CMDLINE, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs6")
                    }

                    var hideloop by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_LOOP, true)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_loop),
                        checked = hideloop,
                        fontSize = 15.sp
                    ) {
                        hideloop = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_LOOP, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs7")
                    }

                    var hidelsposed by rememberSaveable { mutableStateOf(globalConfig.getBoolean(SpfConfig.SUSFS_HIDE_LSPOSED, false)) }
                    SwitchItem(
                        title = stringResource(id = R.string.susfs_hide_lsposed),
                        checked = hidelsposed,
                        fontSize = 15.sp
                    ) {
                        hidelsposed = it
                        globalConfig.edit().putBoolean(SpfConfig.SUSFS_HIDE_LSPOSED, it).apply()
                        if (it)
                            runSVCWorker(context, "susfs8")
                    }
                }
            }
            ElevatedCard (
                modifier = Modifier
                    .fillMaxWidth()
            ){
                var sus_mount by rememberSaveable { mutableStateOf(getSUSList(context, "sus_mount")) }
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(23.dp),
                ){
                    Text(
                        modifier = Modifier,
                        text = "SUS Mount",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        value = sus_mount,
                        onValueChange = {
                            sus_mount = it
                        },
                        placeholder = {
                            Text(
                                text = "exp: /data/adb/modules"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
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
                                    sus_mount = getSUSListReset(context, "sus_mount")
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
                                    applySUSList(context, sus_mount, "sus_mount")
                                    Toast.makeText(context, "Apply Success", Toast.LENGTH_SHORT).show()
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
            ElevatedCard (
                modifier = Modifier
                    .fillMaxWidth()
            ){
                var try_umount by rememberSaveable { mutableStateOf(getSUSList(context, "try_umount")) }
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(23.dp),
                ){
                    Text(
                        modifier = Modifier,
                        text = "Try Umount",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        value = try_umount,
                        onValueChange = {
                            try_umount = it
                        },
                        placeholder = {
                            Text(
                                text = "exp: /data/adb/modules"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
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
                                    try_umount = getSUSListReset(context, "try_umount")
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
                                    applySUSList(context, try_umount, "try_umount")
                                    Toast.makeText(context, "Apply Success", Toast.LENGTH_SHORT).show()
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
            ElevatedCard (
                modifier = Modifier
                    .fillMaxWidth()
            ){
                var sus_path by rememberSaveable { mutableStateOf(getSUSList(context, "sus_path")) }
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(23.dp),
                ){
                    Text(
                        modifier = Modifier,
                        text = "SUS Path",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        value = sus_path,
                        onValueChange = {
                            sus_path = it
                        },
                        placeholder = {
                            Text(
                                text = "exp: /system/addon.d"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
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
                                    sus_path = getSUSListReset(context, "sus_path")
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
                                    applySUSList(context, sus_path, "sus_path")
                                    Toast.makeText(context, "Apply Success", Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(stringResource(R.string.custom_susfs_settings)) },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior,
        modifier = Modifier
            .graphicsLayer {
                shape = RoundedCornerShape(
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                )
                clip = true
            }
    )
}

@Preview
@Composable
private fun KernelPreview() {
    SuSFSScreen(EmptyDestinationsNavigator)
}
