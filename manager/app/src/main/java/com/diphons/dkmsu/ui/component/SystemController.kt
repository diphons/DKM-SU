package com.diphons.dkmsu.ui.component

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.provider.Settings
import android.widget.SeekBar
import androidx.core.content.edit

class SystemController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val contentResolver: ContentResolver = context.contentResolver

    fun setupVolumeSeekBar(seekBar: SeekBar) {
        val currentVolume = getCurrentVolume()
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        seekBar.max = maxVolume
        seekBar.progress = currentVolume
    }

    fun setVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                volume,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentVolume(): Int {
        return try {
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (e: Exception) {
            e.printStackTrace()
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2 // Fallback
        }
    }

    fun setupBrightnessSeekBar(seekBar: SeekBar) {
        val currentBrightness = getCurrentBrightness()
        seekBar.max = 255
        seekBar.progress = currentBrightness

        val isAutoBrightnessEnabled = isAutoBrightnessEnabled()
        seekBar.isEnabled = !isAutoBrightnessEnabled
    }

    fun setBrightness(brightness: Int): Boolean {
        if (isAutoBrightnessEnabled()) {
            return false
        }

        if (!Settings.System.canWrite(context)) {
            return false
        }

        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
            return true
        } catch (e: SecurityException) {
            e.printStackTrace()
            return false
        }
    }

    fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    fun isAutoBrightnessEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }

    fun setAutoBrightness(enabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) {
            return false
        }

        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            return true
        } catch (e: SecurityException) {
            e.printStackTrace()
            return false
        }
    }

    fun toggleAutoBrightness(): Boolean {
        val currentState = isAutoBrightnessEnabled()
        return setAutoBrightness(!currentState)
    }

    fun getBrightnessStatus(): String {
        return if (isAutoBrightnessEnabled()) {
            "Auto"
        } else {
            "Manual (${getCurrentBrightness()})"
        }
    }
}
