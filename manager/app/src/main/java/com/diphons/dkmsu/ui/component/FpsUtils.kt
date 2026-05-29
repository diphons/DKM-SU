package com.diphons.dkmsu.ui.component

import android.view.Choreographer
import com.diphons.dkmsu.ui.component.RootFile.fileExists

/**
 * Frame rate detection
 */
class FpsUtils(private val keepShell: KeepShell = KeepShellPublic.secondaryKeepShell) {
    private var fpsFilePath: String? = null
    private var subStrCommand = "| awk '{print \$2}'"

    private var fpsCommand2 = "service call SurfaceFlinger 1013"
    private var lastTime = -1L
    private var lastFrames = -1
    private var getFPS = 0F
    private var choreographerStarted = false
    private var lastFrameTimeNs = 0L
    private var frameCount = 0
    private var lastUpdateTimeNs = 0L
    private val updateIntervalNs = 1_000_000_000L // setiap 1 detik
    private val smoothing = 0.15f

    val currentFps: String?
        get() {
            if (!fpsFilePath.isNullOrEmpty()) {
                return keepShell.doCmdSync("cat $fpsFilePath $subStrCommand")
            } else if (fpsFilePath == null) {
                when {
                    fileExists("/sys/class/drm/sde-crtc-0/measured_fps") -> {
                        fpsFilePath = "/sys/class/drm/sde-crtc-0/measured_fps"
                    }
                    fileExists("/sys/class/graphics/fb0/measured_fps") -> {
                        fpsFilePath = "/sys/class/graphics/fb0/measured_fps"
                        subStrCommand = ""
                    }
                    else -> {
                        fpsFilePath = ""
                        val keepShell = KeepShell()
                        Thread(Runnable {
                            try {
                                keepShell.doCmdSync("find /sys -name measured_fps 2>/dev/null")
                                    .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.minOrNull()?.run {
                                        fpsFilePath = this
                                    }

                                if (fpsFilePath == null || fpsFilePath == "") {
                                    keepShell.doCmdSync("find /sys -name fps 2>/dev/null")
                                        .trim { it <= ' ' }.split("\n").filter { it.contains("crtc") }.minOrNull()
                                        ?.run {
                                            fpsFilePath = this
                                        }
                                }
                                if (fpsFilePath == null) {
                                    fpsFilePath = ""
                                }
                                keepShell.tryExit()
                            } catch (_: Exception) {
                                fpsFilePath = ""
                            }
                        }).start()
                    }
                }
            } else if (fpsCommand2.isNotEmpty()) {
                val result = keepShell.doCmdSync(fpsCommand2).trim()
                if (result != "error" && !result.contains("Parcel")) {
                    fpsCommand2 = ""
                } else {
                    try {
                        val index = result.indexOf("(") + 1
                        val frames = Integer.parseInt(result.substring(index, index + 8), 16)
                        val time = System.currentTimeMillis()
                        if (lastTime > 0 && lastFrames > 0) {
                            getFPS = (frames - lastFrames) * 1000.0f / (time - lastTime)
                        }
                        lastFrames = frames
                        lastTime = time
                        return String.format("%.1f", getFPS)
                    } catch (_: Exception) {
                        if (!(lastTime > 0 && lastFrames > 0)) {
                            fpsCommand2 = ""
                        }
                    }
                }
            } else {
                if (!choreographerStarted)
                    startChoreographerFps()
                return String.format("%.1f", getFPS)
            }
            return null
        }

    val fps: Float
        get() {
            val fpsStr = currentFps
            if (fpsStr != null) {
                try {
                    return fpsStr.toFloat()
                } catch (_: java.lang.Exception) {
                }
            }
            return -0f
        }

    private fun startChoreographerFps() {
        choreographerStarted = true
        val choreographer = Choreographer.getInstance()

        fun frameCallback(frameTimeNanos: Long) {
            if (lastFrameTimeNs == 0L) {
                lastFrameTimeNs = frameTimeNanos
                lastUpdateTimeNs = frameTimeNanos
            } else {
                frameCount++
                val elapsedNs = frameTimeNanos - lastUpdateTimeNs
                if (elapsedNs >= updateIntervalNs) {
                    val fpsNow = frameCount * 1_000_000_000.0 / elapsedNs
                    getFPS = getFPS * (1 - smoothing) + fpsNow.toFloat() * smoothing
                    frameCount = 0
                    lastUpdateTimeNs = frameTimeNanos
                }
                lastFrameTimeNs = frameTimeNanos
            }
            choreographer.postFrameCallback(::frameCallback)
        }

        choreographer.postFrameCallback(::frameCallback)
    }
}