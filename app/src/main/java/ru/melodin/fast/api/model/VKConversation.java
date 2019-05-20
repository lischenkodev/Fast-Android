package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKConversation extends VKModel implements Serializable {

    public static int count;
    public static ArrayList<VKUser> users = new ArrayList<>();
    public static ArrayList<VKGroup> groups = new ArrayList<>();
    public int conversations_count;
    public int read_in;
    public int read_out;
    public int last_mId;
    public int unread;
    public int membersCount;

    public boolean can_write;
    public int reason;

    public boolean read, group_channel;

    public VKMessage pinned, last;

    public String title;
    public Type type;
    public String state;
    public String photo_50, photo_100, photo_200;

    //other
    public ArrayList<VKUser> conversation_users;
    public ArrayList<VKGroup> conversation_groups;

    //acl
    public boolean can_change_pin, can_change_info, can_change_invite_link, can_invite, can_promote_users, can_see_invite_link;

    //push_settings
    public int disabled_until;
    public boolean disabled_forever;
    public boolean no_sound;

    public enum Type {
        CHAT, GROUP, USER
    }

    public enum Reason {
        KICKED, LEFT, USER_DELETED, USER_BLACKLIST, USER_PRIVACY, MESSAGES_OFF, MESSAGES_BLOCKED, NO_ACCESS_CHAT, NO_ACCESS_EMAIL, NO_ACCESS_GROUP
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    public VKConversation() {

    }

    public static int getReason(Reason reason) {
        switch (reason) {
            case USER_BLACKLIST:
                return 900;
            case USER_PRIVACY:
                return 902;
            case USER_DELETED:
                return 18;
            case LEFT:
                return 2;
            case KICKED:
                return 1;
            case MESSAGES_OFF:
                return 915;
            case MESSAGES_BLOCKED:
                return 916;
            case NO_ACCESS_CHAT:
                return 917;
            case NO_ACCESS_EMAIL:
                return 918;
            case NO_ACCESS_GROUP:
                return 203;
            default:
                return -1;
        }
    }

    public static Reason getReason(int reason) {
        switch (reason) {
            case 900:
                return Reason.USER_BLACKLIST;
            case 902:
                return Reason.USER_PRIVACY;
            case 18:
                return Reason.USER_DELETED;
            case 2:
                return Reason.LEFT;
            case 1:
                return Reason.KICKED;
            case 915:
                return Reason.MESSAGES_OFF;
            case 916:
                return Reason.MESSAGES_BLOCKED;
            case 917:
                return Reason.NO_ACCESS_CHAT;
            case 918:
                return Reason.NO_ACCESS_EMAIL;
            case 203:
                return Reason.NO_ACCESS_GROUP;
            default:
                return null;
        }
    }

    public VKConversation(JSONObject o, JSONObject msg) throws JSONException {

        conversations_count = count;
        conversation_groups = groups;
        conversation_users = users;

        groups = null;
        users = null;

        if (msg != null)
            last = new VKMessage(msg);

        JSONObject peer = o.optJSONObject("peer");
        type = msg != null ? getType(last.peerId) : getType(peer.optString("type"));

        read_in = o.optInt("in_read");
        read_out = o.optInt("out_read");
        last_mId = o.optInt("last_message_id");
        unread = o.optInt("unread_count");

        read = last == null || (last.out && read_out == last_mId || !last.out && read_in == last_mId);

        JSONObject j_can_write = o.optJSONObject("can_write");
        can_write = j_can_write.optBoolean("allowed");
        if (!can_write) {
            reason = j_can_write.optInt("reason", -1);
        }

        JSONObject push_settings = o.optJSONObject("push_settings");
        if (push_settings != null) {
            disabled_until = push_settings.optInt("disabled_until", -1);
            disabled_forever = push_settings.optBoolean("disabled_forever", false);
            no_sound = push_settings.optBoolean("no_sound");
        }

        JSONObject ch = o.optJSONObject("chat_settings");
        if (ch != null) {
            title = ch.optString("title");
            membersCount = ch.optInt("members_count");
            state = ch.optString("state");
            group_channel = ch.optBoolean("is_group_channel");

            JSONObject p = ch.optJSONObject("photo");
            if (p != null) {
                photo_50 = p.optString("photo_50", "");
                photo_100 = p.optString("photo_100", "");
                photo_200 = p.optString("photo_200", "");
            }

            JSONObject acl = ch.optJSONObject("acl");
            if (acl != null) {
                can_invite = acl.optBoolean("can_invite");
                can_promote_users = acl.optBoolean("can_promote_users");
                can_see_invite_link = acl.optBoolean("can_see_invite_link");
                can_change_invite_link = acl.optBoolean("can_change_invite_link");
                can_change_info = acl.optBoolean("can_change_info");
                can_change_pin = acl.optBoolean("can_change_pin");
            }

            JSONObject o_pinned = ch.optJSONObject("pinned_message");
            if (o_pinned != null) {
                pinned = VKMessage.parseFromAttach(o_pinned);
            }
        }
    }

    public static Type getType(int peerId) {
        if (VKConversation.isChatId(peerId)) return Type.CHAT;
        if (VKGroup.isGroupId(peerId)) return Type.GROUP;
        return Type.USER;
    }

    public static Type getType(String type) {
        switch (type) {
            case "user":
                return Type.USER;
            case "group":
                return Type.GROUP;
            case "chat":
                return Type.CHAT;
            default:
                return null;
        }
    }

    public static String getType(Type type) {
        switch (type) {
            case USER:
                return "user";
            case GROUP:
                return "group";
            case CHAT:
                return "chat";
            default:
                return null;
        }
    }

    private static boolean isChatId(int peerId) {
        return peerId > 2_000_000_00;
    }

    public boolean isChat() {
        return isChatId(last.peerId);
    }

    public boolean isFromGroup() {
        return VKGroup.isGroupId(last.fromId);
    }

    public boolean isGroup() {
        return VKGroup.isGroupId(last.peerId);
    }

    public boolean isUser() {
        return !isGroup() && !isChat() && !group_channel;
    }

    public boolean isFromUser() {
        return !isFromGroup();
    }

    public boolean isNotificationsDisabled() {
        return disabled_forever || disabled_until > 0 || no_sound;

    }
}
