package ru.stwtforever.fast;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import ru.stwtforever.fast.adapter.DrawerAdapter;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.cls.BaseFragment;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.db.DBHelper;
import ru.stwtforever.fast.db.MemoryCache;
import ru.stwtforever.fast.fragment.FragmentDialogs;
import ru.stwtforever.fast.fragment.FragmentFriends;
import ru.stwtforever.fast.fragment.FragmentGroups;
import ru.stwtforever.fast.helper.DialogHelper;
import ru.stwtforever.fast.helper.PermissionHelper;
import ru.stwtforever.fast.service.LongPollService;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class MainActivity extends AppCompatActivity {

    private View bottom_toolbar;

    private LinearLayout tb_btn_switch;
    private ImageButton messages, friends, filter, menu;

    private GradientDrawable bg;

    private SlidingDrawer drawer;
    private DrawerAdapter adapter;

    private FragmentDialogs f_dialogs = new FragmentDialogs();
    private FragmentFriends f_friends = new FragmentFriends();
    private FragmentGroups f_groups = new FragmentGroups();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PermissionHelper.init(this);
        ViewUtils.applyWindowStyles(this);
        EventBus.getDefault().register(this);
        setTheme(ThemeManager.getCurrentTheme());

        VKApi.config = UserConfig.restore();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bottom_toolbar = findViewById(R.id.toolbar);

        drawer = findViewById(R.id.drawer);
        initToolbar();

        drawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {

            @Override
            public void onDrawerClosed() {
                drawer.setVisibility(View.GONE);
            }

        });

        drawer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                drawer.animateClose();
            }
        });

        checkLogin();

        startService(new Intent(this, LongPollService.class));
        checkCrash();

        if (UserConfig.isLoggedIn()) {
            trackVisitor();
        }
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
            replaceFragment(f_dialogs);
        }
    }

    private void trackVisitor() {
        ThreadExecutor.execute(new AsyncCallback(this) {

            int i;

            @Override
            public void ready() throws Exception {
                i = VKApi.stats().trackVisitor().execute(Integer.class).get(0);
            }

            @Override
            public void done() {
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
            }

        });
    }

    private void checkCrash() {
        if (Utils.getPrefs().getBoolean("isCrashed", false)) {
            final String trace = Utils.getPrefs().getString("crashLog", "");
            Utils.getPrefs().edit().putBoolean("isCrashed", false).putString("crashLog", "").apply();


            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.warning);

            String message = getString(R.string.app_crashed);

            adb.setMessage(message);
            adb.setPositiveButton(android.R.string.ok, null);
            adb.setNeutralButton(R.string.show_error, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface p1, int p2) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                    adb.setTitle(R.string.error_log);
                    adb.setMessage(trace);
                    adb.setPositiveButton(android.R.string.ok, null);
                    adb.setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface p1, int p2) {
                            Utils.copyText(trace);
                        }
                    });
                    DialogHelper.create(adb).show();
                }

            });
            AlertDialog alert = DialogHelper.create(adb);
            alert.show();
        }
    }

    private void initToolbar() {
        filter = bottom_toolbar.findViewById(R.id.tb_filter);
        menu = bottom_toolbar.findViewById(R.id.tb_menu);

        filter.setEnabled(false);
        filter.getDrawable().setTint(Color.GRAY);

        menu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                drawer.setVisibility(View.VISIBLE);
                drawer.animateToggle();
            }

        });

        bg = new GradientDrawable();
        bg.setColor(ThemeManager.getAccent());
        bg.setCornerRadius(200);

        messages = bottom_toolbar.findViewById(R.id.tb_messages);
        friends = bottom_toolbar.findViewById(R.id.tb_friends);

        messages.setBackground(bg);
        friends.setBackgroundColor(Color.TRANSPARENT);

        friends.setOnClickListener(click);
        messages.setOnClickListener(click);

        tb_btn_switch = bottom_toolbar.findViewById(R.id.tb_icons_switcher);
        tb_btn_switch.setBackgroundResource(ThemeManager.isDark() ? R.drawable.tb_switcher_bg_dark : R.drawable.tb_switcher_bg);

        ListView list = findViewById(R.id.drawer_list);

        final String[] titles = new String[]{getString(R.string.groups), getString(R.string.settings), getString(R.string.logout)};
        int[] icons = new int[]{R.drawable.ic_people_outline_black_24dp, R.drawable.md_settings, R.drawable.md_exit_to_app};
        final String[] tags = new String[]{"groups", "settings", "exit"};

        ArrayList<Object[]> items = new ArrayList<>();

        for (int i = 0; i < titles.length; i++) {
            items.add(new Object[]{titles[i], icons[i], false, tags[i]});
        }

        adapter = new DrawerAdapter(this, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> lv, View v, int position, long p4) {
                boolean exitOrSettings = (tags[position].equals("settings") || tags[position].equals("exit"));

                if (!exitOrSettings)
                    adapter.setHover(true, position);

                itemClick(position);

                if (!tags[position].equals("settings")) {
                    drawer.animateClose();
                } else {
                    drawer.close();
                }
            }
        });
    }

    private void itemClick(int position) {
        String tag = (String) adapter.getValues().get(position)[3];

        switch (tag) {
            case "groups":
                //replaceFragment(f_groups);
                break;
            case "settings":
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case "exit":
                showExitDialog();
                break;
        }
    }

    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (adapter == null) return;
            adapter.clearHover();

            Fragment f = null;

            switch (v.getId()) {
                case R.id.tb_messages:
                    f = f_dialogs;
                    messages.setBackground(bg);
                    messages.getDrawable().setTint(Color.WHITE);
                    friends.getDrawable().setTint(ThemeManager.getAccent());
                    friends.setBackgroundColor(Color.TRANSPARENT);

                    replaceFragment(f_dialogs);
                    break;
                case R.id.tb_friends:
                    f = f_friends;
                    friends.getDrawable().setTint(Color.WHITE);
                    messages.getDrawable().setTint(ThemeManager.getAccent());
                    friends.setBackground(bg);
                    messages.setBackgroundColor(Color.TRANSPARENT);

                    replaceFragment(f_friends);
                    break;
            }

            if (getVisibleFragment() == f) {
                ((BaseFragment) f).getList().scrollToPosition(0);
            }
        }
    };

    @Subscribe(sticky = true)
    public void onUpdateUser(VKUser u) {
        return;
    }

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.warning);
        adb.setMessage(R.string.exit_message);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                UserConfig.clear();
                startLoginActivity();
            }
        });
        adb.setNegativeButton(R.string.no, null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        MemoryCache.clear();
        DBHelper.getInstance().close();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction tr = getSupportFragmentManager().beginTransaction();

        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        if (ArrayUtil.isEmpty(fragments)) {
            tr.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        } else {
            boolean exists = false;

            for (Fragment f : fragments) {
                if (f.getClass().getSimpleName().equals(fragment.getClass().getSimpleName())) {
                    tr.show(f);
                    exists = true;
                } else {
                    tr.hide(f);
                }
            }

            if (!exists) {
                tr.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
            }
        }

        tr.commit();
    }

    private Fragment getVisibleFragment() {
        List<Fragment> frs = getSupportFragmentManager().getFragments();

        if (ArrayUtil.isEmpty(frs)) return null;

        Fragment f = null;

        for (Fragment fr : frs) {
            if (fr.isVisible()) {
                f = fr;
            }
        }

        return f;
    }
}
