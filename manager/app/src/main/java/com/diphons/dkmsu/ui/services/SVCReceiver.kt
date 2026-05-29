package com.diphons.dkmsu.ui.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.diphons.dkmsu.ui.component.KeepShellPublic
import com.diphons.dkmsu.ui.util.dkmsvcDaemonPath

class SVCReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KeepShellPublic.doCmdSync("${dkmsvcDaemonPath()} stop\nsleep 1\n" +
                "${dkmsvcDaemonPath()} service")
    }
}
