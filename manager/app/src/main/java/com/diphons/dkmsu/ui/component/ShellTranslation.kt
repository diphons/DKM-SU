package com.diphons.dkmsu.ui.component

import android.content.Context
import com.diphons.dkmsu.ui.component.ResourceStringResolver

class ShellTranslation(context: Context) : ResourceStringResolver(context) {
    fun getTranslatedResult(shellCommand: String, executor: KeepShell?): String {
        val shell = executor ?: KeepShellPublic.getDefaultInstance()
        val rows = shell.doCmdSync(shellCommand).split("\n")
        return if (rows.isNotEmpty()) {
            resolveRows(rows)
        } else {
            ""
        }
    }
}