package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.util.ArrayUtil;

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

    public static int count;
    public static int lastHistoryCount;
    public static ArrayList<VKUser> users;
    public static ArrayList<VKGroup> groups;

    private Action action;
    private int actionUserId;
    private int id;
    private int peerId;
    private int fromId;
    private int randomId;
    private int flags;
    private int conversationMessageId;
    private String type;
    private long date;
    private String actionText;
    private String text;
    private boolean read;
    private boolean out;
    private boolean important;
    private int unread;
    private ArrayList<VKModel> attachments = new ArrayList<>();
    private ArrayList<VKMessage> fwdMessages;
    private VKReplyMessage reply;
    private ArrayList<VKUser> historyUsers;
    private ArrayList<VKGroup> historyGroups;
    private long updateTime;
    private boolean added;

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

        historyGroups = groups;
        historyUsers = users;

        this.date = o.optLong("date", -1);
        this.fromId = o.optInt("from_id", -1);
        this.peerId = o.optInt("peer_id", -1);
        this.id = o.optInt("id", -1);
        this.out = this.fromId == UserConfig.userId;
        this.text = o.optString("text");
        this.conversationMessageId = o.optInt("conversation_message_id", -1);
        this.important = o.optBoolean("important");
        this.randomId = o.optInt("random_id", -1);
        this.updateTime = o.optLong("update_time", -1);

        JSONObject action = o.optJSONObject("action");
        if (action != null) {
            this.actionUserId = action.optInt("member_id", -1);
            this.action = getAction(action.optString("type"));
            this.actionText = action.optString("text");
        }

        JSONObject reply = o.optJSONObject("reply_message");

        if (reply != null) {
            this.reply = new VKReplyMessage(reply);
        }

        JSONArray fwdMessages = o.optJSONArray("fwd_messages");

        if (!ArrayUtil.isEmpty(fwdMessages)) {
            this.fwdMessages = parseForwarded(fwdMessages);
        }

        JSONArray attachments = o.optJSONArray("attachments");

        if (!ArrayUtil.isEmpty(attachments)) {
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
        ArrayList<VKMessage> forwarded = new ArrayList<>();

        for (int i = 0; i < a.length(); i++) {
            forwarded.add(new VKMessage(a.optJSONObject(i)));
        }

        return forwarded;
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



    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getActionUserId() {
        return actionUserId;
    }

    public void setActionUserId(int actionUserId) {
        this.actionUserId = actionUserId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getRandomId() {
        return randomId;
    }

    public void setRandomId(int randomId) {
        this.randomId = randomId;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getConversationMessageId() {
        return conversationMessageId;
    }

    public void setConversationMessageId(int conversationMessageId) {
        this.conversationMessageId = conversationMessageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public ArrayList<VKModel> getAttachments() {
        return attachments;
    }

    public void setAttachments(ArrayList<VKModel> attachments) {
        this.attachments = attachments;
    }

    public ArrayList<VKMessage> getFwdMessages() {
        return fwdMessages;
    }

    public void setFwdMessages(ArrayList<VKMessage> fwdMessages) {
        this.fwdMessages = fwdMessages;
    }

    public VKReplyMessage getReply() {
        return reply;
    }

    public void setReply(VKReplyMessage reply) {
        this.reply = reply;
    }

    public ArrayList<VKUser> getHistoryUsers() {
        return historyUsers;
    }

    public void setHistoryUsers(ArrayList<VKUser> historyUsers) {
        this.historyUsers = historyUsers;
    }

    public ArrayList<VKGroup> getHistoryGroups() {
        return historyGroups;
    }

    public void setHistoryGroups(ArrayList<VKGroup> historyGroups) {
        this.historyGroups = historyGroups;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }
}
