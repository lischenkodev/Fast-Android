package ru.melodin.fast.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.collection.ArrayMap;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKAttachments;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKLongPollServer;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.net.HttpRequest;
import ru.melodin.fast.util.StringUtils;
import ru.melodin.fast.util.Util;

public class LongPollService extends Service {

    public static final String TAG = "FastVK LongPoll";
    public boolean isRunning;

    public static final String KEY_MESSAGE_NEW = "message_new";
    public static final String KEY_MESSAGE_EDIT = "message_edit";
    public static final String KEY_MESSAGE_CLEAR_FLAGS = "message_clear_flags";
    public static final String KEY_MESSAGE_SET_FLAGS = "message_set_flags";
    public static final String KEY_USER_ONLINE = "user_online";
    public static final String KEY_USER_OFFLINE = "user_offline";

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
                        process(updates);
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
            params.put("wait", "10");
            params.put("mode", "490");
            params.put("version", "6");

            String url = "https://" + server.server;

            String buffer = HttpRequest.get(url, params).asString();
            return new JSONObject(buffer);
        }

        private void messageEvent(JSONArray item) {
            int mId = item.optInt(1);
            int flags = item.optInt(2);
            int peerId = item.optInt(3);
            int time = item.optInt(4);
            String text = StringUtils.unescape(item.optString(5));

            JSONObject fromActions = item.optJSONObject(6);
            JSONObject attachments = item.optJSONObject(7);

            int randomId = item.optInt(8);
            int conversationMessageId = item.optInt(9);
            int updateTime = item.optInt(10);

            VKConversation conversation = new VKConversation();

            VKMessage last = new VKMessage();
            last.id = mId;
            last.flags = flags;
            last.peerId = peerId;
            last.date = time;
            last.text = text;
            last.chatMessageId = conversationMessageId;
            last.update_time = updateTime;

            conversation.read = last.read = ((last.flags & VKMessage.UNREAD) == 0);

            last.out = (last.flags & VKMessage.OUTBOX) != 0;
            last.fromId = fromActions != null && fromActions.has("from") ? fromActions.optInt("from") : last.out ? UserConfig.userId : peerId;
            last.attachments = VKAttachments.parseFromLongPoll(attachments);
            last.randomId = randomId;

            conversation.type = VKConversation.getType(last.peerId);

            conversation.last = last;

            EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_NEW, conversation});
            Log.d("FVK New Message", item.toString());
        }

        private void messageSetFlags(JSONArray item) {
            int mId = item.optInt(1);
            int flags = item.optInt(2);
            int peerId = item.optInt(3);

            VKMessage message = CacheStorage.getMessage(mId);

            if (message != null) {
                message.flags = flags;
                message.peerId = peerId;

                if (VKMessage.isImportant(flags))
                    message.important = true;

                if (VKMessage.isUnread(flags))
                    message.read = false;

                CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID + " = ?", String.valueOf(mId));
            }

            EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_SET_FLAGS, mId, flags, peerId});
            Log.d("FVK Message Edit", item.toString());
        }

        private void messageClearFlags(JSONArray item) {
            int mId = item.optInt(1);
            int flags = item.optInt(2);
            int peerId = item.optInt(3);

            VKMessage message = CacheStorage.getMessage(mId);

            if (message != null) {
                message.flags = flags;
                message.peerId = peerId;

                if (VKMessage.isImportant(flags))
                    message.important = false;

                if (VKMessage.isUnread(flags))
                    message.read = true;

                CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID + " = ?", String.valueOf(mId));
            }

            EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_CLEAR_FLAGS, mId, flags, peerId});

            Log.d("FVK Message Edit", item.toString());
        }

        private void editMessageEvent(JSONArray item) {
            int id = item.optInt(1);
            int flags = item.optInt(2);
            int peerId = item.optInt(3);
            long time = item.optInt(4);
            String text = item.optString(5);
            JSONObject fromActions = item.optJSONObject(6);
            //JSONObject attachments = item.optJSONObject(7);
            int randomId = item.optInt(8);
            int conversationMessageId = item.optInt(9);
            int updateTime = item.optInt(10);

            VKMessage message = CacheStorage.getMessage(id);
            if (message == null) return;

            message.flags = flags;
            message.peerId = peerId;
            message.date = time;
            message.text = text;
            message.fromId = fromActions != null && fromActions.has("from") ? fromActions.optInt("from") : message.out ? UserConfig.userId : peerId;
            message.randomId = randomId;
            message.chatMessageId = conversationMessageId;
            message.update_time = updateTime;

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID + " = ?", String.valueOf(id));

            EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_EDIT, message});
        }

        private void process(JSONArray updates) {
            if (updates.length() == 0) {
                return;
            }

            for (int i = 0; i < updates.length(); i++) {
                JSONArray item = updates.optJSONArray(i);
                int type = item.optInt(0);

                switch (type) {
                    case 2: //set flags
                        messageSetFlags(item);
                        break;
                    case 3: //clear flags
                        messageClearFlags(item);
                        break;
                    case 4: //new message
                        messageEvent(item);
                        break;
                    case 5: //edit message
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

            VKUser user = MemoryCache.getUser(userId);
            if (user != null) {
                user.last_seen = time;
                user.online = false;
                user.online_mobile = false;
            }

            EventBus.getDefault().postSticky(new Object[]{KEY_USER_OFFLINE, userId, time, timeout});
        }

        private void userOnline(JSONArray item) {
            int userId = item.optInt(1) * (-1);
            int time = item.optInt(3);

            VKUser user = MemoryCache.getUser(userId);
            if (user != null) {
                user.last_seen = time;
                user.online = true;
                user.online_mobile = true;
            }

            EventBus.getDefault().postSticky(new Object[]{KEY_USER_ONLINE, userId, time});
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
}

