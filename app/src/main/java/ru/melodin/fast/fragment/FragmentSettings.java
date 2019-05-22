package ru.melodin.fast.fragment;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.adapter.UserAdapter;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    public static final String KEY_NOT_READ_MESSAGES = "not_read";
    public static final String KEY_DARK_STYLE = "dark_theme";
    public static final String KEY_MESSAGE_TEMPLATE = "template";
    public static final String KEY_HIDE_TYPING = "hide_typing";
    public static final String KEY_SHOW_ERROR = "show_error";
    public static final String DEFAULT_TEMPLATE_VALUE = "¯\\_(ツ)_/¯";
    private static final String KEY_ABOUT = "about";
    public static final String KEY_MESSAGES_CLEAR_CACHE = "clear_messages_cache";
    private static final String KEY_SHOW_CACHED_USERS = "show_cached_users";

    private ImageView avatar;
    private TextView name;

    private Drawable placeholder;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatar = view.findViewById(R.id.user_avatar);
        name = view.findViewById(R.id.user_name);

        placeholder = ContextCompat.getDrawable(getContext(), R.drawable.ic_avatar);

        getUser();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onReceive(Object[] data) {
        String key = (String) data[0];

        if (key.equals("update_user")) {
            int userId = (int) data[1];
            if (userId != UserConfig.userId) return;
            getUser();
        }
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void getUser() {
        VKUser user = MemoryCache.getUser(UserConfig.userId);
        if (user == null) user = VKUser.EMPTY;

        name.setText(user.toString());

        if (TextUtils.isEmpty(user.photo_200)) {
            avatar.setImageResource(R.drawable.ic_avatar);
        } else {
            Picasso.get().load(user.photo_200).priority(Picasso.Priority.HIGH).placeholder(placeholder).into(avatar);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs, rootKey);

        Preference hideTyping = findPreference(KEY_HIDE_TYPING);
        Preference darkTheme = findPreference(KEY_DARK_STYLE);

        findPreference(KEY_ABOUT).setOnPreferenceClickListener(this);
        findPreference(KEY_MESSAGES_CLEAR_CACHE).setOnPreferenceClickListener(this);
        findPreference(KEY_SHOW_CACHED_USERS).setOnPreferenceClickListener(this);

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
            case KEY_MESSAGES_CLEAR_CACHE:
                showConfirmClearCacheDialog(false);
                break;
            case KEY_SHOW_CACHED_USERS:
                showCachedUsers();
                break;
        }
        return true;
    }

    private void showCachedUsers() {
        View v = getLayoutInflater().inflate(R.layout.recycler_list, null, false);

        v.findViewById(R.id.refresh).setEnabled(false);
        v.findViewById(R.id.no_items_layout).setVisibility(View.GONE);
        RecyclerView list = v.findViewById(R.id.list);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        ArrayList<VKUser> users = CacheStorage.getUsers();
        if (ArrayUtil.isEmpty(users)) {
            Toast.makeText(getActivity(), R.string.no_data, Toast.LENGTH_SHORT).show();
            return;
        }

        UserAdapter adapter = new UserAdapter(getContext(), users);
        list.setAdapter(adapter);

        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
        adb.setTitle(R.string.cached_users);

        adb.setView(v);
        adb.setPositiveButton(android.R.string.ok, null);
        adb.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                showConfirmClearCacheDialog(true);
            }
        });
        adb.show();
    }

    private void showConfirmClearCacheDialog(final boolean users) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirmation)
                .setMessage(R.string.clear_cache_confirm)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (users) {
                            DatabaseHelper.getInstance().dropUsersTable(AppGlobal.database);
                        } else {
                            DatabaseHelper.getInstance().dropMessagesTable(AppGlobal.database);
                            EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGES_CLEAR_CACHE});
                        }

                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
