package ru.melodin.fast.api;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

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
import ru.melodin.fast.database.CacheStorage;
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

    public static String getActionBody(@NonNull VKMessage msg, boolean fromDialogs) {
        String name;

        if (fromDialogs) {
            name = "";
        } else {
            if (msg.isFromGroup()) {
                VKGroup group = CacheStorage.getGroup(VKGroup.toGroupId(msg.getFromId()));
                name = group != null ? group.getName() : null;
            } else {
                VKUser u = CacheStorage.getUser(msg.getFromId());
                name = u != null ? u.toString() : null;
            }
        }

        String actionName;

        if (VKGroup.isGroupId(msg.getActionId())) {
            VKGroup group = CacheStorage.getGroup(VKGroup.toGroupId(msg.getActionId()));
            actionName = group != null ? group.getName() : null;
        } else {
            VKUser actionUser = CacheStorage.getUser(msg.getActionId());
            actionName = actionUser != null ? actionUser.toString() : null;
        }

        switch (msg.getAction()) {
            case CREATE:
                return String.format(getString(R.string.created_chat_m), name, "«" + msg.getActionText() + "»");
            case INVITE_USER:
                if (msg.getActionId() == msg.getFromId()) {
                    return String.format(getString(R.string.returned_to_chat_m), name);
                } else {
                    return String.format(getString(R.string.invited_to_chat_m), name, actionName);
                }
            case KICK_USER:
                if (msg.getActionId() == msg.getFromId()) {
                    return String.format(getString(R.string.left_the_chat_m), name);
                } else {
                    return String.format(getString(R.string.kicked_from_chat_m), name, actionName);
                }
            case PHOTO_REMOVE:
                return String.format(getString(R.string.remove_chat_photo_m), name);
            case PHOTO_UPDATE:
                return String.format(getString(R.string.updated_chat_photo_m), name);
            case TITLE_UPDATE:
                return String.format(getString(R.string.updated_title_m), name, "«" + msg.getActionText() + "»");
            case INVITE_USER_BY_LINK:
                return String.format(getString(R.string.invited_by_link_m), name);
            case PIN_MESSAGE:
                return String.format(getString(R.string.pinned_message_m), name);
            case UNPIN_MESSAGE:
                return String.format(getString(R.string.unpinned_message_m), name);
        }

        return null;
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

    @Contract("_, null -> null; null, !null -> null")
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

    @NonNull
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

    @NonNull
    private static String getString(int res) {
        return AppGlobal.getContext().getString(res);
    }

    @Nullable
    public static String getPhoto200(@NonNull VKConversation conversation, VKUser user, VKGroup group) {
        int peerId = conversation.getPeerId();

        if (peerId > 2_000_000_000) {
            return conversation.getPhoto200();
        } else if (peerId < 0) {
            return group == null ? null : group.getPhoto200();
        } else {
            return user == null ? null : user.getPhoto200();
        }
    }

    @Nullable
    public static String getPhoto100(@NonNull VKConversation conversation, VKUser user, VKGroup group) {
        int fromId = conversation.getLast().getFromId();

        if (fromId < 0) {
            return group == null ? null : group.getPhoto100();
        } else {
            return conversation.getLast().isOut() && !conversation.isChat() ? UserConfig.user.getPhoto100() : user == null ? null : user.getPhoto100();
        }
    }

}
