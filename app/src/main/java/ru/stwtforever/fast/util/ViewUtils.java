package ru.stwtforever.fast.util;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.common.ThemeManager;

public class ViewUtils {

    public static int statusBarColor = ThemeManager.isDark() ? 0xff212121 : 0xffcccccc;
    public static int popupTheme = ThemeManager.isDark() ? R.style.ThemeOverlay_AppCompat : R.style.ThemeOverlay_AppCompat_Light;
    public static int primaryColor = ThemeManager.isDark() ? 0xff303030 : Color.WHITE;
    public static int mainColor = ThemeManager.isDark() ? Color.WHITE : Color.BLACK;
    public static int secondaryColor = ThemeManager.isDark() ? Color.LTGRAY : Color.DKGRAY;

    static {
        update();
    }

    public static void update() {
        statusBarColor = ThemeManager.isDark() ? 0xff212121 : 0xffcccccc;
        popupTheme = ThemeManager.isDark() ? R.style.ThemeOverlay_AppCompat : R.style.ThemeOverlay_AppCompat_Light;
        primaryColor = ThemeManager.isDark() ? 0xff303030 : Color.WHITE;
        mainColor = ThemeManager.isDark() ? Color.WHITE : Color.BLACK;
        secondaryColor = ThemeManager.isDark() ? Color.LTGRAY : Color.DKGRAY;
    }

    public static void fadeView(View v, long duration) {
        v.setAlpha(0);
        v.animate().alpha(1).setDuration(duration).start();
    }

    public static void fadeView(View v) {
        fadeView(v, 200);
    }

    static @ColorInt
    int getColor() {
        return getColor(R.color.colorPrimary);
    }

    private static @ColorInt
    int getColor(@ColorRes int resId) {
        return AppGlobal.context.getResources().getColor(resId);
    }
}
