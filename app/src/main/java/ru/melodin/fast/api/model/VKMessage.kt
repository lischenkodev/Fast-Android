package ru.melodin.fast.api.model

import org.jetbrains.annotations.Contract
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKMessage : VKModel, Serializable {

    var action: Action? = null
    var actionId: Int = 0
    var id: Int = 0
    var peerId: Int = 0
    var fromId: Int = 0
    var randomId: Int = 0
    var flags: Int = 0
    var conversationMessageId: Int = 0
    var type: String? = null
    var date: Long = 0
    var actionText: String? = null
    var text: String? = null
    var isRead: Boolean = false
    var isOut: Boolean = false
    var isImportant: Boolean = false
    var unread: Int = 0
    var attachments = ArrayList<VKModel>()
    var fwdMessages = ArrayList<VKMessage>()
    var reply: VKReplyMessage? = null
    var updateTime: Long = 0
    var isAdded: Boolean = false
    var status = Status.SENT
    var isNeedUpdate: Boolean = false
    var isPlaying: Boolean = false

    val isChat: Boolean
        get() = peerId > 2000000000

    val isFromGroup: Boolean
        get() = VKGroup.isGroupId(fromId)

    val isGroup: Boolean
        get() = VKGroup.isGroupId(peerId)

    val isUser: Boolean
        get() = !isGroup && !isChat

    val isFromUser: Boolean
        get() = !isFromGroup

    constructor()

    constructor(o: JSONObject) {
        this.date = o.optLong("date", -1)
        this.fromId = o.optInt("from_id", -1)
        this.peerId = o.optInt("peer_id", -1)
        this.id = o.optInt("id", -1)
        this.isOut = this.fromId == UserConfig.userId
        this.text = o.optString("text")
        this.conversationMessageId = o.optInt("conversation_message_id", -1)
        this.isImportant = o.optBoolean("important")
        this.randomId = o.optInt("random_id", -1)
        this.updateTime = o.optLong("update_time", -1)

        val action = o.optJSONObject("action")
        if (action != null) {
            this.actionId = action.optInt("member_id", -1)
            this.action = getAction(action.optString("type"))
            this.actionText = action.optString("text")
        }

        val reply = o.optJSONObject("reply_message")

        if (reply != null) {
            this.reply = VKReplyMessage(reply)
        }

        val fwdMessages = o.optJSONArray("fwd_messages")

        if (!ArrayUtil.isEmpty(fwdMessages)) {
            this.fwdMessages = parseForwarded(fwdMessages!!)
        }

        val attachments = o.optJSONArray("attachments")

        if (!ArrayUtil.isEmpty(attachments)) {
            this.attachments = VKAttachments.parse(attachments!!)
        }
    }

    enum class Action {
        CREATE, INVITE_USER, KICK_USER, TITLE_UPDATE, PHOTO_UPDATE, PHOTO_REMOVE, PIN_MESSAGE, UNPIN_MESSAGE, INVITE_USER_BY_LINK
    }

    enum class Status {
        SENDING, SENT, ERROR
    }

    companion object {

        const val UNREAD = 1         //сообщение не прочитано
        const val OUTBOX = 2         //исходящее сообщение
        const val REPLIED = 4        //на сообщение был создан ответ
        const val IMPORTANT = 8      //помеченное сообщение
        const val CHAT = 16          //сообщение отправлено через диалог
        const val FRIENDS = 32       //сообщение отправлено другом
        const val SPAM = 64          //сообщение помечено как "Спам"
        const val DELETED = 128      //сообщение удалено (в корзине)
        const val FIXED = 256        //сообщение проверено пользователем на спам
        const val MEDIA = 512        //сообщение содержит медиаконтент
        const val BESEDA = 8192      //беседа

        private const val serialVersionUID = 1L

        var count: Int = 0
        var lastHistoryCount: Int = 0

        var users = ArrayList<VKUser>()
        var groups = ArrayList<VKGroup>()

        @Contract(pure = true)
        fun getStatus(status: String?): Status? {
            return when (status) {
                null -> Status.SENT
                "sending" -> Status.SENDING
                "sent" -> Status.SENT
                "error" -> Status.ERROR
                else -> Status.SENT
            }
        }

        @Contract(pure = true)
        fun getStatus(status: Status?): String? {
            return when (status) {
                null -> "sent"
                Status.SENT -> "sent"
                Status.ERROR -> "error"
                Status.SENDING -> "sending"
            }
        }

        @Contract(pure = true)
        fun getAction(action: String?): Action? {
            return when (action) {
                "chat_create" -> Action.CREATE
                "chat_invite_user" -> Action.INVITE_USER
                "chat_kick_user" -> Action.KICK_USER
                "chat_title_update" -> Action.TITLE_UPDATE
                "chat_photo_update" -> Action.PHOTO_UPDATE
                "chat_photo_remove" -> Action.PHOTO_REMOVE
                "chat_pin_message" -> Action.PIN_MESSAGE
                "chat_unpin_message" -> Action.UNPIN_MESSAGE
                "chat_invite_user_by_link" -> Action.INVITE_USER_BY_LINK
                else -> null
            }
        }

        fun getAction(action: Action?): String? {
            return when (action) {
                null -> null
                Action.CREATE -> "chat_create"
                Action.INVITE_USER -> "chat_invite_user"
                Action.KICK_USER -> "chat_kick_user"
                Action.TITLE_UPDATE -> "chat_title_update"
                Action.PHOTO_UPDATE -> "chat_photo_update"
                Action.PHOTO_REMOVE -> "chat_photo_remove"
                Action.PIN_MESSAGE -> "chat_pin_message"
                Action.UNPIN_MESSAGE -> "chat_unpin_message"
                Action.INVITE_USER_BY_LINK -> "chat_invite_user_by_link"
            }
        }

        fun isDeleted(flags: Int): Boolean {
            return flags and DELETED != 0
        }

        fun isImportant(flags: Int): Boolean {
            return flags and IMPORTANT != 0
        }

        fun isUnread(flags: Int): Boolean {
            return flags and UNREAD != 0
        }

        @Throws(JSONException::class)
        private fun parseForwarded(a: JSONArray): ArrayList<VKMessage> {
            val forwarded = ArrayList<VKMessage>()

            for (i in 0 until a.length()) {
                forwarded.add(VKMessage(a.optJSONObject(i)))
            }

            return forwarded
        }
    }
}
