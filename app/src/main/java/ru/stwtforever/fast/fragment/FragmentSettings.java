package ru.stwtforever.fast.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.TaskStackBuilder;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ru.stwtforever.fast.MainActivity;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.helper.PermissionHelper;
import ru.stwtforever.fast.util.ViewUtils;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    public FragmentSettings() {
    }

    private Preference dark_theme, hide_typing, template, about;

    public static final String KEY_DARK_STYLE = "dark_style";
    public static final String KEY_MESSAGE_TEMPLATE = "template";
    public static final String KEY_ABOUT = "about";
    public static final String KEY_HIDE_TYPING = "hide_typing";
    public static final String KEY_NOT_READ_MESSAGES = "not_read";

    public static final String DEFAULT_TEMPLATE_VALUE = "¯\\_(ツ)_/¯";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionHelper.init(getActivity());
    }

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        UserConfig.updateUser();
        setPreferencesFromResource(R.xml.prefs, rootKey);

        hide_typing = findPreference(KEY_HIDE_TYPING);
        template = findPreference(KEY_MESSAGE_TEMPLATE);
        dark_theme = findPreference(KEY_DARK_STYLE);
        about = findPreference(KEY_ABOUT);
        about.setOnPreferenceClickListener(this);

        dark_theme.setOnPreferenceChangeListener(this);

        VKUser user = UserConfig.user;
        if (user == null) return;
        String hide_typing_summary = String.format(getString(R.string.hide_typing_summary), user.name, user.surname.substring(0, 1) + ".");
        hide_typing.setSummary(hide_typing_summary);
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object newVal) {
        switch (p.getKey()) {
            case KEY_DARK_STYLE:
                switchTheme((boolean) newVal);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        switch (pref.getKey()) {
            case KEY_ABOUT:
                Toast.makeText(getContext(), String.format(getString(R.string.about_toast), AppGlobal.app_version_name, AppGlobal.app_version_code), Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    private void switchTheme(boolean dark) {
        ThemeManager.update(dark);
        ViewUtils.update();
        getActivity().finishAffinity();
        TaskStackBuilder.create(getActivity())
                .addNextIntent(new Intent(getActivity(), MainActivity.class))
                .addNextIntent(getActivity().getIntent()).startActivities();

        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
