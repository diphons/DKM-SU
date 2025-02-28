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

@Preview
@Composable
fun DialogSWAP() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
               DialogZRAMContent()
        }
    }
}

@Composable
fun DialogSwap(
        dismiss: () -> Unit
    ) {
    Dialog(
        onDismissRequest = { dismiss() }
    ) {
        DialogSWAP()
    }
}

@Composable
fun DialogSwapLoad() {
    Dialog(
        onDismissRequest = { }
    ) {
        DialogSWAP()
    }
}

@Composable
private fun DialogZRAMContent() {
    var swapMSG by rememberSaveable { mutableStateOf(CMD_MSG) }
    LaunchedEffect(Unit) {
        for (i in 1..200) {
            delay(1000) // update once a second
            swapMSG = CMD_MSG
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
                    text = swapMSG,
                    style = TextStyle(
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
