package ru.melodin.fast.common

import android.os.Build
import android.os.Environment
import android.util.Log
import ru.melodin.fast.io.FileStreams
import java.io.File
import java.io.IOException

internal object CrashManager {

    private val sOldHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val EXCEPTION_HANDLER = Thread.UncaughtExceptionHandler { thread, ex ->
        report(ex)
        sOldHandler?.uncaughtException(thread, ex)
    }

    private fun report(ex: Throwable) {
        val s = "Fast \nVersion: " + AppGlobal.app_version_name + "\nBuild: " + AppGlobal.app_version_code + "\n\n"

        val text = s +
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
                Log.getStackTraceString(ex)

        AppGlobal.preferences.edit().putBoolean("isCrashed", true).putString("crashLog", text).apply()

        val path = Environment.getExternalStorageDirectory().toString() + "/Fast/crash_logs"

        val file = File(path)
        if (!file.exists()) file.mkdirs()

        val name = "log_" + System.currentTimeMillis() + ".txt"
        createFile(file, name, text)
    }

    private fun createFile(path: File, name: String, trace: String) {
        val file = File(path, name)
        try {
            FileStreams.write(trace, file)
        } catch (ignored: IOException) {
        }

    }

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER)
    }
}

