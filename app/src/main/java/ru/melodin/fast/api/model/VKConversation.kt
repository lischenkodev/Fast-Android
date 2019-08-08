package ru.melodin.fast.api.model

import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.jetbrains.annotations.Contract
import org.json.JSONException
import org.json.JSONObject
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.util.Keys
import java.io.Serializable
import java.util.*

class VKConversation : VKModel, Serializable {

    var peerId = 0
    var ownerId = 0
    var localId = 0
    var readIn = 0
    var readOut = 0
    var lastMessageId = 0
    var unread = 0
    var membersCount = 0

    var isCanWrite: Boolean = false
    var reason = 0

    var isRead: Boolean = false
    var isGroupChannel: Boolean = false

    var pinned: VKMessage? = null
    var lastMessage: VKMessage? = null

    var title: String? = null
    var type: Type? = null
    var state: State? = State.IN
    var photo50: String? = null
    var photo100: String? = null
    var photo200: String? = null

    //acl
    var isCanChangePin: Boolean = false
    var isCanChangeInfo: Boolean = false
    var isCanChangeInviteLink: Boolean = false
    var isCanInvite: Boolean = false
    var isCanPromoteUsers: Boolean = false
    var isCanSeeInviteLink: Boolean = false
    var isCanModerate: Boolean = false

    //push_settings
    var disabledUntil: Int = 0
    var isDisabledForever: Boolean = false
    var isNoSound: Boolean = false

    val fullTitle: String?
        get() {
            val peerId = peerId

            val group: VKGroup?
            val user: VKUser?

            when (type) {
                Type.GROUP -> {
                    group = CacheStorage.getGroup(VKGroup.toGroupId(peerId))
                    return if (group == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        group.toString()
                    }
                }
                Type.USER -> {
                    user = CacheStorage.getUser(peerId)
                    return if (user == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        user.toString()
                    }
                }
                Type.CHAT -> return if (isGroupChannel) {
                    group = CacheStorage.getGroup(VKGroup.toGroupId(ownerId))
                    if (group == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        group.toString()
                    }
                } else {
                    toString()
                }
            }

            return null
        }

    val photo: String?
        get() {
            val peerId = peerId

            val group: VKGroup?
            val user: VKUser?

            when (type) {
                Type.GROUP -> {
                    group = MemoryCache.getGroup(VKGroup.toGroupId(peerId))
                    return if (group == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        group.photo200
                    }
                }
                Type.USER -> {
                    user = MemoryCache.getUser(peerId)
                    return if (user == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        user.photo200
                    }
                }
                Type.CHAT -> return if (isGroupChannel) {
                    group = MemoryCache.getGroup(VKGroup.toGroupId(ownerId))
                    if (group == null) {
                        EventBus.getDefault()
                            .postSticky(arrayOf(Keys.NEED_LOAD_ID, peerId, javaClass.simpleName))
                        null
                    } else {
                        group.photo200
                    }
                } else {
                    photo200
                }
            }

            return null
        }

    val isChat: Boolean
        get() = type == Type.CHAT && !isGroupChannel

    val isFromGroup: Boolean
        get() = VKGroup.isGroupId(this.lastMessage!!.fromId)

    val isGroup: Boolean
        get() = type == Type.GROUP && !isGroupChannel

    val isFromUser: Boolean
        get() = !isFromGroup

    val isNotificationsDisabled: Boolean
        get() = this.isDisabledForever || this.disabledUntil > 0 || this.isNoSound

    constructor()

