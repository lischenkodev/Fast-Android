package ru.melodin.fast.util;

import android.animation.Animator;
import android.os.Build;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;

import androidx.annotation.ColorInt;

import ru.melodin.fast.common.ThemeManager;

public class ViewUtil {

    public static void fadeView(View v, long duration, boolean show, Animator.AnimatorListener listener) {
        v.setAlpha(show ? 0 : 1);

        ViewPropertyAnimator animator = v.animate();
        animator.alpha(show ? 1 : 0);
        animator.setDuration(duration);
        if (listener != null)
            animator.setListener(listener);
        animator.start();
    }

    public static void fadeView(View v, boolean show) {
        fadeView(v, 200, show, null);
    }

    public static void fadeView(View v, boolean show, Animator.AnimatorListener listener) {
        fadeView(v, 200, show, listener);
    }

    public static void applyWindowStyles(Window window) {
        applyWindowStyles(window, ThemeManager.getPrimary());
    }

    private static void applyWindowStyles(Window window, @ColorInt int color) {
        boolean light = ColorUtil.isLight(color);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int light_sb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                window.getDecorView().setSystemUiVisibility(light ? light_sb : 0);
                window.setStatusBarColor(color);
                window.setNavigationBarColor(light ? ColorUtil.darkenColor(color) : color);
            } else {
                int light_nb = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                window.getDecorView().setSystemUiVisibility(light ? (light_sb | light_nb) : 0);
                window.setStatusBarColor(color);
                window.setNavigationBarColor(color);
            }

            return;
        }

        window.getDecorView().setSystemUiVisibility(0);
        window.setStatusBarColor(light ? ColorUtil.darkenColor(color) : color);
        window.setNavigationBarColor(window.getStatusBarColor());
    }

}
