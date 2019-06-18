package ru.melodin.fast.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKLongPollServer;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.net.HttpRequest;
import ru.melodin.fast.util.Util;

public class LongPollService extends Service {

    public static final String TAG = "FastVK LongPoll";
    public boolean isRunning;

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
            VKLongPollServer server = null;
            if (!isRunning) {
                isRunning = true;
            }
            while (isRunning) {
                if (!Util.hasConnection()) {
                    Log.e(TAG, "no connection");
                    sleep();
                    continue;
                }

                if (!UserConfig.isLoggedIn()) {
                    sleep();
                    continue;
                }
                try {
                    if (server == null) {
                        server = VKApi.messages().getLongPollServer()
                                .execute(VKLongPollServer.class).get(0);
                    }

                    JSONObject response = getResponse(server);
                    if (response == null || response.has("failed")) {
                        // failed get response, try again
                        Log.w(TAG, "Failed get response from");
                        Thread.sleep(1_000);
                        server = null;
                        continue;
                    }

                    long tsResponse = response.optLong("ts");
                    JSONArray updates = response.optJSONArray("updates");
                    Log.i(TAG, "updates: " + updates);

                    server.ts = tsResponse;
                    if (updates.length() != 0) {
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
            long time = 5_000;
            try {
                Thread.sleep(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JSONObject getResponse(VKLongPollServer server) throws Exception {
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

