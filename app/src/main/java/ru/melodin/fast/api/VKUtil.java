package ru.melodin.fast.api;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.melodin.fast.R;
import ru.melodin.fast.api.model.VKAttachments;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;

public class VKUtil {

    private static String pattern_string_profile_id = "^(id)?(\\d{1,10})$";
    private static Pattern pattern_profile_id = Pattern.compile(pattern_string_profile_id);

    public static String extractPattern(String string, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(string);
        if (!m.find())
            return null;
        return m.toMatchResult().group(1);
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        InputStreamReader r = new InputStreamReader(is);
        StringWriter sw = new StringWriter();
        char[] buffer = new char[2048];
        try {
            for (int n; (n = r.read(buffer)) != -1; )
                sw.write(buffer, 0, n);
        } finally {
            try {
                is.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return sw.toString();
    }

    public static void closeStream(Object oin) {
        if (oin != null)
            try {
                if (oin instanceof InputStream)
                    ((InputStream) oin).close();
                if (oin instanceof OutputStream)
                    ((OutputStream) oin).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static String parseProfileId(String text) {
        Matcher m = pattern_profile_id.matcher(text);
        if (!m.find())
            return null;
        return m.group(2);
    }

    public static String getActionBody(VKMessage msg, boolean fromDialogs) {
        String action = "";

        VKUser u = MemoryCache.getUser(msg.getFromId());
        if (u != null) {
            action += u.toString();
        } else {
            VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(msg.getActionUserId()));
            if (group != null) {
                action = group.getName();
            }
        }

        VKUser action_user = MemoryCache.getUser(msg.getActionUserId());

        String u_name = null;

        if (action_user != null) {
            u_name = action_user.toString();
        } else {
            VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(msg.getActionUserId()));
            if (group != null) {
                u_name = group.getName();
            }
        }

        if (fromDialogs)
            action = "";

        switch (msg.getAction()) {
            case CHAT_CREATE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.created_chat_w : R.string.created_chat_m : R.string.created_chat_m), action, "«" + msg.getActionText() + "»");
                break;
            case CHAT_INVITE_USER:
                if (msg.getActionUserId() == msg.getFromId()) {
                    action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.returned_to_chat_w : R.string.returned_to_chat_m : R.string.returned_to_chat_m), action);
                } else {
                    action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.invited_to_chat_w : R.string.invited_to_chat_m : R.string.invited_to_chat_m), action, u_name);
                }

                break;
            case CHAT_KICK_USER:
                if (msg.getActionUserId() == msg.getFromId()) {
                    action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.left_the_chat_w : R.string.left_the_chat_m : R.string.left_the_chat_m), action);
                } else {
                    action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.kicked_from_chat_w : R.string.kicked_from_chat_m : R.string.kicked_from_chat_m), action, u_name);
                }
                break;
            case CHAT_PHOTO_REMOVE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.remove_chat_photo_w : R.string.remove_chat_photo_m : R.string.remove_chat_photo_m), action);
                break;
            case CHAT_PHOTO_UPDATE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.updated_chat_photo_w : R.string.updated_chat_photo_m : R.string.updated_chat_photo_m), action);
                break;
            case CHAT_TITLE_UPDATE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.updated_title_w : R.string.updated_title_m : R.string.updated_title_m), action, "«" + msg.getActionText() + "»");
                break;
            case CHAT_INVITE_USER_BY_LINK:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.invited_by_link_w : R.string.invited_by_link_m : R.string.invited_by_link_m), action);
                break;
            case CHAT_PIN_MESSAGE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.pinned_message_w : R.string.pinned_message_m : R.string.pinned_message_m), action);
                break;
            case CHAT_UNPIN_MESSAGE:
                action = String.format(getString(u != null ? u.getSex() == VKUser.Sex.FEMALE ? R.string.unpinned_message_w : R.string.unpinned_message_m : R.string.unpinned_message_m), action);
                break;
        }

        return action;
    }

    public static String getAttachmentBody(ArrayList<VKModel> attachments, ArrayList<VKMessage> forwards) {
        if (ArrayUtil.isEmpty(attachments) && ArrayUtil.isEmpty(forwards)) {
            return "";
        }

        String s = "";

        if (!ArrayUtil.isEmpty(attachments)) {
            s = VKAttachments.getAttachmentString(attachments);
            return s;
        }

        if (!ArrayUtil.isEmpty(forwards) && TextUtils.isEmpty(s)) {
            s = forwards.size() > 1 ?
                    forwards.size() + " " + getString(R.string.forwarded_messages).toLowerCase() :
                    getString(R.string.forwarded_messages);
        }

        return s;
    }

    public static String getGroupStringType(Context context, VKGroup.Type type) {
        if (type == null || context == null) return null;

        switch (type) {
            case EVENT:
                return context.getString(R.string.event);
            case PAGE:
                return context.getString(R.string.page);
            case GROUP:
                return context.getString(R.string.group);
        }

        return null;
    }

    public static String getErrorReason(VKConversation.Reason reason) {
        switch (reason) {
            case USER_BLACKLIST:
                return getString(R.string.user_in_blacklist);
            case USER_PRIVACY:
                return getString(R.string.user_strict_messaging);
            case USER_DELETED:
                return getString(R.string.user_blocked_or_deleted);
            case LEFT:
                return getString(R.string.you_left_this_chat);
            case KICKED:
                return getString(R.string.kicked_out_text);
            default:
                return getString(R.string.messaging_restricted);
        }
    }

    private static String getString(int res) {
        return AppGlobal.context().getString(res);
    }
}
