package ru.stwtforever.fast.common;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionManager {

    private static Activity activity;

    public static void setActivity(Activity activity) {
        PermissionManager.activity = activity;
    }

    public static boolean isGrantedPermission(String permission) {
        if (checkIsL()) return true;

        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGranted(int... res) {
        boolean[] granted = new boolean[res.length];
        boolean all_granted = true;

        for (int i = 0; i < res.length; i++) {
            granted[i] = res[i] == PackageManager.PERMISSION_GRANTED;
            if (!granted[i]) all_granted = false;
        }

        return all_granted;
    }

    public static void requestPermissions(int requestCode, String... permissions) {
        if (checkIsL()) return;
        activity.requestPermissions(permissions, requestCode);
    }

    private static boolean checkIsL() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }
}
