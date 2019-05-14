package ru.melodin.fast.fragment

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ru.melodin.fast.R
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.PermissionManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.util.Util

class FragmentSettings : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private var darkTheme: Preference? = null
    private var hideTyping: Preference? = null
    private var template: Preference? = null
    private var about: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionManager.setActivity(activity)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        UserConfig.updateUser()
        setPreferencesFromResource(R.xml.prefs, rootKey)

        hideTyping = findPreference(KEY_HIDE_TYPING)
        template = findPreference(KEY_MESSAGE_TEMPLATE)
        darkTheme = findPreference(KEY_DARK_STYLE)
        about = findPreference(KEY_ABOUT)
        about!!.onPreferenceClickListener = this

        darkTheme!!.onPreferenceChangeListener = this

        val user = UserConfig.user ?: return
        val hideTypingSummary = String.format(getString(R.string.hide_typing_summary), user.name, user.surname.substring(0, 1) + ".")
        hideTyping!!.summary = hideTypingSummary
    }

    override fun onPreferenceChange(p: Preference, newVal: Any): Boolean {
        when (p.key) {
            KEY_DARK_STYLE -> switchTheme(newVal as Boolean)
        }
        return true
    }

    override fun onPreferenceClick(pref: Preference): Boolean {
        when (pref.key) {
            KEY_ABOUT -> Toast.makeText(context, String.format(getString(R.string.about_toast), AppGlobal.app_version_name, AppGlobal.app_version_code), Toast.LENGTH_LONG).show()
        }
        return true
    }

    private fun switchTheme(dark: Boolean) {
        ThemeManager.switchTheme(dark)
        Util.restart(activity, true)
    }

    companion object {
        const val KEY_NOT_READ_MESSAGES = "not_read"
        const val KEY_DARK_STYLE: String = "dark_style"
        const val KEY_MESSAGE_TEMPLATE = "template"
        const val KEY_ABOUT = "about"
        const val KEY_HIDE_TYPING = "hide_typing"
        const val KEY_SHOW_ERROR = "show_error"
        const val DEFAULT_TEMPLATE_VALUE = "¯\\_(ツ)_/¯"
    }
}
