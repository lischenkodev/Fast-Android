package ru.stwtforever.fast.common;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.Locale;

import ru.stwtforever.fast.database.DatabaseHelper;

public class AppGlobal extends Application {

    public static volatile Context context;
    public static volatile SQLiteDatabase database;
    public static volatile Locale locale;
    public static volatile Handler handler;
    public static volatile SharedPreferences preferences;

    public static volatile String app_version_name;
    public static volatile int app_version_code;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        handler = new Handler(getMainLooper());
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        database = DatabaseHelper.getInstance().getWritableDatabase();
        locale = Locale.getDefault();

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
            app_version_name = pInfo.versionName;
            app_version_code = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        AppCenter.start(this, "439a8c78-c0d0-4597-8a72-6a8be2c40d07",
                Analytics.class, Crashes.class);

        CrashManager.init();
        ThemeManager.init();
    }
}
