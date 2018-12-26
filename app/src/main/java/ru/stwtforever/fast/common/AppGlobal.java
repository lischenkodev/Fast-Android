package ru.stwtforever.fast.common;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.sqlite.*;
import android.os.*;

import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.*;

import java.util.*;

import android.util.*;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

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
        onLaunch();
        getVersionBuild();

        handler = new Handler(getMainLooper());

        AppCenter.start(this, "439a8c78-c0d0-4597-8a72-6a8be2c40d07",
                Analytics.class, Crashes.class);

        CrashManager.init();
    }

    private void getVersionBuild() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
            app_version_name = pInfo.versionName;
            app_version_code = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void putException(String s) {
        ArrayList<FException> e = new ArrayList<>();
        e.add(new FException(s, System.currentTimeMillis()));

        CacheStorage.insert(DBHelper.EXCEPTIONS_TABLE, e);
    }

    public static void onLaunch() {
        ThemeManager.init();
        preferences = Utils.getPrefs();
        database = DBHelper.getInstance().getWritableDatabase();
        locale = Locale.getDefault();
    }

    public static void putException(Exception e) {
        ArrayList<FException> exc = new ArrayList<>();
        exc.add(new FException(Log.getStackTraceString(e), Calendar.getInstance().getTimeInMillis()));

        CacheStorage.insert(DBHelper.EXCEPTIONS_TABLE, exc);
    }

    public static boolean isDebug() {
        String[] triggers = new String[]{
                "alpha", "beta", "charlie", "debug"
        };

        boolean isContains = false;

        for (String s : triggers) {
            if (app_version_name.toLowerCase().contains(s))
                isContains = true;
        }

        return isContains;
    }
}