    @Throws(JSONException::class)
    constructor(o: JSONObject, msg: JSONObject?) {
        Log.d("FVKConversation", o.toString())

        if (msg != null)
            lastMessage = VKMessage(msg)

        val peer = o.optJSONObject("peer")
        this.peerId = peer!!.optInt("id", -1)
        this.localId = peer.optInt("local_id", -1)

        val type = peer.optString("type")
        this.type = getType(type)

        this.readIn = o.optInt("in_read")
        this.readOut = o.optInt("out_read")
        this.lastMessageId = o.optInt("last_message_id", -1)
        this.unread = o.optInt("unread_count")

        this.isRead =
            this.lastMessage == null || this.lastMessage!!.isOut && this.readOut == this.lastMessageId || !this.lastMessage!!.isOut && this.readIn == this.lastMessageId

        val canWrite = o.optJSONObject("can_write")
        this.isCanWrite = canWrite!!.optBoolean("allowed")
        if (!this.isCanWrite) {
            this.reason = canWrite.optInt("reason", -1)
        }

        val pushSettings = o.optJSONObject("push_settings")
        if (pushSettings != null) {
            this.disabledUntil = pushSettings.optInt("disabled_until", -1)
            this.isDisabledForever = pushSettings.optBoolean("disabled_forever", false)
            this.isNoSound = pushSettings.optBoolean("no_sound")
        }

        val ch = o.optJSONObject("chat_settings")
        if (ch != null) {
            this.ownerId = ch.optInt("owner_id", -1)
            this.title = ch.optString("title")
            this.membersCount = ch.optInt("members_count")
            this.state = getState(ch.optString("state"))
            this.isGroupChannel = ch.optBoolean("is_group_channel")

            val photo = ch.optJSONObject("photo")
            if (photo != null) {
                this.photo50 = photo.optString("photo_50")
                this.photo100 = photo.optString("photo_100")
                this.photo200 = photo.optString("photo_200")
            }

            val acl = ch.optJSONObject("acl")
            if (acl != null) {
                this.isCanInvite = acl.optBoolean("can_invite")
                this.isCanPromoteUsers = acl.optBoolean("can_promote_users")
                this.isCanSeeInviteLink = acl.optBoolean("can_see_invite_link")
                this.isCanChangeInviteLink = acl.optBoolean("can_change_invite_link")
                this.isCanChangeInfo = acl.optBoolean("can_change_info")
                this.isCanChangePin = acl.optBoolean("can_change_pin")
                this.isCanModerate = acl.optBoolean("can_moderate")
            }

            val pinned = ch.optJSONObject("pinned_message")
            if (pinned != null) {
                this.pinned = VKMessage(pinned)
            }
        }
    }

    override fun toString(): String {
        return title ?: ""
    }

    enum class State {
        IN, KICKED, LEFT
    }

    enum class Type {
        CHAT, GROUP, USER
    }

    enum class Reason {
        KICKED, LEFT, USER_DELETED, USER_BLACKLIST, USER_PRIVACY, MESSAGES_OFF, MESSAGES_BLOCKED, NO_ACCESS_CHAT, NO_ACCESS_EMAIL, NO_ACCESS_GROUP, UNKNOWN
    }

    companion object {

        private const val serialVersionUID = 1L

        var count: Int = 0
        var users = ArrayList<VKUser>()
        var groups = ArrayList<VKGroup>()

        fun getState(state: String?): State? {
            return when (state) {
                null -> null
                "kicked" -> return State.KICKED
                "in" -> return State.IN
                "left" -> return State.LEFT
                else -> null
            }
        }

        fun getState(state: State?): String? {
            if (state == null) return null
            return when (state) {
                State.KICKED -> "kicked"
                State.LEFT -> "left"
                State.IN -> "in"
            }
        }

        fun getReason(reason: Reason): Int {
            return when (reason) {
                Reason.USER_BLACKLIST -> 900
                Reason.USER_PRIVACY -> 902
                Reason.USER_DELETED -> 18
                Reason.LEFT -> 2
                Reason.KICKED -> 1
                Reason.MESSAGES_OFF -> 915
                Reason.MESSAGES_BLOCKED -> 916
                Reason.NO_ACCESS_CHAT -> 917
                Reason.NO_ACCESS_EMAIL -> 918
                Reason.NO_ACCESS_GROUP -> 203
                else -> -1
            }
        }

        fun getReason(reason: Int): Reason {
            return when (reason) {
                900 -> Reason.USER_BLACKLIST
                902 -> Reason.USER_PRIVACY
                18 -> Reason.USER_DELETED
                2 -> Reason.LEFT
                1 -> Reason.KICKED
                915 -> Reason.MESSAGES_OFF
                916 -> Reason.MESSAGES_BLOCKED
                917 -> Reason.NO_ACCESS_CHAT
                918 -> Reason.NO_ACCESS_EMAIL
                203 -> Reason.NO_ACCESS_GROUP
                else -> Reason.UNKNOWN
            }
        }

        fun getType(peerId: Int): Type {
            if (isChatId(peerId)) return Type.CHAT
            return if (VKGroup.isGroupId(peerId)) Type.GROUP else Type.USER
        }

        fun getType(type: String?): Type? {
            return when (type) {
                null -> null
                "user" -> Type.USER
                "group" -> Type.GROUP
                "chat" -> Type.CHAT
                else -> null
            }
        }

        @Contract(value = "null -> null", pure = true)
        fun getType(type: Type?): String? {
            return when (type) {
                null -> null
                Type.USER -> "user"
                Type.GROUP -> "group"
                Type.CHAT -> "chat"
            }
        }

        @Contract(pure = true)
        fun isChatId(peerId: Int): Boolean {
            return peerId > 2000000000
        }

        fun toChatId(id: Int): Int {
            return if (id > 2000000000) id - 2000000000 else id
        }
    }
}
