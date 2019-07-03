package ru.melodin.fast.common;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.IntegerRes;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.Contract;

import ru.melodin.fast.R;
import ru.melodin.fast.fragment.FragmentSettings;

public class ThemeManager {

    public static final String KEY_THEME_UPDATE = "theme_update";
    private static boolean dark;
    @IntegerRes
    private static int currentTheme, popupTheme, loginTheme, alertTheme;
    @ColorInt
    private static int primary, primaryInverse, primaryDark, accent, main, secondary, background;

    public static void switchTheme(boolean dark) {
        AppGlobal.preferences.edit().putBoolean(FragmentSettings.KEY_DARK_STYLE, dark).apply();
        init();
        EventBus.getDefault().post(new Object[]{KEY_THEME_UPDATE});
    }

    public static void toggleTheme() {
        switchTheme(!isDark());
    }

    static void init() {
        dark = AppGlobal.preferences.getBoolean(FragmentSettings.KEY_DARK_STYLE, false);

        currentTheme = isDark() ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
        popupTheme = isDark() ? R.style.ThemeOverlay_AppCompat : R.style.ThemeOverlay_AppCompat_Light;
        loginTheme = isDark() ? R.style.AppTheme_Login_Dark : R.style.AppTheme_Login_Light;
        alertTheme = isDark() ? R.style.AlertDialog_Theme_Dark : R.style.AlertDialog_Theme_Light;

        primary = getColor(isDark() ? R.color.dark_primary : R.color.primary);
        primaryInverse = getColor(isDark() ? R.color.primary : R.color.dark_primary);
        primaryDark = getColor(isDark() ? R.color.dark_primary_dark : R.color.primary_dark);
        accent = getColor(isDark() ? R.color.dark_accent : R.color.accent);
        background = getColor(isDark() ? R.color.dark_background : R.color.background);
        main = isDark() ? Color.WHITE : Color.BLACK;
        secondary = isDark() ? Color.LTGRAY : Color.DKGRAY;

    }

    @Contract(pure = true)
    public static boolean isDark() {
        return dark;
    }

    @Contract(pure = true)
    public static int getPrimaryInverse() {
        return primaryInverse;
    }

    @Contract(pure = true)
    public static int getCurrentTheme() {
        return currentTheme;
    }

    @Contract(pure = true)
    public static int getLoginTheme() {
        return loginTheme;
    }

    @Contract(pure = true)
    public static int getPopupTheme() {
        return popupTheme;
    }

    @Contract(pure = true)
    public static int getAlertTheme() {
        return alertTheme;
    }

    @Contract(pure = true)
    public static int getPrimary() {
        return primary;
    }

    @Contract(pure = true)
    public static int getPrimaryDark() {
        return primaryDark;
    }

    @Contract(pure = true)
    public static int getAccent() {
        return accent;
    }

    @Contract(pure = true)
    public static int getMain() {
        return main;
    }

    @Contract(pure = true)
    public static int getSecondary() {
        return secondary;
    }

    @Contract(pure = true)
    public static int getBackground() {
        return background;
    }

    @ColorInt
    private static int getColor(@ColorRes int resId) {
        return ContextCompat.getColor(AppGlobal.getContext(), resId);
    }
}
