package ru.melodin.fast.util;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.ColorInt;

import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;

public class ViewUtil {

    private static final InputMethodManager keyboard = (InputMethodManager) AppGlobal.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

    public static void fadeView(View v, long duration, boolean show, Runnable startAction, Runnable endAction) {
        v.setAlpha(show ? 0 : 1);

        ViewPropertyAnimator animator = v.animate();
        animator.alpha(show ? 1 : 0);
        animator.setDuration(duration);
        if (startAction != null)
            animator.withStartAction(startAction);
        if (endAction != null)
            animator.withEndAction(endAction);
        animator.start();
    }

    public static void fadeView(View v, boolean show) {
        fadeView(v, 200, show, null, null);
    }

    public static void fadeView(View v, boolean show, Runnable startAction, Runnable endAction) {
        fadeView(v, 200, show, startAction, endAction);
    }

    public static void showKeyboard(View v) {
        keyboard.showSoftInput(v, 0);
    }

    public static void hideKeyboard(View v) {
        keyboard.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void applyWindowStyles(Window window) {
        applyWindowStyles(window, ThemeManager.getPrimary());
    }

    public static void applyWindowStyles(Window window, @ColorInt int color) {
        boolean light = ColorUtil.isLight(color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            int light_sb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(light ? light_sb : 0);
            window.setStatusBarColor(color);
            window.setNavigationBarColor(light ? ColorUtil.darkenColor(color) : color);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int light_sb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            int light_nb = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(light ? (light_sb | light_nb) : 0);
            window.setStatusBarColor(color);
            window.setNavigationBarColor(color);
        } else {
            window.getDecorView().setSystemUiVisibility(0);
            window.setStatusBarColor(light ? ColorUtil.darkenColor(color) : color);
            window.setNavigationBarColor(window.getStatusBarColor());
        }
    }
}
