package com.diphons.dkmsu.ui.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShellExecutor {
    private static String extraEnvPath = "";
    private static String defaultEnvPath = ""; // /sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin

    public static void setExtraEnvPath(String extraEnvPath) {
        ShellExecutor.extraEnvPath = extraEnvPath;
    }

    private static String getEnvPath() {
        if (extraEnvPath != null && !extraEnvPath.isEmpty()) {
            if (defaultEnvPath.isEmpty()) {
                try {
                    Process process = Runtime.getRuntime().exec("sh");
                    OutputStream outputStream = process.getOutputStream();
                    outputStream.write("echo $PATH".getBytes());
                    outputStream.flush();
                    outputStream.close();

                    InputStream inputStream = process.getInputStream();
                    byte[] cache = new byte[16384];
                    int length = inputStream.read(cache);
                    inputStream.close();
                    process.destroy();

                    String path = new String(cache, 0, length).trim();
                    if (path.length() > 0) {
                        defaultEnvPath = path;
                    } else {
                        throw new RuntimeException("Failed to get the $PATH parameter");
                    }
                } catch (Exception ex) {
                    defaultEnvPath = "/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin";
                }
            }

            String path = defaultEnvPath;

            return ("PATH=" + path + ":" + extraEnvPath);
        }

        return null;
    }

    private static Process getProcess(String run) throws IOException {
        String env = getEnvPath();
        Runtime runtime = Runtime.getRuntime();
        /*
        // 部分机型会有Aborted错误
        if (env != null) {
            return runtime.exec(run, new String[]{
                env
            });
        }
        */
        Process process = runtime.exec(run);
        if (env != null) {
            OutputStream outputStream = process.getOutputStream();
            outputStream.write("export ".getBytes());
            outputStream.write(env.getBytes());
            outputStream.write("\n".getBytes());
            outputStream.flush();
        }
        return process;
    }

    public static Process getSuperUserRuntime() throws IOException {
        return getProcess("su");
    }

    public static Process getRuntime() throws IOException {
        return getProcess("sh");
    }
}
