package ru.stwtforever.fast.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import ru.stwtforever.fast.MainActivity;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.common.OTAManager;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.helper.DialogHelper;
import ru.stwtforever.fast.helper.PermissionHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.FException;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, ListView.OnItemClickListener {

    public FragmentSettings() {
    }

    private Preference dark_theme, ota, hide_typing, template, about, updates;

    public static final String KEY_DARK_STYLE = "dark_style";
    public static final String KEY_MESSAGE_TEMPLATE = "template";
    public static final String KEY_ABOUT = "about";
    public static final String KEY_UPDATES = "check_updates";
    public static final String KEY_HIDE_TYPING = "hide_typing";
    public static final String KEY_ENABLE_OTA = "ota";

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
        updates = findPreference(KEY_UPDATES);
        ota = findPreference(KEY_ENABLE_OTA);

        updates.setOnPreferenceClickListener(this);
        about.setOnPreferenceClickListener(this);

        ota.setOnPreferenceChangeListener(this);
        dark_theme.setOnPreferenceChangeListener(this);

        updates.setVisible(Utils.getPrefs().getBoolean(KEY_ENABLE_OTA, false));

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
            case KEY_ENABLE_OTA:
                updates.setVisible((boolean) newVal);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        switch (pref.getKey()) {
            case KEY_UPDATES:
                checkUpdates();
                break;
            case KEY_ABOUT:
                Toast.makeText(getContext(), String.format(getString(R.string.about_toast), AppGlobal.app_version_name, AppGlobal.app_version_code), Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> a, View v, final int pos, long p4) {
        final FException e = (FException) a.getItemAtPosition(pos);

        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(Utils.dateFullFormatter.format(new Date(e.time)));
        adb.setMessage(e.exception);

        adb.setPositiveButton(android.R.string.ok, null);
        adb.setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                Utils.copyText(e.exception);
            }
        });

        DialogHelper.create(adb).show();
    }

    private void checkUpdates() {
        OTAManager.checkUpdate(getActivity(), true);
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
