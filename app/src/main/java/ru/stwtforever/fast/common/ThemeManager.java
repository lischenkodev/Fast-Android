package ru.stwtforever.fast.common;

import android.*;
import android.graphics.*;
import android.support.annotation.*;
import ru.stwtforever.fast.fragment.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.R;

public class ThemeManager {
	
	private static boolean dark = false;
	
	private static int current_theme = -1;
	
	public static final @ColorInt int dark_bg = 0xff222222;
	public static final @ColorInt int light_bg = Color.WHITE;
	
	public static void init() {
		getData();
	}
	
	public static void update(boolean isDark) {
		dark = isDark;
		getTheme();
	}
	
	private static void getData() {
		dark = Utils.getPrefs().getBoolean(FragmentSettings.KEY_DARK_STYLE, false);
		getTheme();
	}
	
	private static void getTheme() {
		if (dark) {
			current_theme = R.style.DarkTheme;
		} else {
			current_theme = R.style.LightTheme;
		}
	}
	
	public static boolean isDark() {
		return dark;
	}
	
	public static int getCurrentTheme() {
		return current_theme;
	}
	
	public static @ColorInt int getBackground() {
		return isDark() ? dark_bg : light_bg;
	}
	
	public static @ColorInt int getPrimary() {
		return getColor(isDark() ? R.color.colorDarkPrimary : R.color.colorPrimary);
	}
	
	public static @ColorInt int getAccent() {
		return getColor(isDark() ? R.color.colorAccentDark : R.color.colorAccent);
	}
	
	static @ColorInt int getColor(int i) {
		return AppGlobal.context.getResources().getColor(i);
	}
}
