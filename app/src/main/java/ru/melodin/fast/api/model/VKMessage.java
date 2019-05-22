package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.api.UserConfig;

public class VKMessage extends VKModel implements Serializable {

    public static final int UNREAD = 1;        //сообщение не прочитано
    public static final int OUTBOX = 2;        //исходящее сообщение
    public static final int REPLIED = 4;       //на сообщение был создан ответ
    public static final int IMPORTANT = 8;      //помеченное сообщение
    public static final int CHAT = 16;          //сообщение отправлено через диалог
    public static final int FRIENDS = 32;       //сообщение отправлено другом
    public static final int SPAM = 64;          //сообщение помечено как "Спам"
    public static final int DELETED = 128;      //сообщение удалено (в корзине)
    public static final int FIXED = 256;        //сообщение проверено пользователем на спам
    public static final int MEDIA = 512;        //сообщение содержит медиаконтент
    public static final int BESEDA = 8192;      //беседа

    public static final int STATUS_SENDING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_ERROR = 2;
    public static int count;
    public static int lastHistoryCount;
    public static ArrayList<VKUser> users;
    public static ArrayList<VKGroup> groups;
    public Action action;
    public int actionUserId = 0;
    public int id;
    public int peerId;
    public int fromId;
    public int randomId = -1;
    public int status;
    public int flags;
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
    public boolean added;

    public enum Action {
        CHAT_CREATE, CHAT_INVITE_USER, CHAT_KICK_USER, CHAT_TITLE_UPDATE, CHAT_PHOTO_UPDATE, CHAT_PHOTO_REMOVE, CHAT_PIN_MESSAGE, CHAT_UNPIN_MESSAGE, CHAT_INVITE_USER_BY_LINK
    }

    public static Action getAction(String action) {
        switch (action) {
            case "chat_create":
                return Action.CHAT_CREATE;
            case "chat_invite_user":
                return Action.CHAT_INVITE_USER;
            case "chat_kick_user":
                return Action.CHAT_KICK_USER;
            case "chat_title_update":
                return Action.CHAT_TITLE_UPDATE;
            case "chat_photo_update":
                return Action.CHAT_PHOTO_UPDATE;
            case "chat_photo_remove":
                return Action.CHAT_PHOTO_REMOVE;
            case "chat_pin_message":
                return Action.CHAT_PIN_MESSAGE;
            case "chat_unpin_message":
                return Action.CHAT_UNPIN_MESSAGE;
            case "chat_invite_user_by_link":
                return Action.CHAT_INVITE_USER_BY_LINK;
            default:
                return null;
        }
    }

    public static String getAction(Action action) {
        if (action == null) return "";
        switch (action) {
            case CHAT_CREATE:
                return "chat_create";
            case CHAT_INVITE_USER:
                return "chat_invite_user";
            case CHAT_KICK_USER:
                return "chat_kick_user";
            case CHAT_TITLE_UPDATE:
                return "chat_title_update";
            case CHAT_PHOTO_UPDATE:
                return "chat_photo_update";
            case CHAT_PHOTO_REMOVE:
                return "chat_photo_remove";
            case CHAT_PIN_MESSAGE:
                return "chat_pin_message";
            case CHAT_UNPIN_MESSAGE:
                return "chat_unpin_message";
            case CHAT_INVITE_USER_BY_LINK:
                return "chat_invite_user_by_link";
            default:
                return "";
        }
    }

    public VKMessage() {
    }

    public VKMessage(JSONObject o) throws JSONException {
        status = VKMessage.STATUS_SENT;

        history_groups = groups;
        history_users = users;

        date = o.optLong("date");
        fromId = o.optInt("from_id");
        peerId = o.optInt("peer_id");
        id = o.optInt("id");
        out = fromId == UserConfig.userId;
        text = o.optString("text");
        chatMessageId = o.optInt("conversation_message_id");
        important = o.optBoolean("important");
        randomId = o.optInt("random_id", -1);
        update_time = o.optLong("update_time");

        if (o.has("action")) {
            JSONObject a = o.optJSONObject("action");
            actionUserId = a.optInt("member_id");
            action = getAction(a.optString("type"));
            actionText = a.optString("text");
        }

        JSONArray fws = o.optJSONArray("fwd_messages");

        if (fws != null && fws.length() > 0) {
            fwd_messages = parseForwarded(fws);
        }

        JSONArray attachments = o.optJSONArray("attachments");

        if (attachments != null && attachments.length() > 0) {
            this.attachments = VKAttachments.parse(attachments);
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

    private static ArrayList<VKMessage> parseForwarded(JSONArray a) throws JSONException {
        ArrayList<VKMessage> ms = new ArrayList<>();

        for (int i = 0; i < a.length(); i++) {
            VKMessage m = new VKMessage(a.optJSONObject(i));

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
