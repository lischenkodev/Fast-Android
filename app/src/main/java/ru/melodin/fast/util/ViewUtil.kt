package ru.melodin.fast.util

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import ru.melodin.fast.common.AppGlobal


object ViewUtil {

    private val keyboard = AppGlobal.inputService

    fun showKeyboard(v: View) {
        v.requestFocus()
        keyboard.showSoftInput(v, 0)
    }

    fun hideKeyboard(v: View) {
        keyboard.hideSoftInputFromWindow(v.windowToken, 0)
    }

    fun snackbar(v: View, text: String): Snackbar {
        return Snackbar.make(v, text, Snackbar.LENGTH_SHORT)
    }

    fun snackbar(v: View, @StringRes text: Int): Snackbar {
        return Snackbar.make(v, text, Snackbar.LENGTH_SHORT)
    }
}
