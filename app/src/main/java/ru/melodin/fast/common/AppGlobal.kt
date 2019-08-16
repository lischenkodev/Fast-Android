package ru.melodin.fast.common

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import ru.melodin.fast.database.DatabaseHelper
import java.util.*

class AppGlobal : Application() {

    companion object {

        lateinit var database: SQLiteDatabase

        lateinit var locale: Locale

        lateinit var handler: Handler

        lateinit var preferences: SharedPreferences

        var app_version_name = ""

        var app_version_code = -1

        lateinit var clipService: ClipboardManager

        lateinit var connectionService: ConnectivityManager

        lateinit var inputService: InputMethodManager

        lateinit var res: Resources

        fun isAlpha(): Boolean {
            return app_version_name.toLowerCase(Locale.getDefault()).contains("alpha")
        }

        fun isBeta(): Boolean {
            return app_version_name.toLowerCase(Locale.getDefault()).contains("beta")
        }

        fun isDebug(): Boolean {
            return isAlpha() || isBeta()
        }
    }

    override fun onCreate() {
        super.onCreate()
        res = resources
        handler = Handler(mainLooper)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        database = DatabaseHelper.getInstance(this).writableDatabase
        locale = Locale.getDefault()

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            app_version_name = pInfo.versionName
            app_version_code = PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        initServices()
        CrashManager.init()
        TaskManager.init()
        ThemeManager.init()
    }

    private fun initServices() {
        inputService = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        connectionService = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        clipService = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }


}
