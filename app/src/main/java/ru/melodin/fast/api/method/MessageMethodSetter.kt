package ru.melodin.fast.api.method

import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil


class MessageMethodSetter(name: String) : MethodSetter("messages.$name") {

    fun out(value: Boolean): MessageMethodSetter {
        put("out", value)
        return this
    }

    fun timeOffset(value: Int): MessageMethodSetter {
        put("time_offset", value)
        return this
    }

    fun filters(value: Int): MessageMethodSetter {
        put("filters", value)
        return this
    }

    fun keepForwardMessages(keep: Boolean): MessageMethodSetter {
        put("keep_forward_messages", if (keep) 1 else 0)
        return this
    }

    fun keepSnippets(keep: Boolean): MessageMethodSetter {
        put("keep_snippets", if (keep) 1 else 0)
        return this
    }

    fun filter(filter: String): MessageMethodSetter {
        put("filter", filter)
        return this
    }

    fun dontParseLinks(dont_parse: Boolean): MessageMethodSetter {
        put("dont_parse_links", dont_parse)
        return this
    }

    fun previewLength(value: Int): MessageMethodSetter {
        put("preview_length", value)
        return this
    }

    fun lastMessageId(value: Int): MessageMethodSetter {
        put("last_message_id", value)
        return this
    }

    override fun extended(extended: Boolean): MessageMethodSetter {
        put("extended", if (extended) 1 else 0)
        return this
    }

    fun unread(value: Boolean): MessageMethodSetter {
        put("unread", value)
        return this
    }

    fun messageIds(vararg ids: Int): MessageMethodSetter {
        return put("message_ids", ArrayUtil.toString(*ids)) as MessageMethodSetter
    }

    fun messageIds(id: Int): MessageMethodSetter {
        return put("message_ids", id) as MessageMethodSetter
    }

    fun messageIds(messages: ArrayList<VKMessage>): MessageMethodSetter {
        val ids = arrayListOf<Int>()

        for (message in messages)
            ids.add(message.id)

        return put("message_ids", ArrayUtil.toString(ids)) as MessageMethodSetter
    }

    fun every(every: Boolean?): MessageMethodSetter {
        if (every != null) {
            put("delete_for_all", every)
        }
        return this
    }

    fun spam(spam: Boolean?): MessageMethodSetter {
        if (spam != null) {
            put("spam", spam)
        }

        return this
    }

    fun q(query: String): MessageMethodSetter {
        put("q", query)
        return this
    }

    fun startMessageId(id: Int): MessageMethodSetter {
        put("start_message_id", id)
        return this
    }

    fun peerId(value: Long): MessageMethodSetter {
        put("peer_id", value)
        return this
    }

    fun peerIds(vararg value: Int): MessageMethodSetter {
        put("peer_ids", ArrayUtil.toString(*value))
        return this
    }


    fun peerIds(value: Int): MessageMethodSetter {
        put("peer_ids", value)
        return this
    }

    fun rev(value: Boolean): MessageMethodSetter {
        put("rev", value)
        return this
    }

    fun domain(value: String): MessageMethodSetter {
        put("domain", value)
        return this
    }

    fun chatId(value: Int): MessageMethodSetter {
        put("chat_id", value)
        return this
    }

    fun message(message: String): MessageMethodSetter {
        put("message", message)
        return this
    }

    fun randomId(value: Int): MessageMethodSetter {
        put("random_id", value)
        return this
    }

    fun lat(lat: Double): MessageMethodSetter {
        put("lat", lat)
        return this
    }

    fun longitude(value: Long): MessageMethodSetter {
        put("LONG", value)
        return this
    }

    fun attachment(attachments: ArrayList<VKModel>): MessageMethodSetter {
        put("attachment", ArrayUtil.toString(attachments))
        return this
    }

    fun forwardMessages(vararg ids: Int): MessageMethodSetter {
        return put("forward_messages", ArrayUtil.toString(*ids)) as MessageMethodSetter
    }

    fun forwardMessages(messages: ArrayList<VKMessage>): MessageMethodSetter {
        val ids = arrayListOf<Int>()
        for (m in messages)
            ids.add(m.id)
        return put("forward_messages", ArrayUtil.toString(ids)) as MessageMethodSetter
    }

    fun stickerId(value: Int): MessageMethodSetter {
        put("sticker_id", value)
        return this
    }

    fun messageId(value: Int): MessageMethodSetter {
        put("message_id", value)
        return this
    }

    fun important(value: Boolean): MessageMethodSetter {
        put("important", value)
        return this
    }

    fun ts(value: Long): MessageMethodSetter {
        put("ts", value)
        return this
    }

    fun pts(value: Int): MessageMethodSetter {
        put("pts", value)
        return this
    }

    fun title(title: String): MessageMethodSetter {
        put("title", title)
        return this
    }

    fun type(typing: Boolean): MessageMethodSetter {
        return put("type", if (typing) "typing" else "audiomessage") as MessageMethodSetter
    }

    fun mediaType(type: String): MessageMethodSetter {
        put("media_type", type)
        return this
    }

    fun photoSizes(value: Boolean): MessageMethodSetter {
        put("photo_sizes", value)
        return this
    }

    fun replyTo(value: Int): MessageMethodSetter {
        return put("reply_to", value) as MessageMethodSetter
    }
}
