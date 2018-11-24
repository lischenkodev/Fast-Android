package ru.stwtforever.fast.util;

import android.*;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.support.annotation.*;
import android.support.v7.widget.*;
import android.text.*;
import android.view.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.R;
import android.graphics.drawable.*;

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
	
	public static void applyWindowStyles(Activity a) {
		if (a == null) return;

		Window w = a.getWindow();

		int light_sb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		int light_nb = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

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


	static @ColorInt int getColor() {
		return getColor(R.color.colorPrimary);
	}

	static @ColorInt int getColor(int resId) {
		return AppGlobal.context.getResources().getColor(resId);
	}
	;
}
