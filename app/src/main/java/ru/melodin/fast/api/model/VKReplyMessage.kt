package ru.melodin.fast.api.model

import org.json.JSONObject
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable

class VKReplyMessage(o: JSONObject) : VKModel(), Serializable {

    var id: Int = 0
    var fromId: Int = 0
    var date: Int = 0
    var peerId: Int = 0
    var conversationMessageId: Int = 0
    var attachments = ArrayList<VKModel>()
    var text: String? = null

    init {
        this.id = o.optInt("id", -1)
        this.fromId = o.optInt("from_id", -1)
        this.peerId = o.optInt("peer_id", -1)
        this.date = o.optInt("date", -1)
        this.conversationMessageId = o.optInt("conversation_message_id", -1)
        this.text = o.optString("text")

        val attachments = o.optJSONArray("attachments")
        if (!ArrayUtil.isEmpty(attachments)) {
            this.attachments = VKAttachments.parse(attachments!!)
        }
    }

    fun asMessage(): VKMessage {
        val message = VKMessage()

        message.id = id
        message.fromId = fromId
        message.peerId = peerId
        message.date = date.toLong()
        message.conversationMessageId = conversationMessageId
        message.text = text
        message.attachments = attachments

        return message
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}
