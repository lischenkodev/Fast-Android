package ru.melodin.fast.common

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import org.greenrobot.eventbus.EventBus
import ru.melodin.fast.R
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.Keys

object ThemeManager {

    @ColorInt
    private fun getColor(@ColorRes resId: Int): Int {
        return AppGlobal.res.getColor(resId)
    }

    internal fun init() {
        IS_DARK = AppGlobal.preferences.getBoolean(FragmentSettings.KEY_DARK_STYLE, true)

        CURRENT_THEME = if (IS_DARK) R.style.AppTheme_Dark else R.style.AppTheme_Light
        POPUP_THEME =
            if (IS_DARK) R.style.ThemeOverlay_AppCompat else R.style.ThemeOverlay_AppCompat_Light
        LOGIN_THEME = if (IS_DARK) R.style.AppTheme_Login_Dark else R.style.AppTheme_Login_Light
        ALERT_THEME =
            if (IS_DARK) R.style.AlertDialog_Theme_Dark else R.style.AlertDialog_Theme_Light

        PRIMARY = getColor(if (IS_DARK) R.color.dark_primary else R.color.primary)
        PRIMARY_INVERSE = getColor(if (IS_DARK) R.color.primary else R.color.dark_primary)
        PRIMARY_DARK = getColor(if (IS_DARK) R.color.dark_primary_dark else R.color.primary_dark)
        ACCENT = getColor(if (IS_DARK) R.color.dark_accent else R.color.accent)
        BACKGROUND = getColor(if (IS_DARK) R.color.dark_background else R.color.background)
        MAIN = if (IS_DARK) Color.WHITE else Color.BLACK
        SECONDARY = if (IS_DARK) Color.LTGRAY else Color.DKGRAY
    }

    fun switchTheme(dark: Boolean) {
        AppGlobal.preferences.edit().putBoolean(FragmentSettings.KEY_DARK_STYLE, dark).apply()
        init()
        EventBus.getDefault().post(arrayOf<Any>(Keys.THEME_UPDATE))
    }

    fun toggleTheme() {
        switchTheme(!IS_DARK)
    }

    var IS_DARK = false

    var CURRENT_THEME = 0
    var POPUP_THEME = 0
    var LOGIN_THEME = 0
    var ALERT_THEME = 0

    var PRIMARY = 0
    var PRIMARY_INVERSE = 0
    var PRIMARY_DARK = 0
    var ACCENT = 0
    var MAIN = 0
    var SECONDARY = 0
    var BACKGROUND = 0

}
