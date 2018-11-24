package ru.stwtforever.fast.common;

import android.util.*;

import ru.stwtforever.fast.io.*;

import java.io.*;

import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.helper.*;

import android.*;
import android.os.*;

public class CrashManager {

    private static final String TAG = "CrashManager";
    private static final Thread.UncaughtExceptionHandler sOldHandler = Thread.getDefaultUncaughtExceptionHandler();
    public static final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            report(ex);
            if (sOldHandler != null) {
                sOldHandler.uncaughtException(thread, ex);
            }
        }
    };

    private CrashManager() {
    }

    private static void report(Throwable ex) {
        String trace = "Fast \nVersion: " + AppGlobal.app_version_name + "\nBuild: " + AppGlobal.app_version_code + "\n\n";

        trace +=
                "Android SDK: " + Build.VERSION.SDK + "\n" +
                        "Device: " + Build.DEVICE + "\n" +
                        "Model: " + Build.MODEL + "\n" +
                        "Brand: " + Build.BRAND + "\n" +
                        "Manufacturer: " + Build.MANUFACTURER + "\n" +
                        "Display: " + Build.DISPLAY + "\n";

        trace += "\nLog below: \n\n";
        trace += Log.getStackTraceString(ex);
        Utils.copyText(trace);

        String path = Environment.getExternalStorageDirectory() + "/Fast/crash_logs"; // AppGlobal.context.getFilesDir();

        File ff = new File(path);
        if (!ff.exists()) ff.mkdirs();

        String name = "log_" + System.currentTimeMillis() + ".txt";
        createFile(new File(path), name, trace);

        File f = new File(Environment.getExternalStorageDirectory() + "/Fast/crash_logs");
        if (!f.exists()) f.mkdirs();

        createFile(f, name, trace);

        Utils.getPrefs().edit().putBoolean("isCrashed", true).putString("crashLog", trace).apply();
    }

    private static void createFile(File path, String name, String trace) {
        File file = new File(path, name);
        try {
            FileStreams.write(trace, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER);
    }
}

