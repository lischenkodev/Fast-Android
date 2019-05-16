package ru.melodin.fast.fragment;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ru.melodin.fast.R;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.util.Util;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    public static final String KEY_NOT_READ_MESSAGES = "not_read";
    public static final String KEY_DARK_STYLE = "dark_style";
    public static final String KEY_MESSAGE_TEMPLATE = "template";
    public static final String KEY_HIDE_TYPING = "hide_typing";
    public static final String KEY_SHOW_ERROR = "show_error";
    public static final String DEFAULT_TEMPLATE_VALUE = "¯\\_(ツ)_/¯";
    private static final String KEY_ABOUT = "about";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        Preference hideTyping = findPreference(KEY_HIDE_TYPING);
        Preference darkTheme = findPreference(KEY_DARK_STYLE);

        findPreference(KEY_ABOUT).setOnPreferenceClickListener(this);

        darkTheme.setOnPreferenceChangeListener(this);

        VKUser user = UserConfig.user;
        if (user == null) return;
        String hideTypingSummary = String.format(getString(R.string.hide_typing_summary), user.name, user.surname.substring(0, 1) + ".");
        hideTyping.setSummary(hideTypingSummary);
    }


    private void switchTheme(boolean dark) {
        ThemeManager.switchTheme(dark);
        Util.restart(getActivity(), true);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_DARK_STYLE:
                switchTheme((boolean) newValue);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case KEY_ABOUT:
                Toast.makeText(getContext(), String.format(getString(R.string.about_toast), AppGlobal.app_version_name, AppGlobal.app_version_code), Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }
}
