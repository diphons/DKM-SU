package com.diphons.dkmsu.ui.component

import com.diphons.dkmsu.ui.component.KeepShellPublic

/**
 * Created by Hello on 2017/8/8.
 */

object PropsUtils {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    fun getProp(propName: String): String {
        return KeepShellPublic.doCmdSync("getprop \"$propName\"")
    }

    fun ReadFile(filePath: String): String {
        return KeepShellPublic.doCmdSync("cat \"$filePath\"")
    }

    fun CmdShell(command: String): String {
        return KeepShellPublic.doCmdSync("$command")
    }

    fun setPorp(propName: String, value: String): Boolean {
        return KeepShellPublic.doCmdSync("setprop \"$propName\" \"$value\"") != "error"
    }
}
