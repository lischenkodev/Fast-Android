package ru.stwtforever.fast;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.greenrobot.eventbus.*;
import ru.stwtforever.fast.adapter.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.cls.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.fragment.*;
import ru.stwtforever.fast.helper.*;
import ru.stwtforever.fast.service.*;
import ru.stwtforever.fast.util.*;

public class MainActivity extends AppCompatActivity {

	private View bottom_toolbar;

	private LinearLayout tb_btn_switch;
	private ImageButton messages, friends, filter, menu;

	private GradientDrawable bg;
	
	private SlidingDrawer drawer;
	private DrawerAdapter adapter;

	FragmentDialogs f_dialogs;
	FragmentFriends f_friends;
	
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
		
		initFragments();
		
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
				public void done() {}

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

	

	private void initFragments() {
		f_dialogs = new FragmentDialogs(getString(R.string.fragment_messages));
		f_friends = new FragmentFriends(getString(R.string.fragment_friends));
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
		
		final String[] titles = new String[]{getString(R.string.settings), getString(R.string.logout)};
		int[] icons = new int[] {R.drawable.md_settings, R.drawable.md_exit_to_app};
		final String[] tags = new String[]{"settings", "exit"};
		
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
		
		switch(tag) {
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

	@Subscribe (sticky = true) 
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

	private void replaceFragment(Fragment f) {
		List<Fragment> fragments = getSupportFragmentManager().getFragments();

		if (fragments == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, f).commit();
		} else {
			boolean added = false;
			final FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
			for (Fragment fr : fragments) {
				if (fr == f) {
					added = true;
					tr.show(fr);
				} else {
					tr.hide(fr);
				}
			}

			if (!added) {
				tr.add(R.id.fragment_container, f);
			}

			new Handler().post(new Runnable() {

					@Override
					public void run() {
						tr.commit();
					}
			});
			
			invalidateOptionsMenu();
		}
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
