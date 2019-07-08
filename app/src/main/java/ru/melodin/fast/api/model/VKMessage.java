package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
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
    public static final int BESEDA = 8192;
    //беседа
    private static final long serialVersionUID = 1L;

    public static int count;
    public static int lastHistoryCount;

    public static ArrayList<VKUser> users = new ArrayList<>();
    public static ArrayList<VKGroup> groups = new ArrayList<>();

    private Action action;
    private int actionId;
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
    private ArrayList<VKMessage> fwdMessages = new ArrayList<>();
    private VKReplyMessage reply;
    private long updateTime;
    private boolean added;
    private Status status = Status.SENT;
    private boolean needUpdate;
    private boolean playing;

    public VKMessage() {
    }

    public VKMessage(@NonNull JSONObject o) throws JSONException {
        this.date = o.optLong("date", -1);
        this.fromId = o.optInt("from_id", -1);
        this.peerId = o.optInt("peer_id", -1);
        this.id = o.optInt("id", -1);
        this.out = this.fromId == UserConfig.Companion.getUserId();
        this.text = o.optString("text");
        this.conversationMessageId = o.optInt("conversation_message_id", -1);
        this.important = o.optBoolean("important");
        this.randomId = o.optInt("random_id", -1);
        this.updateTime = o.optLong("update_time", -1);

        JSONObject action = o.optJSONObject("action");
        if (action != null) {
            this.actionId = action.optInt("member_id", -1);
            this.action = getAction(action.optString("type"));
            this.actionText = action.optString("text");
        }

        JSONObject reply = o.optJSONObject("reply_message");

        if (reply != null) {
            this.reply = new VKReplyMessage(reply);
        }

        JSONArray fwdMessages = o.optJSONArray("fwd_messages");

        if (!ArrayUtil.INSTANCE.isEmpty(fwdMessages)) {
            this.fwdMessages = parseForwarded(fwdMessages);
        }

        JSONArray attachments = o.optJSONArray("attachments");

        if (!ArrayUtil.INSTANCE.isEmpty(attachments)) {
            this.attachments = VKAttachments.parse(attachments);
        }
    }

    @Nullable
    @Contract(pure = true)
    public static Status getStatus(@NonNull String status) {
        switch (status) {
            case "sending":
                return Status.SENDING;
            case "sent":
                return Status.SENT;
            case "error":
                return Status.ERROR;
        }

        return null;
    }

    @NonNull
    @Contract(pure = true)
    public static String getStatus(Status status) {
        if (status == null) return "";
        switch (status) {
            case SENT:
                return "sent";
            case ERROR:
                return "error";
            case SENDING:
                return "sending";
        }
        return "";
    }

    @Nullable
    @Contract(pure = true)
    public static Action getAction(@NonNull String action) {
        switch (action) {
            case "chat_create":
                return Action.CREATE;
            case "chat_invite_user":
                return Action.INVITE_USER;
            case "chat_kick_user":
                return Action.KICK_USER;
            case "chat_title_update":
                return Action.TITLE_UPDATE;
            case "chat_photo_update":
                return Action.PHOTO_UPDATE;
            case "chat_photo_remove":
                return Action.PHOTO_REMOVE;
            case "chat_pin_message":
                return Action.PIN_MESSAGE;
            case "chat_unpin_message":
                return Action.UNPIN_MESSAGE;
            case "chat_invite_user_by_link":
                return Action.INVITE_USER_BY_LINK;
        }
        return null;
    }

    public static String getAction(Action action) {
        if (action == null) return "";
        switch (action) {
            case CREATE:
                return "chat_create";
            case INVITE_USER:
                return "chat_invite_user";
            case KICK_USER:
                return "chat_kick_user";
            case TITLE_UPDATE:
                return "chat_title_update";
            case PHOTO_UPDATE:
                return "chat_photo_update";
            case PHOTO_REMOVE:
                return "chat_photo_remove";
            case PIN_MESSAGE:
                return "chat_pin_message";
            case UNPIN_MESSAGE:
                return "chat_unpin_message";
            case INVITE_USER_BY_LINK:
                return "chat_invite_user_by_link";
        }

        return "";
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

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isNeedUpdate() {
        return needUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
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

    public enum Action {
        CREATE, INVITE_USER, KICK_USER, TITLE_UPDATE, PHOTO_UPDATE, PHOTO_REMOVE, PIN_MESSAGE, UNPIN_MESSAGE, INVITE_USER_BY_LINK
    }

    public enum Status {
        SENDING, SENT, ERROR
    }
}
