package ru.melodin.fast.api.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.api.UserConfig;

public class VKMessage extends VKModel implements Serializable {

    public static final int UNREAD = 1;        //сообщение не прочитано
    public static final int OUTBOX = 2;        //исходящее сообщение
    public static final int REPLIED = 4;        //на сообщение был создан ответ
    public static final int IMPORTANT = 8;    //помеченное сообщение
    public static final int CHAT = 16;        //сообщение отправлено через диалог
    public static final int FRIENDS = 32;        //сообщение отправлено другом
    public static final int SPAM = 64;        //сообщение помечено как "Спам"
    public static final int DELETED = 128;    //сообщение удалено (в корзине)
    public static final int FIXED = 256;        //сообщение проверено пользователем на спам
    public static final int MEDIA = 512;        //сообщение содержит медиаконтент
    public static final int BESEDA = 8192;    //беседа
    public static final String ACTION_CHAT_CREATE = "chat_create";
    public static final String ACTION_CHAT_INVITE_USER = "chat_invite_user";
    public static final String ACTION_CHAT_KICK_USER = "chat_kick_user";
    public static final String ACTION_CHAT_TITLE_UPDATE = "chat_title_update";
    public static final String ACTION_CHAT_PHOTO_UPDATE = "chat_photo_update";
    public static final String ACTION_CHAT_PHOTO_REMOVE = "chat_photo_remove";
    public static final String ACTION_CHAT_PIN_MESSAGE = "chat_pin_message";
    public static final String ACTION_CHAT_UNPIN_MESSAGE = "chat_unpin_message";
    public static final String ACTION_CHAT_INVITE_USER_BY_LINK = "chat_invite_user_by_link";
    public static final int STATUS_SENDING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_ERROR = 2;
    private static final String TAG = "FVKMessage";
    public static int count;
    public static int lastHistoryCount;
    public static ArrayList<VKUser> users;
    public static ArrayList<VKGroup> groups;
    public String actionType = null;
    public int actionUserId = 0;
    public int id;
    public int peerId;
    public int fromId;
    public int randomId;
    public int mask;
    public int status;
    public int flag;
    public int chatMessageId;
    public String type;
    public long date;
    public String actionText;
    public String text;
    public boolean read;
    public boolean out;
    public boolean important;
    public int unread;
    public ArrayList<VKModel> attachments = new ArrayList<>();
    public ArrayList<VKMessage> fwd_messages;
    public ArrayList<VKUser> history_users;
    public ArrayList<VKGroup> history_groups;
    public long update_time;
    public boolean isAdded;

    public VKMessage() {
    }

    public VKMessage(JSONObject o) throws JSONException {
        Log.d(TAG, o.toString());

        status = VKMessage.STATUS_SENT;

        history_groups = groups;
        history_users = users;

        date = o.optLong("date");
        fromId = o.optInt("from_id");
        peerId = o.optInt("peer_id");
        id = o.optInt("id");
        out = fromId == UserConfig.userId;
        read = false;
        text = o.optString("text");
        chatMessageId = o.optInt("conversation_message_id");
        important = o.optBoolean("important");
        randomId = o.optInt("random_id");
        update_time = o.optLong("update_time");

        if (o.has("action")) {
            JSONObject a = o.optJSONObject("action");
            actionUserId = a.optInt("member_id");
            actionType = a.optString("type");
            actionText = a.optString("text");
        }

        JSONArray fws = o.optJSONArray("fwd_messages");

        if (fws != null && fws.length() > 0) {
            fwd_messages = parseAttMessages(fws);
        }

        JSONArray atts = o.optJSONArray("attachments");

        if (atts != null && atts.length() > 0) {
            attachments = VKAttachments.parse(atts);
        }
    }

    public static boolean isDeleted(int flags) {
        return (flags & DELETED) != 0;
    }

    public static boolean isImportant(int flags) {
        return (flags & IMPORTANT) != 0;
    }

    public static boolean isUnread(int flags) {
        return (flags & UNREAD) != 0;
    }

    static VKMessage parseFromAttach(JSONObject o) {
        VKMessage m = new VKMessage();

        m.date = o.optLong("date");
        m.fromId = o.optInt("from_id");
        m.out = m.fromId == UserConfig.userId;
        m.text = o.optString("text");

        JSONArray attachments = o.optJSONArray("attachments");
        if (attachments != null) {
            if (attachments.length() > 0)
                m.attachments = VKAttachments.parse(attachments);
        }

        m.update_time = o.optLong("update_time");

        return m;
    }

    private static ArrayList<VKMessage> parseAttMessages(JSONArray a) throws JSONException {
        ArrayList<VKMessage> ms = new ArrayList<>();

        for (int i = 0; i < a.length(); i++) {
            VKMessage m = parseFromAttach(a.optJSONObject(i));
            ms.add(m);
        }

        return ms;
    }

    public boolean isChat() {
        return peerId > 2_000_000_000;
    }

    public boolean isFromGroup() {
        return VKGroup.isGroupId(fromId);
    }

    public boolean isGroup() {
        return VKGroup.isGroupId(peerId);
    }

    public boolean isUser() {
        return !isGroup() && !isChat();
    }

    public boolean isFromUser() {
        return !isFromGroup();
    }
}
