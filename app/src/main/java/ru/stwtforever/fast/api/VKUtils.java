package ru.stwtforever.fast.api;

import android.content.*;
import android.text.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.model.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class VKUtils {

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

    private static String pattern_string_profile_id = "^(id)?(\\d{1,10})$";
    private static Pattern pattern_profile_id = Pattern.compile(pattern_string_profile_id);

    public static String parseProfileId(String text) {
        Matcher m = pattern_profile_id.matcher(text);
        if (!m.find())
            return null;
        return m.group(2);
    }
	
	public static String getActionBody(VKMessage msg) {
		String action = "<b>";

		VKUser u = CacheStorage.getUser(msg.fromId);
		if (u != null) {
			action += u.toString();
		}

		action += ("</b>");

		VKUser action_user = CacheStorage.getUser(msg.actionUserId);

		String u_name = null;

		if (action_user != null) {
			u_name = action_user.toString();
		} else {
			VKGroup group = CacheStorage.getGroup(VKGroup.toGroupId(msg.actionUserId));
			if (group != null) {
				u_name = group.name;
			}
		}

		switch (msg.actionType) {
			case VKMessage.ACTION_CHAT_CREATE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.created_chat_w : R.string.created_chat_m : R.string.created_chat_m), action, " «<b>" + msg.actionText + "</b>»");
				break;
			case VKMessage.ACTION_CHAT_INVITE_USER:
				if (msg.actionUserId == msg.fromId) {
					action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.returned_to_chat_w : R.string.returned_to_chat_m : R.string.returned_to_chat_m), action);
				} else {
					action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.invited_to_chat_w : R.string.invited_to_chat_m : R.string.invited_to_chat_m), action, "<b>" + u_name + "</b>");
				}

				break;
			case VKMessage.ACTION_CHAT_KICK_USER:
				if (msg.actionUserId == msg.fromId) {
					action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.left_the_chat_w : R.string.left_the_chat_m : R.string.left_the_chat_m), action);
				} else {
					action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.kicked_from_chat_w : R.string.kicked_from_chat_m : R.string.kicked_from_chat_m), action, "<b>" + u_name + "</b>");
				}
				break;
			case VKMessage.ACTION_CHAT_PHOTO_REMOVE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.remove_chat_photo_w : R.string.remove_chat_photo_m : R.string.remove_chat_photo_m), action);
				break;
			case VKMessage.ACTION_CHAT_PHOTO_UPDATE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.updated_chat_photo_w : R.string.updated_chat_photo_m : R.string.updated_chat_photo_m), action);
				break;
			case VKMessage.ACTION_CHAT_TITLE_UPDATE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.updated_title_w : R.string.updated_title_m : R.string.updated_title_m), action, "«<b>" + msg.actionText + "</b>»");
				break;
			case VKMessage.ACTION_CHAT_INVITE_USER_BY_LINK:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.invited_by_link_w : R.string.invited_by_link_m : R.string.invited_by_link_m), action);
				break;
			case VKMessage.ACTION_CHAT_PIN_MESSAGE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.pinned_message_w : R.string.pinned_message_m : R.string.pinned_message_m), action);
				break;
			case VKMessage.ACTION_CHAT_UNPIN_MESSAGE:
				action = String.format(getString(u != null ? u.sex == VKUser.Sex.FEMALE ? R.string.unpinned_message_w : R.string.unpinned_message_m : R.string.unpinned_message_m), action);
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
	
	public static String getErrorReason(int reason) {
		String s = "";
		
		switch (reason) {
			case VKConversation.REASON_CANT_SEND_MESSAGE_USER_WHICH_IN_BLACKLIST:
				s = getString(R.string.user_in_blacklist);
				break;
			case VKConversation.REASON_CANT_SEND_USER_PRIVACY:
				s = getString(R.string.user_strict_messaging);
				break;
			case VKConversation.REASON_USER_BLOCKED_DELETED:
				s = getString(R.string.user_blocked_or_deleted);
				break;
			case VKConversation.REASON_LEFT:
				s = getString(R.string.you_left_this_chat);
				break;
			case VKConversation.REASON_KICKED:
				s = getString(R.string.kicked_out_text);
				break;
			default:
				s = getString(R.string.messaging_restricted);
				break;
		}
		
		return s;
	}
	
	private static String getString(int res) {
		return AppGlobal.context.getString(res);
	}
}
