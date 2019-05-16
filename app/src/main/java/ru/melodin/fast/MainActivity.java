package ru.melodin.fast;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.PermissionManager;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.current.BaseFragment;
import ru.melodin.fast.fragment.FragmentDialogs;
import ru.melodin.fast.fragment.FragmentFriends;
import ru.melodin.fast.fragment.FragmentNavDrawer;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.service.LongPollService;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;

public class MainActivity extends AppCompatActivity {

    private View bottom_toolbar;
    private LinearLayout tb_btn_switch;
    private ImageButton filter, menu, conversations, friends;

    private GradientDrawable background;

    private FragmentDialogs fd = new FragmentDialogs();
    private FragmentFriends ff = new FragmentFriends();

    private int selectedId = -1;
    private Fragment selectedFragment;

    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tb_messages:
                    if (selectedFragment == fd) return;
                    selectedFragment = fd;
                    conversations.setBackground(background);
                    conversations.getDrawable().setTint(Color.WHITE);
                    friends.getDrawable().setTint(ThemeManager.getAccent());
                    friends.setBackgroundColor(Color.TRANSPARENT);
                    break;
                case R.id.tb_friends:
                    if (selectedFragment == ff) return;
                    selectedFragment = ff;
                    friends.setBackground(background);
                    friends.getDrawable().setTint(Color.WHITE);
                    conversations.getDrawable().setTint(ThemeManager.getAccent());
                    conversations.setBackgroundColor(Color.TRANSPARENT);
                    break;
            }

            Fragment visibleFragment = getVisibleFragment();

            if (visibleFragment != selectedFragment)
                replaceFragment(selectedFragment);
            else {
                if (visibleFragment instanceof BaseFragment)
                    ((BaseFragment) visibleFragment).getRecyclerView().scrollToPosition(0);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PermissionManager.setActivity(this);
        EventBus.getDefault().register(this);
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        VKApi.config = UserConfig.restore();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bottom_toolbar = findViewById(R.id.toolbar);

        initToolbar();

        checkLogin();

        startService(new Intent(this, LongPollService.class));
        checkCrash();

        if (UserConfig.isLoggedIn()) {
            trackVisitor();
        }

        if (!PermissionManager.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionManager.requestPermissions(44, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceive(Object[] data) {
        if (ArrayUtil.isEmpty(data)) return;

        String key = (String) data[0];
        if (key.equals(ThemeManager.KEY_THEME_UPDATE))
            Util.restart(this, true);
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
            replaceFragment(fd);
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


    private void initToolbar() {
        filter = bottom_toolbar.findViewById(R.id.tb_filter);
        menu = bottom_toolbar.findViewById(R.id.tb_menu);

        findViewById(R.id.toolbar).setBackgroundColor(ThemeManager.getPrimary());

        filter.setEnabled(false);
        filter.getDrawable().setTint(Color.GRAY);

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentNavDrawer.display(getSupportFragmentManager(), selectedId);
            }
        });

        background = new GradientDrawable();
        background.setColor(ThemeManager.getAccent());
        background.setCornerRadius(200f);

        conversations = bottom_toolbar.findViewById(R.id.tb_messages);
        friends = bottom_toolbar.findViewById(R.id.tb_friends);

        conversations.setBackground(background);
        friends.setBackgroundColor(Color.TRANSPARENT);

        friends.setOnClickListener(click);
        conversations.setOnClickListener(click);

        tb_btn_switch = bottom_toolbar.findViewById(R.id.tb_icons_switcher);
        tb_btn_switch.setBackgroundResource(ThemeManager.isDark() ? R.drawable.tb_switcher_bg_dark : R.drawable.tb_switcher_bg);
    }

    public void itemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.groups:
                selectedId = R.id.groups;
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.left:
                showExitDialog();
                break;
        }
    }

    private Fragment getVisibleFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        if (ArrayUtil.isEmpty(fragments)) return null;

        for (Fragment fragment : fragments)
            if (fragment.isVisible()) return fragment;

        return null;
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
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    }

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.warning);
        adb.setMessage(R.string.exit_message);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserConfig.clear();
                startLoginActivity();
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
}
