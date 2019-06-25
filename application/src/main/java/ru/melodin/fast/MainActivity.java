package ru.melodin.fast;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.PermissionManager;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.current.BaseFragment;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.fragment.FragmentConversations;
import ru.melodin.fast.fragment.FragmentFriends;
import ru.melodin.fast.fragment.FragmentItems;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.service.LongPollService;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener {

    private FragmentConversations fragmentConversations = new FragmentConversations();
    private FragmentFriends fragmentFriends = new FragmentFriends();
    private FragmentItems fragmentItems = new FragmentItems();

    private BottomNavigationView navigationView;

    private int selectedId = -1;
    private BaseFragment selectedFragment;

    private Intent longPollIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PermissionManager.setActivity(this);
        ViewUtil.applyWindowStyles(getWindow(), ThemeManager.getPrimaryDark());
        setTheme(ThemeManager.getCurrentTheme());
        VKApi.config = UserConfig.restore();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        navigationView = findViewById(R.id.bottom_navigation_view);
        navigationView.setSelectedItemId(R.id.conversations);
        navigationView.setOnNavigationItemSelectedListener(this);
        navigationView.setOnNavigationItemReselectedListener(this);

        checkLogin();
        checkCrash();

        if (UserConfig.isLoggedIn()) {
            trackVisitor();
        }

        if (!PermissionManager.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionManager.requestPermissions(44, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        EventBus.getDefault().register(this);
    }

    private void loadUser() {
        ThreadExecutor.execute(new AsyncCallback(this) {
            VKUser user;

            @Override
            public void ready() throws Exception {
                user = VKApi.users().get().fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, user);
                EventBus.getDefault().postSticky(new Object[]{"update_user", UserConfig.userId});
            }

            @Override
            public void error(Exception e) {
                Log.e("Error get user", Log.getStackTraceString(e));
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceive(Object[] data) {
        if (ArrayUtil.isEmpty(data)) return;

        String key = (String) data[0];
        if (key.equals(ThemeManager.KEY_THEME_UPDATE))
            Util.restart(this, false);
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void checkLogin() {
        UserConfig.restore();
        if (!UserConfig.isLoggedIn()) {
            startLoginActivity();
        } else {
            loadUser();
            longPollIntent = new Intent(this, LongPollService.class);
            startService(longPollIntent);
            selectedFragment = fragmentConversations;
            replaceFragment(fragmentConversations);
        }
    }

    private void trackVisitor() {
        if (!Util.hasConnection()) return;
        ThreadExecutor.execute(new AsyncCallback(this) {


            @Override
            public void ready() throws Exception {
                VKApi.stats().trackVisitor().execute();
            }

            @Override
            public void done() {

            }

            @Override
            public void error(Exception e) {
                Log.e("Error track visitor", Log.getStackTraceString(e));
            }
        });
    }

    private void checkCrash() {
        if (AppGlobal.preferences.getBoolean("isCrashed", false)) {
            final String trace = AppGlobal.preferences.getString("crashLog", "");
            AppGlobal.preferences.edit().putBoolean("isCrashed", false).putString("crashLog", "").apply();


            if (!AppGlobal.preferences.getBoolean(FragmentSettings.KEY_SHOW_ERROR, false))
                return;

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.warning);

            adb.setMessage(R.string.app_crashed);
            adb.setPositiveButton(android.R.string.ok, null);
            adb.setNeutralButton(R.string.show_error, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                    adb.setTitle(R.string.error_log);
                    adb.setMessage(trace);
                    adb.setPositiveButton(android.R.string.ok, null);
                    adb.setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Util.copyText(trace);
                        }
                    });
                    adb.create().show();
                }
            });

            adb.create().show();
        }
    }

    public void itemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.groups:
                selectedId = R.id.groups;
                break;
            case R.id.menu:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.left:
                showExitDialog();
                break;
        }
    }

    private void replaceFragment(Fragment fragment) {
        if (fragment == null) return;

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        List<Fragment> fragments = manager.getFragments();
        List<String> classNames = new ArrayList<>(fragments.size());

        final int FRAGMENT_CONTAINER = R.id.fragment_container;

        if (ArrayUtil.isEmpty(fragments)) {
            transaction.add(FRAGMENT_CONTAINER, fragment, fragment.getClass().getSimpleName());
        } else {
            for (Fragment f : fragments) {
                transaction.hide(f);
                classNames.add(f.getClass().getSimpleName());
            }

            if (classNames.contains(fragment.getClass().getSimpleName())) {
                for (Fragment f : fragments)
                    if (f.getClass().getSimpleName().equals(fragment.getClass().getSimpleName())) {
                        transaction.show(f);
                        break;
                    }
            } else {
                transaction.add(FRAGMENT_CONTAINER, fragment, fragment.getClass().getSimpleName());
            }
        }
        transaction.commit();
    }

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.warning);
        adb.setMessage(R.string.exit_message);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startLoginActivity();
                stopService(longPollIntent);
                UserConfig.clear();
                DatabaseHelper.getInstance().dropTables(AppGlobal.database);
                DatabaseHelper.getInstance().onCreate(AppGlobal.database);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        selectedId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.conversations:
                selectedFragment = fragmentConversations;
                break;
            case R.id.friends:
                selectedFragment = fragmentFriends;
                break;
            case R.id.menu:
                selectedFragment = fragmentItems;
                break;
        }

        replaceFragment(selectedFragment);
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        if (selectedFragment != null)
            selectedFragment.scrollToTop();
    }
}
