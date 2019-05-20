package ru.melodin.fast.api;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.StringUtils;

public class LongPollEvents {

    public static final String KEY_MESSAGE_NEW = "message_new";
    public static final String KEY_MESSAGE_EDIT = "message_edit";
    public static final String KEY_MESSAGE_CLEAR_FLAGS = "message_clear_flags";
    public static final String KEY_MESSAGE_SET_FLAGS = "message_set_flags";
    public static final String KEY_USER_ONLINE = "user_online";
    public static final String KEY_USER_OFFLINE = "user_offline";
    public static final String KEY_MESSAGE_UPDATE = "message_update";

    private static LongPollEvents instance;

    public static synchronized LongPollEvents getInstance() {
        if (instance == null) instance = new LongPollEvents();
        return instance;
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

        if (attachments != null && attachments.length() > 0) {
            loadMessage(mId);
        }
        //last.attachments = VKAttachments.parseFromLongPoll(attachments);


        last.randomId = randomId;

        conversation.type = VKConversation.getType(last.peerId);

        conversation.last = last;

        EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_NEW, conversation});
        Log.d("FVK New Message", item.toString());
    }

    private void loadMessage(final int mId) {
        ThreadExecutor.execute(new LowThread() {

            VKMessage message;

            @Override
            public void run() {
                super.run();

                VKApi.messages().getById().messageIds(mId).execute(VKMessage.class, new VKApi.OnResponseListener<VKMessage>() {
                    @Override
                    public void onSuccess(ArrayList<VKMessage> models) {
                        message = models.get(0);
                        EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_UPDATE, mId});

                        CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID + " = ?", String.valueOf(mId));
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Error load message", Log.getStackTraceString(e));
                    }
                });
            }
        });
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

    public void process(JSONArray updates) {
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
                case 62: //user types in chat
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
}
