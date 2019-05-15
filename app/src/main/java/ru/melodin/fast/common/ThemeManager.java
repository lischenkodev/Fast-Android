package ru.melodin.fast.common;

import android.graphics.Color;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.ColorInt;
import androidx.annotation.IntegerRes;
import ru.melodin.fast.R;
import ru.melodin.fast.fragment.FragmentSettings;

public class ThemeManager {

    public static final String KEY_THEME_UPDATE = "theme_update";
    private static boolean dark;
    @IntegerRes
    private static int currentTheme, popupTheme;
    @ColorInt
    private static int primary, primaryDark, accent, main, secondary, background;

    public static void switchTheme(boolean dark) {
        AppGlobal.preferences.edit().putBoolean(FragmentSettings.KEY_DARK_STYLE, dark).apply();
        init();
        EventBus.getDefault().post(new Object[]{KEY_THEME_UPDATE});
    }

    static void init() {
        dark = AppGlobal.preferences.getBoolean(FragmentSettings.KEY_DARK_STYLE, false);

        currentTheme = isDark() ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
        popupTheme = isDark() ? R.style.ThemeOverlay_AppCompat : R.style.ThemeOverlay_AppCompat_Light;

        primary = getColor(isDark() ? R.color.dark_primary : R.color.primary);
        primaryDark = getColor(isDark() ? R.color.dark_primary_dark : R.color.primary_dark);
        accent = getColor(isDark() ? R.color.dark_accent : R.color.accent);
        background = getColor(isDark() ? R.color.dark_background : R.color.background);
        main = isDark() ? Color.WHITE : Color.BLACK;
        secondary = isDark() ? Color.LTGRAY : Color.DKGRAY;
    }

    public static boolean isDark() {
        return dark;
    }

    public static int getCurrentTheme() {
        return currentTheme;
    }

    public static int getPopupTheme() {
        return popupTheme;
    }

    public static int getPrimary() {
        return primary;
    }

    public static int getPrimaryDark() {
        return primaryDark;
    }

    public static int getAccent() {
        return accent;
    }

    public static int getMain() {
        return main;
    }

    public static int getSecondary() {
        return secondary;
    }

    public static int getBackground() {
        return background;
    }

    @ColorInt
    private static int getColor(int i) {
        return AppGlobal.context.getResources().getColor(i);
    }
}
