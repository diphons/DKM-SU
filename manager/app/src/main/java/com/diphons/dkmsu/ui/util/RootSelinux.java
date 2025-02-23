/*
 * Copyright (C) 2015 Willi Ye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diphons.dkmsu.ui.util;

import android.content.Context;
import android.util.Log;

import com.diphons.dkmsu.ui.util.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by willi on 14.12.14.

 * Here you have different functions which will help you with root commands.
 * I think they are self explained and do no need any further descriptions.
 */
public class RootSelinux {

    private static SU su;

    public static boolean rooted() {
        return existBinary_se("su");
    }

    public static boolean rootAccess_se() {
        SU su = getSU();
        su.runCommand_se("echo /testRoot/");
        return !su.denied;
    }

    public static boolean hasAppletSupport(Context context) {
        String busybox = context.getFilesDir().getPath() + "/busybox";
        return Utils.existFile(busybox, true);
    }

    private static boolean existBinary_se(String binary) {
        for (String path: System.getenv("PATH").split(":")) {
            if (!path.endsWith("/")) path += "/";
            if (new File(path + binary).exists() || Utils.existFile(path + binary, true))
                return true;
        }
        return false;
    }

    public static String getKernelVersion() {
        return runCommand_se("uname -r");
    }

    public static void mountSystem(boolean writeable) {
        mount(writeable, "/system");
        mount(writeable, "/");
    }

    public static void mount(boolean writeable, String mountpoint) {
        if (writeable) runCommand_se("mount -o rw,remount " + mountpoint);
        else runCommand_se("mount -o ro,remount " + mountpoint);
    }

    public static void closeSU_se() {
        if (su != null) su.close();
        su = null;
    }

    public static String runCommand_se(String command) {
        return getSU().runCommand_se(command);
    }

    private static SU getSU() {
        if (su == null) su = new SU();
        else if (su.closed || su.denied) su = new SU();
        return su;
    }

    /*
     * Based on AndreiLux's SU code in Synapse
     * https://github.com/AndreiLux/Synapse/blob/master/src/main/java/com/af/synapse/utils/Utils.java#L238
     */
    public static class SU {

        private Process process;
        private BufferedWriter bufferedWriter;
        private BufferedReader bufferedReader;
        private final boolean root;
        private boolean closed;
        private boolean denied;
        private boolean firstTry;

        public SU() {
            this(true);
        }

        public SU(boolean root) {
            this.root = root;
            try {
                Log.i(Utils.TAG, root ? "SU initialized" : "SH initialized");
                firstTry = true;
                process = Runtime.getRuntime().exec(root ? "su" : "sh");
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            } catch (IOException e) {
                Log.e(Utils.TAG, root ? "Failed to run shell as su" : "Failed to run shell as sh");
                denied = true;
                closed = true;
            }
        }

        public synchronized String runCommand_se(final String command) {
            try {
                StringBuilder sb = new StringBuilder();
                String callback = "/shellCallback/";
                bufferedWriter.write(command + "\necho " + callback + "\n");
                bufferedWriter.flush();

                int i;
                char[] buffer = new char[256];
                while (true) {
                    sb.append(buffer, 0, bufferedReader.read(buffer));
                    if ((i = sb.indexOf(callback)) > -1) {
                        sb.delete(i, i + callback.length());
                        break;
                    }
                }
                firstTry = false;
                return sb.toString().trim();
            } catch (NullPointerException e) {
                Log.e(Utils.TAG, "catch NullPointerException runCommand as Su");
            } catch (IOException e) {
                closed = true;
                e.printStackTrace();
                if (firstTry) denied = true;
            } catch (ArrayIndexOutOfBoundsException e) {
                denied = true;
            } catch (Exception e) {
                e.printStackTrace();
                denied = true;
            }
            return null;
        }

        public void close() {
            try {
                bufferedWriter.write("exit\n");
                bufferedWriter.flush();

                process.waitFor();
                Log.i(Utils.TAG, root ? "SU closed: " + process.exitValue() : "SH closed: " + process.exitValue());
                closed = true;
            } catch (NullPointerException e) {
                Log.e(Utils.TAG, "catch NullPointerException close Su");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
