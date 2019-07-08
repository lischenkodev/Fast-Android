package ru.melodin.fast.common

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import ru.melodin.fast.database.DatabaseHelper
import java.util.*

class AppGlobal : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        handler = Handler(mainLooper)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        database = DatabaseHelper.getInstance().writableDatabase
        locale = Locale.getDefault()

        try {

            val pInfo = packageManager.getPackageInfo(packageName, 0)
            app_version_name = pInfo.versionName
            app_version_code = PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        CrashManager.init()
        TaskManager.init()
        ThemeManager.init()
    }

    companion object {

        @Volatile
        lateinit var database: SQLiteDatabase

        @Volatile
        lateinit var locale: Locale

        @Volatile
        lateinit var handler: Handler

        @Volatile
        lateinit var preferences: SharedPreferences

        @Volatile
        lateinit var app_version_name: String

        @Volatile
        var app_version_code: Int = -1

        @get:Synchronized
        lateinit var context: AppGlobal
    }
}
