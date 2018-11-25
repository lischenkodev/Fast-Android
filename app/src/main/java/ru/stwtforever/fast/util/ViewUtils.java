package ru.stwtforever.fast.util;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

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

    public static void fadeImage(ImageView imageView, long duration) {
        imageView.setAlpha(0f);
        imageView.animate().alpha(1f).setDuration(duration).start();
    }

    public static void fadeImage(ImageView imageView) {
        fadeImage(imageView, 200);
    }

    public static void applyWindowStyles(Activity a) {
        if (a == null) return;

        Window w = a.getWindow();

        boolean isBiggerM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

        int light_sb = isBiggerM ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0;
        int light_nb = isBiggerM ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0;

        w.setBackgroundDrawable(new ColorDrawable(primaryColor));

        if (ThemeManager.isDark()) {
            w.getDecorView().setSystemUiVisibility(0);
            w.setStatusBarColor(statusBarColor);
            w.setNavigationBarColor(statusBarColor);
            return;
        }


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            w.getDecorView().setSystemUiVisibility(0);
            w.setStatusBarColor(statusBarColor);
            w.setNavigationBarColor(statusBarColor);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                w.setStatusBarColor(Color.WHITE);
                w.setNavigationBarColor(Color.WHITE);
                w.getDecorView().setSystemUiVisibility(light_sb | light_nb);
            } else {
                w.setStatusBarColor(Color.WHITE);
                w.setNavigationBarColor(statusBarColor);
                w.getDecorView().setSystemUiVisibility(light_sb);
            }
        }
    }

    public static void applyToolbarStyles(Toolbar tb) {
        tb.setPopupTheme(popupTheme);
        tb.setBackgroundColor(primaryColor);

        tb.setTitleTextColor(mainColor);
        tb.setSubtitleTextColor(secondaryColor);

        if (tb.getOverflowIcon() != null) {
            tb.getOverflowIcon().setTint(mainColor);
        }

        if (tb.getNavigationIcon() != null) {
            tb.getNavigationIcon().setTint(mainColor);
        }
    }


    static @ColorInt
    int getColor() {
        return getColor(R.color.colorPrimary);
    }

    static @ColorInt
    int getColor(int resId) {
        return AppGlobal.context.getResources().getColor(resId);
    }

    ;
}
