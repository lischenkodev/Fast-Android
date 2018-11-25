package ru.stwtforever.fast.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.collection.ArrayMap;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKAttachments;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKLongPollServer;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.concurrent.LowThread;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.DBHelper;
import ru.stwtforever.fast.net.HttpRequest;
import ru.stwtforever.fast.util.Utils;

public class LongPollService extends Service {

    public boolean isRunning;

    public static final String TAG = "FastVK LongPoll";

    private LowThread updateThread;
    private boolean error;

    public LongPollService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        launchLongpoll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchLongpoll() {
        if (!isRunning) {
            isRunning = true;
        }
        updateThread = new LowThread(new MessageUpdater());
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
                if (!Utils.hasConnection()) {
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
                        process(updates);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.toString() + "    Log below...");
                    e.printStackTrace();
                    server = null;
                    error = true;
                    continue;
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
            params.put("mode", "238");
            params.put("version", "3");

            String url = "https://" + server.server;

            String buffer = HttpRequest.get(url, params).asString();
            return new JSONObject(buffer);
        }

        private void messageEvent(JSONArray item) {
            VKConversation conversation = VKConversation.parseFromLongPoll(item);

            ArrayList<VKMessage> m = new ArrayList<>();
            m.add(conversation.last);
            CacheStorage.insert(DBHelper.MESSAGES_TABLE, m);

            EventBus.getDefault().postSticky(new Object[]{4, conversation});
        }

        private void messageClearFlags(int id, int mask) {
            if ((mask & VKMessage.UNREAD) != 0) {
                EventBus.getDefault().postSticky(new Object[]{3, id});
            }
        }

        private void editMessageEvent(JSONArray item) {
            VKMessage msg = new VKMessage();

            msg.id = item.optInt(1);
            msg.mask = item.optInt(2);
            msg.peerId = item.optInt(3);
            msg.update_time = item.optInt(4);
            msg.text = item.optString(5);
            msg.attachments = VKAttachments.parseFromLongPoll(item.optJSONObject(6));

            ArrayList<VKMessage> m = new ArrayList<>();
            m.add(msg);

            CacheStorage.insert(DBHelper.MESSAGES_TABLE, m);

            EventBus.getDefault().post(new Object[]{5, msg});
        }

        private void process(JSONArray updates) {
            if (updates.length() == 0) {
                return;
            }

            for (int i = 0; i < updates.length(); i++) {
                JSONArray item = updates.optJSONArray(i);
                int type = item.optInt(0);

                switch (type) {
                    case 3: //clear flags
                        int id = item.optInt(1);
                        int mask = item.optInt(2);
                        messageClearFlags(id, mask);
                        break;
                    case 4: //new message
                        messageEvent(item);
                        break;
                    case 5:
                        editMessageEvent(item);
                        break;
                    case 8: //user online
                        userOnline(item);
                        break;
                    case 9: //user offline
                        userOffline(item);
                        break;
                    case 61: //user types in dialog
                        userType(item);
                        break;
                    case 62: //user types in chat
                        userTyping(item);
                        break;

                }
            }
        }

        private void userOffline(JSONArray item) {
            int userId = item.optInt(1) * (-1);
            boolean timeout = item.optInt(2) == 1;
            int time = item.optInt(3);
            EventBus.getDefault().postSticky(new Object[]{9, userId, time, timeout});
        }

        private void userOnline(JSONArray item) {
            int userId = item.optInt(1) * (-1);
            int time = item.optInt(3);
            EventBus.getDefault().postSticky(new Object[]{8, userId, time});
        }

        private void userType(JSONArray item) {
            int userId = item.optInt(1);
            int flag = item.optInt(2);

            if (flag == 1) {

            }
        }

        private void userTyping(JSONArray item) {
            int userId = item.optInt(1);
            int peerId = 2_000_000_000 + item.optInt(2);
        }
    }

    private VKMessage getMessage(int id) throws Exception {
        return VKApi.messages().getById().messageIds(id).extended(true).execute(VKMessage.class).get(0);
    }
}

