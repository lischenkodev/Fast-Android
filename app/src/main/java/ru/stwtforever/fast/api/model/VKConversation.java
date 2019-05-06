package ru.stwtforever.fast.api.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.util.StringUtils;

public class VKConversation extends VKModel implements Serializable {

    public static int count;
    public static ArrayList<VKUser> users = new ArrayList<>();
    public static ArrayList<VKGroup> groups = new ArrayList<>();

    public int conversations_count;

    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_USER = "user";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_EMAIL = "email";

    public static final int REASON_KICKED = 1;
    public static final int REASON_LEFT = 2;
    public static final int REASON_USER_BLOCKED_DELETED = 18;
    public static final int REASON_CANT_SEND_MESSAGE_USER_WHICH_IN_BLACKLIST = 900;
    public static final int REASON_CANT_SEND_USER_PRIVACY = 902;
    public static final int REASON_GROUP_MESSAGES_OFF = 915;

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
    public String type;
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

    public VKConversation() {

    }

    public VKConversation(JSONObject o, JSONObject msg) throws JSONException {
        conversations_count = count;
        conversation_groups = groups;
        conversation_users = users;

        last = new VKMessage(msg);

        read_in = o.optInt("in_read");
        read_out = o.optInt("out_read");
        last_mId = o.optInt("last_message_id");
        unread = o.optInt("unread_count");

        read = last.out && read_out == last_mId || !last.out && read_in == last_mId;

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

        JSONObject peer = o.optJSONObject("peer");
        type = peer.optString("type");

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

    public static VKConversation parseFromLongPoll(JSONArray a) {
        VKConversation conversation = new VKConversation();
        VKMessage last = new VKMessage();
        last.id = a.optInt(1);
        last.flag = a.optInt(2);
        last.peerId = a.optInt(3);
        last.date = a.optLong(4);
        last.text = StringUtils.unescape(a.optString(5));
        conversation.read = ((last.flag & VKMessage.UNREAD) == 0);
        last.read = conversation.read;
        last.out = (last.flag & VKMessage.OUTBOX) != 0;
        last.fromId = last.out ? UserConfig.userId : last.peerId;

        if ((last.flag & VKMessage.BESEDA) != 0) {
            JSONObject o = a.optJSONObject(6);
            last.fromId = o.optInt("from");
        }

        last.randomId = a.optInt(8);

        conversation.type = getType(last.peerId);

        JSONObject attachments = a.optJSONObject(7);

        if (attachments != null && attachments.length() > 0) {
            last.attachments = VKAttachments.parseFromLongPoll(attachments);
        }

        conversation.last = last;

        Log.d("FVKConversation", a.toString());

        return conversation;
    }

    private static String getType(int peerId) {
        if (peerId > 2_000_000_00) return TYPE_CHAT;
        if (VKGroup.isGroupId(peerId)) return TYPE_GROUP;
        return TYPE_USER;
    }

    public boolean isChat() {
        return last.peerId > 2_000_000_000;
    }

    public boolean isFromGroup() {
        return VKGroup.isGroupId(last.fromId);
    }

    public boolean isGroup() {
        return VKGroup.isGroupId(last.peerId);
    }

    public boolean isUser() {
        return !isGroup() && !isChat() && !isChannel();
    }

    public boolean isFromUser() {
        return !isFromGroup();
    }

    public boolean isChannel() {
        return group_channel;
    }

    public boolean isNotificationsDisabled() {
        return disabled_forever || disabled_until > 0 || no_sound;

    }
}
