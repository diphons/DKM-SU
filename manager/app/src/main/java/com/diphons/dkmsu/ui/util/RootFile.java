/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.diphons.dkmsu.ui.util;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 30.12.15.
 */

// TODO: 22/04/20 Perhaps use com.github.topjohnwu.libsu:io
public class RootFile {

    private final String mFile;

    public RootFile(String file) {
        mFile = file;
    }

    public String getName() {
        return new File(mFile).getName();
    }

    public void mkdir() {
        Shell.cmd("mkdir -p '" + mFile + "'").exec();
    }

    public RootFile mv(String newPath) {
        Shell.cmd("mv -f '" + mFile + "' '" + newPath + "'").exec();
        return new RootFile(newPath);
    }

    public void cp(String path) {
        Shell.cmd("cp -r '" + mFile + "' '" + path + "'").exec();
    }

    public void write(String text, boolean append) {
        String[] array = text.split("\\r?\\n");
        if (!append) delete();
        for (String line : array) {
            Shell.cmd("echo '" + line + "' >> " + mFile).exec();
        }
        RootUtils.chmod(mFile, "755");
    }

    public void execute() {
        RootUtils.runCommand("sh " + mFile);
    }

    public void delete() {
        Shell.cmd("rm -r '" + mFile + "'").exec();
    }

    public List<String> list() {
        List<String> list = new ArrayList<>();
        String files = RootUtils.runAndGetOutput("ls '" + mFile + "/'");
        if (!files.isEmpty()) {
            // Make sure the files exists
            for (String file : files.split("\\r?\\n")) {
                if (file != null && !file.isEmpty() && Utils.existFile(mFile + "/" + file)) {
                    list.add(file);
                }
            }
        }
        return list;
    }

    public List<RootFile> listFiles() {
        List<RootFile> list = new ArrayList<>();
        String files = RootUtils.runAndGetOutput("ls '" + mFile + "/'");
        if (!files.isEmpty()) {
            // Make sure the files exists
            for (String file : files.split("\\r?\\n")) {
                if (file != null && !file.isEmpty() && Utils.existFile(mFile + "/" + file)) {
                    list.add(new RootFile(mFile.equals("/") ? mFile + file : mFile + "/" + file));
                }
            }
        }
        return list;
    }

    public boolean isEmpty() {
        return "false".equals(RootUtils.runAndGetOutput("find '" + mFile + "' -mindepth 1 | read || echo false"));
    }

    public boolean exists() {
        String output = RootUtils.runAndGetOutput("[ -e " + mFile + " ] && echo true");
        return output.equals("true");
    }

    public String readFile() {
        return RootUtils.runAndGetOutput("cat '" + mFile + "'");
    }

    public RootFile getParentFile() {
        return new RootFile(new File(mFile).getParent());
    }

    @NonNull
    @Override
    public String toString() {
        return mFile;
    }
}
