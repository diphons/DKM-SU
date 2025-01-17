package com.diphons.dkmsu.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diphons.dkmsu.R
import com.diphons.dkmsu.ui.util.Utils.*
import kotlinx.coroutines.delay
import java.util.Locale

@Preview
@Composable
fun DialogCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (DIALOG_MODE == 1)
                DialogSVCContent()
            else if (DIALOG_MODE == 2)
                DialogUFSContent()
            else if (DIALOG_MODE == 3)
                DialogPermContent()
            else
                DialogCardContent()
        }
    }
}

@Composable
fun runDialog(dismiss: () -> Unit) {
    Dialog(
        onDismissRequest = { dismiss() }
    ) {
        DialogCard()
    }
}

@Composable
private fun DialogCardContent() {
    DIALOG_MODE = 0
    var pif_update by rememberSaveable { mutableStateOf("Checking internet connection...") }
    LaunchedEffect(Unit) {
        for (i in 1..200) {
            delay(1000) // update once a second
            if (AV_INTERNET) {
                if (CMD_MSG.isNotEmpty())
                    pif_update = CMD_MSG
                if (pif_update.contains("Checking internet")) {
                    if (!pif_update.contains("Your PIF") && pif_update.contains("failed"))
                        pif_update = "Update PIF Failed\nCheck your internet connection"
                    else
                        pif_update = "Checking internet connection...${replacDoubleToEmpty(CMD_MSG, "Checking internet connection...")}"
                }
            } else
                pif_update = "Update PIF Failed\nCheck your internet connection"
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Column {
                Text(
                    stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pif_update,
                    style = TextStyle(
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun DialogSVCContent() {
    DIALOG_MODE = 0
    var dkm_service by rememberSaveable { mutableStateOf(CMD_MSG) }
    LaunchedEffect(Unit) {
        for (i in 1..200) {
            delay(1000) // update once a second
            dkm_service = CMD_MSG
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Column {
                Text(
                    stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dkm_service,
                    style = TextStyle(
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun DialogUFSContent() {
    DIALOG_MODE = 0
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Column {
                Text(
                    stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ufs_health_info_die),
                    style = TextStyle(
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun DialogPermContent() {
    DIALOG_MODE = 0
    var apply_perm by rememberSaveable { mutableStateOf(CMD_MSG) }
    LaunchedEffect(Unit) {
        for (i in 1..200) {
            delay(1000) // update once a second
            apply_perm = CMD_MSG
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Column {
                Text(
                    stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (apply_perm.isEmpty()) "Trying to set permissions.." else "Set Permissions ${apply_perm.replace("_", " ").lowercase(Locale.getDefault())}",
                    style = TextStyle(
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
