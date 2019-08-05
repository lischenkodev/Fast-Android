package ru.melodin.fast.common

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import org.greenrobot.eventbus.EventBus
import ru.melodin.fast.R
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.Keys

class ThemeManager {

    companion object {

        @ColorInt
        private fun getColor(@ColorRes resId: Int): Int {
            return AppGlobal.res.getColor(resId)
        }

        internal fun init() {
            isDark = AppGlobal.preferences.getBoolean(FragmentSettings.KEY_DARK_STYLE, false)

            currentTheme = if (isDark) R.style.AppTheme_Dark else R.style.AppTheme_Light
            popupTheme =
                if (isDark) R.style.ThemeOverlay_AppCompat else R.style.ThemeOverlay_AppCompat_Light
            loginTheme = if (isDark) R.style.AppTheme_Login_Dark else R.style.AppTheme_Login_Light
            alertTheme =
                if (isDark) R.style.AlertDialog_Theme_Dark else R.style.AlertDialog_Theme_Light

            primary = getColor(if (isDark) R.color.dark_primary else R.color.primary)
            primaryInverse = getColor(if (isDark) R.color.primary else R.color.dark_primary)
            primaryDark = getColor(if (isDark) R.color.dark_primary_dark else R.color.primary_dark)
            accent = getColor(if (isDark) R.color.dark_accent else R.color.accent)
            background = getColor(if (isDark) R.color.dark_background else R.color.background)
            main = if (isDark) Color.WHITE else Color.BLACK
            secondary = if (isDark) Color.LTGRAY else Color.DKGRAY
        }

        fun switchTheme(dark: Boolean) {
            AppGlobal.preferences.edit().putBoolean(FragmentSettings.KEY_DARK_STYLE, dark).apply()
            init()
            EventBus.getDefault().post(arrayOf<Any>(Keys.THEME_UPDATE))
        }

        fun toggleTheme() {
            switchTheme(!isDark)
        }

        var isDark: Boolean = false

        @IntegerRes
        var currentTheme: Int = 0
            private set

        @IntegerRes
        var popupTheme: Int = 0
            private set

        @IntegerRes
        var loginTheme: Int = 0
            private set

        @IntegerRes
        var alertTheme: Int = 0
            private set

        @ColorInt
        var primary: Int = 0
            private set

        @ColorInt
        var primaryInverse: Int = 0
            private set

        @ColorInt
        var primaryDark: Int = 0
            private set

        @ColorInt
        var accent: Int = 0
            private set

        @ColorInt
        var main: Int = 0
            private set
        @ColorInt
        var secondary: Int = 0
            private set

        @ColorInt
        var background: Int = 0
            private set

    }


}
