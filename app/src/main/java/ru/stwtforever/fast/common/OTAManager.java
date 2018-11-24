package ru.stwtforever.fast.common;

import android.app.*;
import android.content.*;
import android.widget.*;
import org.json.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.fragment.*;
import ru.stwtforever.fast.util.*;

public class OTAManager {

	public static final String DEFAULT_URL = "http://stwtforever.do.am/";
	public static final String VERSION_NAME = AppGlobal.app_version_name;

	public static boolean important;
	public static int build;
	public static String date, version, apk, changelog, name;

	public static void checkUpdate(final Activity context, final boolean withToast) {
		if (!(context instanceof Activity)) return;
		if (!Utils.hasConnection()) {
			Toast.makeText(context, context.getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
			return;
		}
		
		if (!Utils.getPrefs().getBoolean(FragmentSettings.KEY_ENABLE_OTA, false)) return;
		
		ThreadExecutor.execute(new AsyncCallback(context) {

				@Override
				public void ready() throws Exception {
					JSONObject root = JSONParser.parse(DEFAULT_URL + "fvk.json");
					if (root == null) return;

					JSONObject l = root.optJSONObject("last_build");
					if (l == null) return;

					version = l.optString("version");
					name = l.optString("name");
					build = l.optInt("build");
					apk = l.optString("apk");
					changelog = l.optString("changelog");
					important = l.optBoolean("important");

					date = root.optString("date");
				}

				@Override
				public void done() {
					if (VERSION_NAME.equals(version)) {
						if (withToast) {
							context.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										Toast.makeText(context, context.getString(R.string.no_updates), Toast.LENGTH_LONG).show();
									}
								});
							return;
						}
					} else {
						showUpdateDialog(context);
					}
				}

				@Override
				public void error(Exception e) {
					e.printStackTrace();
					AppGlobal.putException(e);
				}
				
		});
	}

	public static void checkUpdate(Activity context) {
		checkUpdate(context, false);
	}

	private static void showUpdateDialog(Activity context) {
		context
			.startActivity(new Intent(context, UpdateActivity.class)
						   .putExtra("version", version)
						   .putExtra("name", name)
						   .putExtra("build", build)
						   .putExtra("apk", apk)
						   .putExtra("changelog", changelog)
						   .putExtra("important", important)
						   .putExtra("date", date)
						   );
	}
}
