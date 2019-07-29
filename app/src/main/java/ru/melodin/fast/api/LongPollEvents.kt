package ru.melodin.fast.api

import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.StringUtils

object LongPollEvents {

    private fun messageEvent(item: JSONArray) {
        Log.d("FVK New Message", item.toString())
        val mId = item.optInt(1)
        val flags = item.optInt(2)
        val peerId = item.optInt(3)
        val date = item.optInt(4)
        val text = StringUtils.unescape(item.optString(5))

        val fromActions = item.optJSONObject(6)
        val attachments = item.optJSONObject(7)

        val randomId = item.optInt(8)
        val conversationMessageId = item.optInt(9)
        val updateTime = item.optInt(10)

        val conversation = VKConversation()
        val last = VKMessage()

        conversation.peerId = peerId

        last.id = mId
        last.flags = flags
        last.peerId = peerId
        last.date = date.toLong()
        last.conversationMessageId = conversationMessageId
        last.updateTime = updateTime.toLong()

        conversation.isRead = last.flags and VKMessage.UNREAD == 0

        last.isRead = conversation.isRead
        last.isOut = last.flags and VKMessage.OUTBOX != 0
        last.fromId = if (fromActions != null && fromActions.has("from")) fromActions.optInt("from", -1) else if (last.isOut) UserConfig.userId else peerId

        if (fromActions != null) {
            val actionType = fromActions.optString("source_act")
            val actionText = fromActions.optString("source_message")
            val actionId = fromActions.optInt("source_mid", -1)

            if (actionId != -1) {
                last.actionId = actionId
                last.action = VKMessage.getAction(actionType)
                last.actionText = actionText
            }
        }

        last.text = if (fromActions != null && fromActions.optInt("source_mid", -1) != -1) "" else text

        if (!ArrayUtil.isEmpty(attachments)) {
            TaskManager.loadMessage(last.id, true, null)
        }

        last.randomId = randomId

        conversation.type = VKConversation.getType(last.peerId)

        conversation.last = last

        EventBus.getDefault().postSticky(arrayOf<Any>(Keys.MESSAGE_NEW, conversation))

        CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation)

        conversation.last ?: return
        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, conversation.last!!)
    }

    private fun messageSetFlags(item: JSONArray) {
        val mId = item.optInt(1)
        val flags = item.optInt(2)
        val peerId = item.optInt(3)

        val message = CacheStorage.getMessage(mId)

        if (message != null) {
            message.flags = flags
            message.peerId = peerId

            if (VKMessage.isImportant(flags))
                message.isImportant = true

            if (VKMessage.isUnread(flags))
                message.isRead = false

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, mId)
        }

        EventBus.getDefault().postSticky(arrayOf(Keys.MESSAGE_SET_FLAGS, mId, flags, peerId))
        Log.d("FVK Message Edit", item.toString())
    }

    private fun messageClearFlags(item: JSONArray) {
        val mId = item.optInt(1)
        val flags = item.optInt(2)
        val peerId = item.optInt(3)

        val message = CacheStorage.getMessage(mId)

        if (message != null) {
            message.flags = flags
            message.peerId = peerId

            if (VKMessage.isImportant(flags))
                message.isImportant = false

            if (VKMessage.isUnread(flags))
                message.isRead = true

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, mId)
        }

        EventBus.getDefault().postSticky(arrayOf(Keys.MESSAGE_CLEAR_FLAGS, mId, flags, peerId))

        Log.d("FVK Message Edit", item.toString())
    }

    private fun editMessageEvent(item: JSONArray) {
        val id = item.optInt(1)
        val flags = item.optInt(2)
        val peerId = item.optInt(3)
        val date = item.optInt(4).toLong()
        val text = StringUtils.unescape(item.optString(5))
        val fromActions = item.optJSONObject(6)
        //JSONObject attachments = item.optJSONObject(7);
        val randomId = item.optInt(8)
        val conversationMessageId = item.optInt(9)
        val updateTime = item.optInt(10)

        val message = CacheStorage.getMessage(id) ?: return

        message.flags = flags
        message.peerId = peerId
        message.date = date
        message.text = text
        message.fromId = if (fromActions != null && fromActions.has("from")) fromActions.optInt("from") else if (message.isOut) UserConfig.userId else peerId
        message.randomId = randomId
        message.conversationMessageId = conversationMessageId
        message.updateTime = updateTime.toLong()

        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, message)

        EventBus.getDefault().postSticky(arrayOf<Any>(Keys.MESSAGE_EDIT, message))
    }

    fun process(updates: JSONArray) {
        if (updates.length() == 0) {
            return
        }

        for (i in 0 until updates.length()) {
            val item = updates.optJSONArray(i)

            when (item.optInt(0)) {
                2 //set flags
                -> messageSetFlags(item)
                3 //clear flags
                -> messageClearFlags(item)
                4 //new message
                -> messageEvent(item)
                5 //edit message
                -> editMessageEvent(item)
                8 //user online
                -> userOnline(item)
                9 //user offline
                -> userOffline(item)
                114 //notifications changes
                -> changeNotifications(item)
            }
        }
    }

    private fun changeNotifications(item: JSONArray) {
        val o = item.optJSONObject(1)

        val peerId = o.optInt("peer_id", -1)
        val sound = o.optInt("sound", -1) == 1
        val disabledUntil = o.optInt("disabled_until", -2)

        EventBus.getDefault().postSticky(arrayOf(Keys.NOTIFICATIONS_CHANGE, peerId, !sound, disabledUntil))
    }

    private fun userOffline(item: JSONArray) {
        val userId = item.optInt(1) * -1
        val timeout = item.optInt(2) == 1
        val time = item.optInt(3)

        val user = CacheStorage.getUser(userId) ?: return

        user.apply {
            isOnline = false
            isOnlineMobile = false
            lastSeen = time.toLong()
        }

        CacheStorage.update(DatabaseHelper.USERS_TABLE, user, DatabaseHelper.USER_ID, userId)

        EventBus.getDefault().postSticky(arrayOf(Keys.USER_OFFLINE, userId, time, timeout))
    }

    private fun userOnline(item: JSONArray) {
        val userId = item.optInt(1) * -1
        val platform = item.optInt(2)
        val time = item.optInt(3)

        val user = CacheStorage.getUser(userId) ?: return

        user.apply {
            isOnline = true
            isOnlineMobile = platform > 0
            lastSeen = time.toLong()
        }

        CacheStorage.update(DatabaseHelper.USERS_TABLE, user, DatabaseHelper.USER_ID, userId)

        EventBus.getDefault().postSticky(arrayOf(Keys.USER_ONLINE, userId, time, platform > 0))
    }
}
