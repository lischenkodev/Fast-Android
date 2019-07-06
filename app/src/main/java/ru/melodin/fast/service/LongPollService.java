package ru.melodin.fast.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKLongPollServer;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.net.HttpRequest;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Keys;
import ru.melodin.fast.util.Util;

public class LongPollService extends Service {

    public static final String TAG = "FastVK LongPoll";

    public boolean isRunning;

    private boolean needRefresh;


    private VKLongPollServer server;

    public LongPollService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        launchLongPoll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchLongPoll() {
        if (!isRunning) {
            isRunning = true;
        }
        LowThread updateThread = new LowThread(new MessageUpdater());
        updateThread.start();
    }

    private class MessageUpdater implements Runnable {
        @Override
        public void run() {
            if (!isRunning) {
                isRunning = true;
            }
            while (isRunning) {
                if (!Util.hasConnection()) {
                    needRefresh = true;
                    Log.e(TAG, "no connection");
                    sleep();
                    continue;
                }

                if (!UserConfig.isLoggedIn()) {
                    needRefresh = false;
                    sleep();
                    continue;
                }

                if (needRefresh) {
                    EventBus.getDefault().postSticky(new Object[]{Keys.CONNECTED});
                    needRefresh = false;
                }

                try {
                    if (server == null) {
                        server = VKApi.messages().getLongPollServer()
                                .execute(VKLongPollServer.class).get(0);
                    }

                    JSONObject response = getResponse(server);
                    if (response.has("failed")) {
                        Log.w(TAG, "Failed to get response from");
                        Thread.sleep(1_000);
                        server = null;
                        continue;
                    }

                    long tsResponse = response.optLong("ts");
                    JSONArray updates = response.optJSONArray("updates");
                    Log.i(TAG, "updates: " + updates);

                    server.ts = tsResponse;
                    if ((!ArrayUtil.isEmpty(updates) ? updates.length() : 0) != 0) {
                        LongPollEvents.getInstance().process(updates);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.toString() + "    Log below...");
                    e.printStackTrace();
                    server = null;
                    run();
                }

            }
        }

        private void sleep() {
            try {
                Thread.sleep(5_000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @NonNull
        @Contract("_ -> new")
        private JSONObject getResponse(@NonNull VKLongPollServer server) throws Exception {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("act", "a_check");
            params.put("key", server.key);
            params.put("ts", String.valueOf(server.ts));
            params.put("wait", "25");
            params.put("mode", "490");
            params.put("version", "7");

            String url = "https://" + server.server;

            String buffer = HttpRequest.get(url, params).asString();
            return new JSONObject(buffer);
        }
    }
}

