package ru.melodin.fast.api

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import androidx.annotation.NonNull
import org.jetbrains.annotations.Contract
import ru.melodin.fast.R
import ru.melodin.fast.api.model.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.util.ArrayUtil
import java.util.*

object VKUtil {

    fun getActionBody(@NonNull msg: VKMessage, fromDialogs: Boolean): String? {
        val name: String?

        name = if (fromDialogs) {
            ""
        } else {
            if (msg.isFromGroup) {
                val group = CacheStorage.getGroup(VKGroup.toGroupId(msg.fromId))
                group?.name
            } else {
                val u = CacheStorage.getUser(msg.fromId)
                u?.toString()
            }
        }

        val actionName: String?

        actionName = if (VKGroup.isGroupId(msg.actionId)) {
            val group = CacheStorage.getGroup(VKGroup.toGroupId(msg.actionId))
            group?.name
        } else {
            val actionUser = CacheStorage.getUser(msg.actionId)
            actionUser?.toString()
        }

        when (msg.action) {
            null -> return null
            VKMessage.Action.CREATE -> return String.format(getString(R.string.created_chat_m), name, "«" + msg.actionText + "»")
            VKMessage.Action.INVITE_USER -> return if (msg.actionId == msg.fromId) {
                String.format(getString(R.string.returned_to_chat_m), name!!)
            } else {
                String.format(getString(R.string.invited_to_chat_m), name, actionName)
            }
            VKMessage.Action.KICK_USER -> return if (msg.actionId == msg.fromId) {
                String.format(getString(R.string.left_the_chat_m), name!!)
            } else {
                String.format(getString(R.string.kicked_from_chat_m), name, actionName)
            }
            VKMessage.Action.PHOTO_REMOVE -> return String.format(getString(R.string.remove_chat_photo_m), name!!)
            VKMessage.Action.PHOTO_UPDATE -> return String.format(getString(R.string.updated_chat_photo_m), name!!)
            VKMessage.Action.TITLE_UPDATE -> return String.format(getString(R.string.updated_title_m), name, "«" + msg.actionText + "»")
            VKMessage.Action.INVITE_USER_BY_LINK -> return String.format(getString(R.string.invited_by_link_m), name!!)
            VKMessage.Action.PIN_MESSAGE -> return String.format(getString(R.string.pinned_message_m), name!!)
            VKMessage.Action.UNPIN_MESSAGE -> return String.format(getString(R.string.unpinned_message_m), name!!)
        }

    }

    @SuppressLint("DefaultLocale")
    fun getAttachmentBody(attachments: ArrayList<VKModel>, forwards: ArrayList<VKMessage>): String {
        if (ArrayUtil.isEmpty(attachments) && ArrayUtil.isEmpty(forwards)) {
            return ""
        }

        var s = ""

        if (!ArrayUtil.isEmpty(attachments)) {
            s = VKAttachments.getAttachmentString(attachments)
            return s
        }

        if (!ArrayUtil.isEmpty(forwards) && TextUtils.isEmpty(s)) {
            s = if (forwards.size > 1)
                forwards.size.toString() + " " + getString(R.string.forwarded_messages).toLowerCase()
            else
                getString(R.string.forwarded_messages)
        }

        return s
    }

    @Contract("_, null -> null; null, !null -> null")
    fun getGroupStringType(context: Context?, type: VKGroup.Type?): String? {
        if (type == null || context == null) return null

        return when (type) {
            VKGroup.Type.EVENT -> context.getString(R.string.event)
            VKGroup.Type.PAGE -> context.getString(R.string.page)
            VKGroup.Type.GROUP -> context.getString(R.string.group)
        }
    }

    fun getErrorReason(reason: VKConversation.Reason): String {
        return when (reason) {
            VKConversation.Reason.USER_BLACKLIST -> getString(R.string.user_in_blacklist)
            VKConversation.Reason.USER_PRIVACY -> getString(R.string.user_strict_messaging)
            VKConversation.Reason.USER_DELETED -> getString(R.string.user_blocked_or_deleted)
            VKConversation.Reason.LEFT -> getString(R.string.you_left_this_chat)
            VKConversation.Reason.KICKED -> getString(R.string.kicked_out_text)
            else -> getString(R.string.messaging_restricted)
        }
    }

    private fun getString(res: Int): String {
        return AppGlobal.context.getString(res)
    }

    fun getPhoto100(conversation: VKConversation, user: VKUser?, group: VKGroup?): String? {
        val fromId = conversation.last.fromId

        return if (fromId < 0) {
            group?.photo100
        } else {
            if (conversation.last.isOut && !conversation.isChat) if (UserConfig.getUser() != null) UserConfig.getUser()!!.photo100 else null else user?.photo100
        }
    }
}
