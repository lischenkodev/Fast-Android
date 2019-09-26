package ru.melodin.fast.common

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ru.melodin.fast.R
import ru.melodin.fast.fragment.FragmentMessages
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.io.FileStreams
import ru.melodin.fast.util.Constants
import java.io.File
import java.io.IOException

internal object CrashManager {

    private val sOldHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val EXCEPTION_HANDLER = Thread.UncaughtExceptionHandler { thread, ex ->
        report(ex)
        sOldHandler?.uncaughtException(thread, ex)
    }

    private fun report(ex: Throwable) {
        val s =
            "#crash\n\nFast \nVersion: " + AppGlobal.app_version_name + "\nBuild: " + AppGlobal.app_version_code + "\n\n"

        val text = s + getInfo(ex)

        AppGlobal.preferences.edit().putBoolean("isCrashed", true).putString("crashLog", text)
            .apply()

        val path = Environment.getExternalStorageDirectory().toString() + "/Fast/crash_logs"

        val file = File(path)
        if (!file.exists()) file.mkdirs()

        val name = "log_" + System.currentTimeMillis() + ".txt"
        createFile(file, name, text)
    }

    fun getInfo(e: Throwable?): String {
        return "Android: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})\n" +
                "Device: ${Build.DEVICE}\n" +
                "Model: ${Build.MODEL}\n" +
                "Brand: ${Build.BRAND}\n" +
                "Manufacturer: ${Build.MANUFACTURER}\n" +
                "Display: ${Build.DISPLAY}" +
                (if (e != null)
                    "\n\nLog below:\n\n${Log.getStackTraceString(e)}"
                else "")
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

    fun checkCrash(activity: AppCompatActivity) {
        if (AppGlobal.preferences.getBoolean(FragmentSettings.KEY_CRASHED, false)) {
            val trace = AppGlobal.preferences.getString(FragmentSettings.KEY_CRASH_LOG, "")!!

            AppGlobal.preferences.edit()
                .putBoolean(FragmentSettings.KEY_CRASHED, false)
                .putString(FragmentSettings.KEY_CRASH_LOG, "")
                .apply()

            if (!AppGlobal.preferences.getBoolean(FragmentSettings.KEY_SHOW_ERROR, false))
                return

            val adb = AlertDialog.Builder(activity)
            adb.setTitle(R.string.warning)

            adb.setMessage(R.string.app_crashed)
            adb.setPositiveButton(android.R.string.ok, null)
            adb.setNeutralButton(R.string.report) { _, _ ->
                FragmentSelector.selectFragment(
                    activity.supportFragmentManager,
                    FragmentMessages(),
                    Bundle().apply {
                        putInt("peer_id", Constants.BOT_ID)
                        putString("text", trace)
                    },
                    true
                )
            }

            adb.show()
        }
    }
}