package ru.melodin.fast.common;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import ru.melodin.fast.io.FileStreams;

class CrashManager {

    private static final String TAG = "CrashManager";
    private static final Thread.UncaughtExceptionHandler sOldHandler = Thread.getDefaultUncaughtExceptionHandler();
    private static final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new Thread.UncaughtExceptionHandler() {
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
        String s = "Fast \nVersion: " + AppGlobal.app_version_name + "\nBuild: " + AppGlobal.app_version_code + "\n\n";

        String path = Environment.getExternalStorageDirectory() + "/Fast/crash_logs"; // AppGlobal.context.getFilesDir();

        File file = new File(path);
        if (!file.exists()) file.mkdirs();

        String text = s +
                "Android SDK: " + Build.VERSION.SDK_INT +
                "\n" +
                "Device: " + Build.DEVICE +
                "\n" +
                "Model: " + Build.MODEL +
                "\n" +
                "Brand: " + Build.BRAND +
                "\n" +
                "Manufacturer: " + Build.MANUFACTURER +
                "\n" +
                "Display: " + Build.DISPLAY +
                "\n" +
                "\n" + "Log below:" + "\n" + "\n" +
                Log.getStackTraceString(ex);

        String name = "log_" + System.currentTimeMillis() + ".txt";
        createFile(file, name, text);

        AppGlobal.preferences.edit().putBoolean("isCrashed", true).putString("crashLog", text).apply();
    }

    private static void createFile(File path, String name, String trace) {
        File file = new File(path, name);
        try {
            FileStreams.write(trace, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void init() {
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER);
    }
}

