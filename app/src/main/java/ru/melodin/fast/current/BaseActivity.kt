package ru.melodin.fast.current

import android.graphics.drawable.Drawable
import android.os.Bundle

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar

abstract class BaseActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.CURRENT_THEME)
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)
    }

    open fun applyStyles() {
        recreate()
    }

    fun drawable(@DrawableRes resId: Int): Drawable {
        return ContextCompat.getDrawable(this, resId)!!
    }

    @ColorInt
    fun color(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }

    fun string(@StringRes resId: Int): String {
        return getString(resId)
    }

    fun string(@StringRes resId: Int, vararg args: Any): String {
        return getString(resId, *args)
    }

    protected fun setToolbar(toolbar: FastToolbar) {

    }
}
