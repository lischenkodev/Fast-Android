package ru.stwtforever.fast.fragment;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v7.preference.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.helper.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import java.util.*;

import ru.stwtforever.fast.R;

import static ru.stwtforever.fast.common.AppGlobal.*;
import ru.stwtforever.fast.adapter.*;
import android.support.v7.app.*;
import org.greenrobot.eventbus.*;

public class FragmentSettings extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, ListView.OnItemClickListener {

	public FragmentSettings() {}
	
	private ExceptionAdapter adapter;
	
	private ArrayList<FException> excs;
	
	private Preference exceptions, exception, dark_theme, ota, clean_cache, clean_msg_cache, hide_typing, error, template, about, updates;
	
	public static final String KEY_EXCEPTIONS = "exceptions";
	public static final String KEY_CLEAN_CACHE = "clean_all_cache";
	public static final String KEY_CLEAN_MESSAGES_CACHE = "clean_messages_cache";
	public static final String KEY_DARK_STYLE = "dark_style";
	public static final String KEY_DOMAINS = "domains";
	public static final String KEY_DOMAIN_API = "domain_api";
	public static final String KEY_DOMAIN_OAUTH = "domain_oauth";
	public static final String KEY_MESSAGE_TEMPLATE = "template";
	public static final String KEY_ABOUT = "about";
	public static final String KEY_UPDATES = "check_updates";
	public static final String KEY_MAKE_ERROR = "do_error";
	public static final String KEY_MAKE_EXCEPTION = "do_exception";
	public static final String KEY_HIDE_TYPING = "hide_typing";
	public static final String KEY_ENABLE_OTA = "ota";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PermissionHelper.init(getActivity());
	}

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		UserConfig.updateUser();
        setPreferencesFromResource(R.xml.prefs, rootKey);
		
		exception = findPreference(KEY_MAKE_EXCEPTION);
		exceptions = findPreference(KEY_EXCEPTIONS);
		clean_msg_cache = findPreference(KEY_CLEAN_MESSAGES_CACHE);
		clean_cache = findPreference(KEY_CLEAN_CACHE);
		hide_typing = findPreference(KEY_HIDE_TYPING);
		error = findPreference(KEY_MAKE_ERROR);
		template = findPreference(KEY_MESSAGE_TEMPLATE);
		dark_theme = findPreference(KEY_DARK_STYLE);
		about = findPreference(KEY_ABOUT);
		updates = findPreference(KEY_UPDATES);
		ota = findPreference(KEY_ENABLE_OTA);
		
		clean_msg_cache.setOnPreferenceClickListener(this);
		clean_cache.setOnPreferenceClickListener(this);
		error.setOnPreferenceClickListener(this);
		exception.setOnPreferenceClickListener(this);
		updates.setOnPreferenceClickListener(this);
		about.setOnPreferenceClickListener(this);
		exceptions.setOnPreferenceClickListener(this);

		ota.setOnPreferenceChangeListener(this);
		dark_theme.setOnPreferenceChangeListener(this);
		
		clean_cache.setEnabled(false);
		clean_msg_cache.setEnabled(false);
		
		updates.setVisible(Utils.getPrefs().getBoolean(KEY_ENABLE_OTA, false));
		
		setExceptionsVisible();
		
		VKUser user = UserConfig.user;
		if (user == null) return;
		String hide_typing_summary = String.format(getString(R.string.hide_typing_summary), user.name, user.surname.substring(0, 1) + ".");
		hide_typing.setSummary(hide_typing_summary);
    }

	private void setExceptionsVisible() {
		excs = CacheStorage.getExceptions();

		boolean hasExceptions = !ArrayUtil.isEmpty(excs);
		exceptions.setVisible(hasExceptions);
	}

	@Override
	public boolean onPreferenceChange(Preference p, Object newVal) {
		switch (p.getKey()) {
			case KEY_DARK_STYLE:
				switchTheme(newVal);
				break;
			case KEY_ENABLE_OTA:
				updates.setVisible(newVal);
				break;
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference pref) {
		switch (pref.getKey()) {
			case KEY_EXCEPTIONS:
				showExceptionsDialog();
				break;
			case KEY_CLEAN_CACHE:
				//dbHelper.dropTables(database);
				break;
			case KEY_CLEAN_MESSAGES_CACHE:
				//dbHelper.dropMessagesTable(database);
				break;
			case KEY_MAKE_ERROR:
				makeError();
				break;
			case KEY_MAKE_EXCEPTION:
				makeException();
				setExceptionsVisible();
				break;
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
	
	private void showExceptionsDialog() {
		ListView lv = new ListView(getActivity());
		lv.setDividerHeight(-1);
		lv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		lv.setOnItemClickListener(this);
		
		adapter = new ExceptionAdapter(getActivity(), excs);
		lv.setAdapter(adapter);
		
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		adb.setTitle(R.string.exceptions);
		adb.setView(lv);
		adb.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface p1, int p2) {
					CacheStorage.delete(DBHelper.EXCEPTIONS_TABLE);
					setExceptionsVisible();
				}
			
		});
		adb.setPositiveButton(android.R.string.ok, null);
		
		DialogHelper.create(adb).show();
	}
	
	private void makeError() {
		String s = getString(R.string.custom_error);
		Integer.valueOf(s);
	}
	
	private void makeException() {
		ThreadExecutor.execute(new AsyncCallback(getActivity()) {

				@Override
				public void ready() throws Exception {
					String sm = getString(R.string.custom_exception);
					Integer.valueOf(sm);
				}

				@Override
				public void done() {
					setExceptionsVisible();
				}

				@Override
				public void error(Exception e) {
					setExceptionsVisible();
				}
			
		});
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
