package ru.melodin.fast.api.method;

import java.util.ArrayList;
import java.util.Collection;

import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.util.ArrayUtil;

/**
 * Method setter for users
 */

public class MessageMethodSetter extends MethodSetter {

    public MessageMethodSetter(String name) {
        super("messages." + name);
    }

    public MessageMethodSetter out(boolean value) {
        put("out", value);
        return this;
    }

    public MessageMethodSetter timeOffset(int value) {
        put("time_offset", value);
        return this;
    }

    public MessageMethodSetter filters(int value) {
        put("filters", value);
        return this;
    }

    public MessageMethodSetter keepForwardMessages(boolean keep) {
        put("keep_forward_messages", keep ? 1 : 0);
        return this;
    }

    public MessageMethodSetter keepSnippets(boolean keep) {
        put("keep_snippets", keep ? 1 : 0);
        return this;
    }

    public MessageMethodSetter filter(String filter) {
        put("filter", filter);
        return this;
    }

    public MessageMethodSetter dontParseLinks(boolean dont_parse) {
        put("dont_parse_links", dont_parse);
        return this;
    }

    public MessageMethodSetter previewLength(int value) {
        put("preview_length", value);
        return this;
    }

    public MessageMethodSetter lastMessageId(int value) {
        put("last_message_id", value);
        return this;
    }

    public MessageMethodSetter extended(boolean extended) {
        put("extended", extended ? 1 : 0);
        return this;
    }

    public MessageMethodSetter unread(boolean value) {
        put("unread", value);
        return this;
    }

    public MessageMethodSetter messageIds(int... ids) {
        put("message_ids", ArrayUtil.toString(ids));
        return this;
    }

    public MessageMethodSetter messageIds(int id) {
        put("message_ids", id);
        return this;
    }

    public MessageMethodSetter every(Boolean every) {
        if (every != null) {
            put("delete_for_all", every);
        }
        return this;
    }

    public MessageMethodSetter spam(Boolean spam) {
        if (spam != null) {
            put("spam", spam);
        }

        return this;
    }

    public MessageMethodSetter q(String query) {
        put("q", query);
        return this;
    }

    public MessageMethodSetter startMessageId(int id) {
        put("start_message_id", id);
        return this;
    }

    public MessageMethodSetter peerId(long value) {
        put("peer_id", value);
        return this;
    }

    public MessageMethodSetter peerIds(int... value) {
        put("peer_ids", ArrayUtil.toString(value));
        return this;
    }


    public MessageMethodSetter peerIds(int value) {
        put("peer_ids", value);
        return this;
    }

    public MessageMethodSetter rev(boolean value) {
        put("rev", value);
        return this;
    }

    public MessageMethodSetter domain(String value) {
        put("domain", value);
        return this;
    }

    public MessageMethodSetter chatId(int value) {
        put("chat_id", value);
        return this;
    }

    public MessageMethodSetter text(String message) {
        put("message", message);
        return this;
    }

    public MessageMethodSetter randomId(int value) {
        put("random_id", value);
        return this;
    }

    public MessageMethodSetter lat(double lat) {
        put("lat", lat);
        return this;
    }

    public MessageMethodSetter longitude(long value) {
        put("LONG", value);
        return this;
    }

    public final MessageMethodSetter attachment(Collection<String> attachments) {
        put("attachment", ArrayUtil.toString(attachments));
        return this;
    }

    public final MessageMethodSetter attachment(ArrayList<VKModel> attachments) {
        put("attachment", ArrayUtil.toString(attachments));
        return this;
    }

    public final MessageMethodSetter attachment(String... attachments) {
        put("attachment", ArrayUtil.toString(attachments));
        return this;
    }

    public final MessageMethodSetter forwardMessages(Collection<String> ids) {
        put("forward_messages", ArrayUtil.toString(ids));
        return this;
    }

    public final MessageMethodSetter forwardMessages(int... ids) {
        put("forward_messages", ArrayUtil.toString(ids));
        return this;
    }

    public final MessageMethodSetter stickerId(int value) {
        put("sticker_id", value);
        return this;
    }

    public final MessageMethodSetter messageId(int value) {
        put("message_id", value);
        return this;
    }

    public final MessageMethodSetter important(boolean value) {
        put("important", value);
        return this;
    }

    public final MessageMethodSetter ts(long value) {
        put("ts", value);
        return this;
    }

    public final MessageMethodSetter pts(int value) {
        put("pts", value);
        return this;
    }

    public final MessageMethodSetter msgsLimit(int limit) {
        put("msgs_limit", limit);
        return this;
    }

    public final MessageMethodSetter onlines(boolean onlines) {
        put("onlines", onlines);
        return this;
    }

    public final MessageMethodSetter maxMsgId(int id) {
        put("max_msg_id", id);
        return this;
    }

    public final MessageMethodSetter chatIds(int... ids) {
        put("max_msg_id", ArrayUtil.toString(ids));
        return this;
    }

    public final MessageMethodSetter chatIds(Collection<Integer> ids) {
        put("max_msg_id", ArrayUtil.toString(ids));
        return this;
    }

    public final MessageMethodSetter title(String title) {
        put("title", title);
        return this;
    }

    public final MessageMethodSetter type(boolean typing) {
        if (typing) {
            put("type", "typing");
        }
        return this;
    }

    public final MessageMethodSetter mediaType(String type) {
        put("media_type", type);
        return this;
    }

    public final MessageMethodSetter photoSizes(boolean value) {
        put("photo_sizes", value);
        return this;
    }
}
