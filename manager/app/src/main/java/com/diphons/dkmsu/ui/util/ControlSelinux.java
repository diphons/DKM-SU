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
import android.nfc.Tag;
import android.util.Log;

import com.diphons.dkmsu.ui.util.Utils;
import com.diphons.dkmsu.ui.data.CommandDB;
import com.diphons.dkmsu.ui.util.RootUtils;
import com.diphons.dkmsu.ui.util.RootSelinux;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by willi on 14.12.14.
 */
public class ControlSelinux {

    public enum CommandType {
        GENERIC,
        CPU,
        CPU_LITTLE,
        FAUX_GENERIC,
        CUSTOM,
        SHELL,
        ASTERISK
    }

    public static void commandSaver(final Context context, final String path, final String command) {
        CommandDB commandDB = new CommandDB(context);
        if (path != null && command != null && !path.equals("null") && !command.equals("null")) {
            List < CommandDB.CommandItem > commandItems = commandDB.getAllCommands();
            for (int i = 0; i < commandItems.size(); i++) {
                String p = commandItems.get(i).getPath();
                if (p != null && p.equals(path)) {
                    commandDB.delete(i);
                }
            }
            commandDB.putCommand(path, command);
            commandDB.commit();
        } else {
            Log.i(Utils.TAG, "Unable to save command due to null values.");
        }
    }

    public static void run(String command, String path, Context context) {
        if (path != null && command != null && !path.equals("null") && !command.equals("null")) {
            RootSelinux.runCommand_se(command);
            commandSaver(context, path, command);
            Log.i(Utils.TAG, "Run command: " + command);
        } else {
            Log.i(Utils.TAG, "Unable to run command due to null values.");
        }
    }


    private static int getChecksum(int arg1, int arg2) {
        return 255 & (Integer.MAX_VALUE ^ (arg1 & 255) + (arg2 & 255));
    }

    public static void setPermission(String file, int permission, Context context) {
        run("chmod " + permission + " " + file, file + "permission" + permission, context);
    }

    private static void runGeneric(String file, String value, String id, Context context) {
        run("echo " + value + " > " + file, id != null ? file + id : file, context);
    }

    private static void runAsterisk(String file, String value, String id, Context context) {
        run("path=(" + file + ") && echo " + value + " > \"${path[0]}\"", id != null ? file + id : file, context);
    }

    private static void runShell(String value, String command, Context context) {
        run(command + " " + value, command, context);
    }

    private static void runFauxGeneric(String file, String value, Context context) {
        String command = value.contains(" ") ? value + " " + getChecksum(Utils.stringToInt(value.split(" ")[0]),
            Utils.stringToInt(value.split(" ")[1])) : value + " " + getChecksum(Utils.stringToInt(value), 0);
        run("echo " + value + " > " + file, file + "nochecksum", context);
        run("echo " + command + " > " + file, file, context);
    }

    public static void setProp(String key, String value, Context context) {
        run("setprop " + key + " " + value, key, context);
    }

    public static void startService(String service, Context context) {
        RootSelinux.runCommand_se("start " + service);

        if (context != null) commandSaver(context, service, "start " + service);
    }

    public static void stopService(String service, Context context) {
        RootSelinux.runCommand_se("stop " + service);

        if (context != null) commandSaver(context, service, "stop " + service);
    }

    private static final List < Thread > tasks = new ArrayList < > ();

    public static void runCommand_se(final String value, final String file, final CommandType command, final String id,
        final Context context) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (command == CommandType.GENERIC) {
                    runGeneric(file, value, id, context);
                } else if (command == CommandType.ASTERISK) {
                    runAsterisk(file, value, id, context);
                } else if (command == CommandType.FAUX_GENERIC) {
                    runFauxGeneric(file, value, context);
                } else if (command == CommandType.CUSTOM) {
                    ControlSelinux.run(value, id == null ? file : file + id, context);
                } else if (command == CommandType.SHELL) {
                    runShell(value, file, context);
                }
            }
        });

        tasks.add(thread);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    if (tasks.get(0) == thread) {
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        tasks.remove(thread);
                        break;
                    }
            }
        }).start();
    }

    public static void runCommand_se(final String value, final String file, final CommandType command, final Context context) {
        runCommand_se(value, file, command, null, context);
    }

    public static void deletespecificcommand(final Context context, final String path, final String command) {
        CommandDB commandDB = new CommandDB(context);

        List < CommandDB.CommandItem > commandItems = commandDB.getAllCommands();
        if (path == null && command == null) {
            for (int i = 0; i <= commandItems.size(); i++) {
                commandDB.delete(0);
            }
        } else {
            for (int i = 0; i < commandItems.size(); i++) {
                String p = commandItems.get(i).getPath();
                String c = commandItems.get(i).getCommand();
                if (p != null && p.equals(path) || c.equals(command)) {
                    commandDB.delete(i);
                }
            }
        }
        commandDB.commit();
    }

}
