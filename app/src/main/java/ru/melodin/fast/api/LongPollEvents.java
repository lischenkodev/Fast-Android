package ru.melodin.fast.api;

import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.StringUtils;

public class LongPollEvents {

    public static final String KEY_MESSAGE_NEW = "message_new";
    public static final String KEY_MESSAGE_EDIT = "message_edit";
    public static final String KEY_MESSAGE_CLEAR_FLAGS = "message_clear_flags";
    public static final String KEY_MESSAGE_SET_FLAGS = "message_set_flags";
    public static final String KEY_USER_ONLINE = "user_online";
    public static final String KEY_USER_OFFLINE = "user_offline";
    public static final String KEY_MESSAGE_UPDATE = "message_update";
    public static final String KEY_NOTIFICATIONS_CHANGE = "notifications_change";

    private static LongPollEvents instance;

    public static synchronized LongPollEvents getInstance() {
        if (instance == null) instance = new LongPollEvents();
        return instance;
    }

    private void messageEvent(JSONArray item) {
        int mId = item.optInt(1);
        int flags = item.optInt(2);
        int peerId = item.optInt(3);
        int date = item.optInt(4);
        String text = StringUtils.unescape(item.optString(5));

        JSONObject fromActions = item.optJSONObject(6);
        JSONObject attachments = item.optJSONObject(7);

        int randomId = item.optInt(8);
        int conversationMessageId = item.optInt(9);
        int updateTime = item.optInt(10);

        VKConversation conversation = new VKConversation();

        VKMessage last = new VKMessage();
        last.setId(mId);
        last.setFlags(flags);
        last.setPeerId(peerId);
        last.setDate(date);
        last.setConversationMessageId(conversationMessageId);
        last.setUpdateTime(updateTime);
        conversation.setRead((last.getFlags() & VKMessage.UNREAD) == 0);

        last.setRead(conversation.isRead());
        last.setOut((last.getFlags() & VKMessage.OUTBOX) != 0);
        last.setFromId(fromActions != null && fromActions.has("from") ? fromActions.optInt("from", -1) : last.isOut() ? UserConfig.userId : peerId);

        if (fromActions != null) {
            String actionType = fromActions.optString("source_act");
            String actionText = fromActions.optString("source_message");
            int actionId = fromActions.optInt("source_mid", -1);

            if (actionId != -1) {
                last.setActionId(actionId);
                last.setAction(VKMessage.getAction(actionType));
                last.setActionText(actionText);
            }
        }

        last.setText(fromActions != null && fromActions.optInt("source_mid", -1) != -1 ? "" : text);

        if (attachments != null && attachments.length() > 0) {
            last.setNeedUpdate(true);
        }

        last.setRandomId(randomId);

        conversation.setType(VKConversation.getType(last.getPeerId()));

        conversation.setLast(last);

        EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_NEW, conversation});
        Log.d("FVK New Message", item.toString());
    }

    private void messageSetFlags(JSONArray item) {
        int mId = item.optInt(1);
        int flags = item.optInt(2);
        int peerId = item.optInt(3);

        VKMessage message = CacheStorage.getMessage(mId);

        if (message != null) {
            message.setFlags(flags);
            message.setPeerId(peerId);

            if (VKMessage.isImportant(flags))
                message.setImportant(true);

            if (VKMessage.isUnread(flags))
                message.setRead(false);

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, mId);
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
            message.setFlags(flags);
            message.setPeerId(peerId);

            if (VKMessage.isImportant(flags))
                message.setImportant(false);

            if (VKMessage.isUnread(flags))
                message.setRead(true);

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, mId);
        }

        EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_CLEAR_FLAGS, mId, flags, peerId});

        Log.d("FVK Message Edit", item.toString());
    }

    private void editMessageEvent(JSONArray item) {
        int id = item.optInt(1);
        int flags = item.optInt(2);
        int peerId = item.optInt(3);
        long date = item.optInt(4);
        String text = StringUtils.unescape(item.optString(5));
        JSONObject fromActions = item.optJSONObject(6);
        //JSONObject attachments = item.optJSONObject(7);
        int randomId = item.optInt(8);
        int conversationMessageId = item.optInt(9);
        int updateTime = item.optInt(10);

        VKMessage message = CacheStorage.getMessage(id);
        if (message == null) return;

        message.setFlags(flags);
        message.setPeerId(peerId);
        message.setDate(date);
        message.setText(text);
        message.setFromId(fromActions != null && fromActions.has("from") ? fromActions.optInt("from") : message.isOut() ? UserConfig.userId : peerId);
        message.setRandomId(randomId);
        message.setConversationMessageId(conversationMessageId);
        message.setUpdateTime(updateTime);

        CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, id);

        EventBus.getDefault().postSticky(new Object[]{KEY_MESSAGE_EDIT, message});
    }

    public void process(@NonNull JSONArray updates) {
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
                case 114: //notifications changes
                    changeNotifications(item);
                    break;
            }
        }
    }

    private void changeNotifications(@NonNull JSONArray item) {
        JSONObject o = item.optJSONObject(1);

        int peerId = o.optInt("peer_id", -1);
        boolean sound = o.optInt("sound", -1) == 1;
        int disabledUntil = o.optInt("disabled_until", -2);

        EventBus.getDefault().postSticky(new Object[]{KEY_NOTIFICATIONS_CHANGE, peerId, !sound, disabledUntil});
    }

    private void userOffline(@NonNull JSONArray item) {
        int userId = item.optInt(1) * (-1);
        boolean timeout = item.optInt(2) == 1;
        int time = item.optInt(3);

        VKUser user = CacheStorage.getUser(userId);
        if (user != null) {
            user.setOnline(false);
            user.setOnlineMobile(false);
            user.setLastSeen(time);
        }

        EventBus.getDefault().postSticky(new Object[]{KEY_USER_OFFLINE, userId, user.getLastSeen(), timeout});
    }

    private void userOnline(@NonNull JSONArray item) {
        int userId = item.optInt(1) * (-1);
        int platform = item.optInt(2);
        int time = item.optInt(3);

        VKUser user = CacheStorage.getUser(userId);
        if (user != null) {
            user.setOnline(true);
            user.setOnlineMobile(platform > 0);
            user.setLastSeen(time);
        }

        EventBus.getDefault().postSticky(new Object[]{KEY_USER_ONLINE, userId, user.getLastSeen(), user.isOnlineMobile()});
    }
}
