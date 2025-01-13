package com.diphons.dkmsu.ui.util;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import androidx.appcompat.content.res.AppCompatResources;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {
    public static String D8G_PATH = "/sys/module/d8g_helper/parameters/";
    public static String OPROFILE = D8G_PATH + "oprofile";
    public static String GAME_AI = D8G_PATH + "game_ai_enable";
    public static String GAME_AI_LOG = D8G_PATH + "game_ai_log";
    public static String HAPTIC_LEVEL = D8G_PATH + "haptic_gain";
    public static String DYNAMIC_CHARGING = D8G_PATH + "dynamic_charger";
    public static String SKIP_THERMAL = D8G_PATH + "skip_thermal_main";
    public static String BLOCK_JOYOSE = D8G_PATH + "block_joyose";
    public static String FCHG_SYS = "/sys/kernel/fast_charge/force_fast_charge";
    public static String CPU_WQ_POWER_SAVING = "/sys/module/workqueue/parameters/power_efficient";
    public static String DT2W = "/sys/touchpanel/double_tap";
    public static String DT2W_LEGACY = "/proc/tp_gesture";
    public static String TOUCH_SAMPLE = "/sys/devices/virtual/touch/touch_dev/bump_sample_rate";
    public static String SOUND_CONTROL = "/sys/kernel/sound_control";
    public static String HEADPHONE_GAIN = SOUND_CONTROL + "/headphone_gain";
    public static String MICROPHONE_GAIN = SOUND_CONTROL + "/mic_gain";
    public static String EARPIECE_GAIN = SOUND_CONTROL + "/earpiece_gain";

    public static String DISPLAY_COLOR = "/sys/module/msm_drm/parameters/";
    public static String DISPLAY_RED = DISPLAY_COLOR + "kcal_red";
    public static String DISPLAY_GREEN = DISPLAY_COLOR + "kcal_green";
    public static String DISPLAY_BLUE = DISPLAY_COLOR + "kcal_blue";
    public static String DISPLAY_SATURATED = DISPLAY_COLOR + "kcal_sat";
    public static String DISPLAY_VALUE = DISPLAY_COLOR + "kcal_val";
    public static String DISPLAY_CONTRAST = DISPLAY_COLOR + "kcal_cont";
    public static String DISPLAY_HUE = DISPLAY_COLOR + "kcal_hue";

    public static final String CEK_MIUI = "/system/product/priv-app/MiuiSystemUI/MiuiSystemUI.apk";
    public static final String CEK_MIUI2 = "/system_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk";
    public static final String CEK_MIUI3 = "/system/priv-app/MiuiSystemUI/MiuiSystemUI.apk";
    public static final String CEK_OP = "/system/system_ext/priv-app/OPSystemUI/OPSystemUI.apk";
    public static final String CEK_OP2 = "/system/priv-app/OPSystemUI/OPSystemUI.apk";
    public static final String CEK_OP3 = "/system/product/priv-app/OPSystemUI/OPSystemUI.apk";

    public static final String CPUFREQ = "/sys/devices/system/cpu/cpufreq";
    public static final String CPUFREQ_FULL_TABLE = "/stats/trans_table";
    public static final String CPU_GOV = "scaling_governor";
    public static final String CPU_INFO_MAX = "cpuinfo_max_freq";
    public static final String CPU_INFO_MIN = "cpuinfo_min_freq";
    public static final String CPU_MAX_FREQ = "scaling_max_freq";
    public static final String CPU_MIN_FREQ = "scaling_min_freq";
    public static final String CPU_AV_GOV = "scaling_available_governors";
    public static final String CPU_AV_FREQ = "scaling_available_frequencies";

    public static String THERMAL_PROFILE = "/sys/class/thermal/thermal_message/sconfig";
    public static String CHG_CUR_MAX = "/sys/class/power_supply/battery/constant_charge_current_max";
    public static String NIGHT_CHARGING = "/sys/class/power_supply/battery/night_charging";
    public static String STEP_CHARGING = "/sys/class/power_supply/battery/step_charging_enabled";
    public static String PIF_UPDATER = "/vendor/bin/oemports10t_PIF-updater";
    public static String CMD_MSG = "";

    public static String GPU_ADRENO = "/sys/class/kgsl/kgsl-3d0";
    public static String GPU_MALI = "/sys/class/devfreq/gpufreq";
    public static String ADRENO_BOOST = "devfreq/adrenoboost";
    public static String GPU_AV_FREQ = "devfreq/available_frequencies";
    public static String GPU_AV_CLK = "gpu_available_frequencies";
    public static String GPU_AV_GOV = "devfreq/available_governors";
    public static String GPU_GOV = "devfreq/governor";
    public static String GPU_MAX_FREQ = "devfreq/max_freq";
    public static String GPU_MIN_FREQ = "devfreq/min_freq";
    public static String GPU_MAX_CKL = "max_clock_mhz"; //Split GPU Clk
    public static String GPU_MAX_GPUCLK = "max_gpuclk";
    public static String GPU_MIN_CKL = "min_clock_mhz"; //Split GPU Clk
    public static String GPU_MIN_GPUCLK = "min_gpuclk";
    public static String GPU_DEF_PWRLEVEL = "default_pwrlevel";
    public static String GPU_NUM_PWRLEVEL = "num_pwrlevels";
    public static String IO_PATH = "/sys/block";
    public static String IO_SCHEDULER = "queue/scheduler";
    public static String TOUCH_DEV = "/sys/devices/virtual/touch/touch_dev";
    public static String DATA_DATA = "/data/data";
    public static Boolean AV_INTERNET = false;

    public static Integer DIALOG_MODE = 0;
    public static String SUS_MOUNT() {
        return "# example\n# /system\n# /system_ext\n/data/adb/modules\n/debug_ramdisk\n" ;
    }
    public static String TRY_UMOUNT() {
        return "# example\n# /system\n# /system_ext\n/debug_ramdisk\n" ;
    }
    public static String SUS_PATH() {
        return "# example\n/system/addon.d\n/vendor/bin/install-recovery.sh\n/system/bin/install-recovery.sh\n" ;
    }

    public static int strToInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static String replacDoubleToEmpty(String text, String replace) {
        String result = text;
        result = result.replaceAll(replace, "");
        return result;
    }
    public static void writeFile(String path, String text, boolean append, boolean asRoot) {
        if (asRoot) {
            new RootFile(path).write(text, append);
            return;
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(path, append);
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + path);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readFile(String file) {
        return readFile(file, true);
    }

    public static String readFile(String file, boolean root) {
        if (root) {
            return new RootFile(file).readFile();
        }

        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader(file));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = buf.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            return stringBuilder.toString().trim();
        } catch (IOException ignored) {
        } finally {
            try {
                if (buf != null) buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean existFile(String file) {
        return existFile(file, true);
    }

    public static boolean existFile(String file, boolean root) {
        return !root ? new File(file).exists() : new RootFile(file).exists();
    }

    public static String removeSuffix(@Nullable String s, @Nullable String suffix) {
        if (s != null && suffix != null && s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    public static String checkFPSLegacy() {
        String cekLinux = RootUtils.runAndGetOutput("grep '4.19' /proc/version");
        String cfps1;
        if (cekLinux.isEmpty()) {
            cfps1 = RootUtils.runAndGetOutput("getdata=$(dumpsys display | grep 'fps='); rfps=${getdata%%.0,*}; rfps2=${rfps##*=}; get_fps=${rfps2%%.0*}; echo $get_fps\n");
        } else {
            cfps1 = RootUtils.runAndGetOutput("getdata=$(dumpsys display | sed 's/, /\n/g' | grep 'fps=' | sed 's/fps=//g'); get_fps=${getdata%%.*}; echo $get_fps\n");
        }
        if ((!cfps1.contains("71")) && (!cfps1.contains("70")) && (!cfps1.contains("69")) &&
                (!cfps1.contains("68")) && (!cfps1.contains("67")) && (!cfps1.contains("66")) &&
                (!cfps1.contains("65")) && (!cfps1.contains("61")) && (!cfps1.contains("60"))) {
            cfps1 = "60";
        }
        return cfps1;
    }

    public static void setSuSFSLog(Boolean value){
        Integer result = 0;
        if (value)
            result = 1;
        RootUtils.runCommand("susfs enable_log " + result );
    }
    public static void hideSuSFS_Cusrom(){
        RootUtils.runCommand("find /system /vendor /system_ext /product -type f -o -type d | grep -iE \"lineage|crdroid\" | while read -r path; do\n" +
                "\tsusfs add_sus_path \"$path\"\n" +
                "done\n");
    }
    public static void hideSuSFS_GAPPS(){
        RootUtils.runCommand("for i in $(find /system /vendor /system_ext /product -iname *gapps*xml -o -type d -iname *gapps*) ; do \n" +
                "\tsusfs add_sus_path $i \n" +
                "done\n");
    }
    public static void hideSuSFS_cmdline(Context context){
        RootUtils.runCommand("sed 's|androidboot.verifiedbootstate=orange|androidboot.verifiedbootstate=green|g' /proc/cmdline > /data/data/" + context.getPackageName() + "/files/cmdline\n" +
                "susfs set_proc_cmdline /data/data/" + context.getPackageName() + "/files/cmdline\n");
    }
    public static void hideSuSFS_loop(){
        RootUtils.runCommand("for device in $(ls -Ld /proc/fs/jbd2/loop*8 | sed 's|/proc/fs/jbd2/||; s|-8||'); do\n" +
                "\tsusfs add_sus_path /proc/fs/jbd2/${device}-8\n" +
                "\tsusfs add_sus_path /proc/fs/ext4/${device}\n" +
                "done\n");
    }
    public static void hideSuSFS_lsposed(){
        RootUtils.runCommand("for device in $(ls -Ld /proc/fs/jbd2/loop*8 | sed 's|/proc/fs/jbd2/||; s|-8||'); do\n" +
                "\t$susfs add_sus_path /proc/fs/jbd2/${device}-8\n" +
                "\tsusfs add_sus_path /proc/fs/ext4/${device}\n" +
                "done\n");
    }

    public static int color_start = Color.parseColor("#FFFFFF");
    public static int color_end = Color.parseColor("#FFFFFF");

    public static Bitmap getBitmapFromDrawable(Integer mode, Drawable drawable) {
        GradientDrawable gd = new GradientDrawable();
        // Set the color array to draw gradient
        gd.setColors(new int[]{
                Color.parseColor("#0D2446"),
                Color.parseColor("#112e59"),
                Color.parseColor("#133465"),
                Color.parseColor("#163d75")
        });
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gd.setShape(GradientDrawable.RECTANGLE);
        Paint p = new Paint();
        p.setDither(true);

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawableCompat || drawable instanceof VectorDrawable) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            // Set GradientDrawable width and in pixels
            gd.setSize(canvas.getWidth(), canvas.getHeight()); // Width 450 pixels and height 150 pixels
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

            RadialGradient radial = new RadialGradient((float) canvas.getWidth() / 2, (float) canvas.getHeight() /2, canvas.getWidth(), color_end,
                    color_start, android.graphics.Shader.TileMode.CLAMP);
            LinearGradient linear = new LinearGradient(0,0,0, canvas.getHeight(), color_start, color_end, Shader.TileMode.MIRROR);
            if (mode == 1) {
                p.setShader(radial);
            } else {
                p.setShader(linear);
            }

            canvas.drawRect(0,0,canvas.getWidth(),canvas.getHeight(),p);
            drawable.draw(canvas);

            return bitmap;
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    public static Bitmap getBitmapFromDrawable(Context context, Integer mode, @DrawableRes int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
        return getBitmapFromDrawable(mode, drawable);
    }
}
