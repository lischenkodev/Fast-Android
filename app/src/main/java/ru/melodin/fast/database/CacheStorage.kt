package ru.melodin.fast.database

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.model.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.AppGlobal.Companion.database
import ru.melodin.fast.database.DatabaseHelper.Companion.ACTION_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.ACTION_TEXT
import ru.melodin.fast.database.DatabaseHelper.Companion.ACTION_TYPE
import ru.melodin.fast.database.DatabaseHelper.Companion.ADMIN_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.ADMIN_LEVEL
import ru.melodin.fast.database.DatabaseHelper.Companion.ATTACHMENTS
import ru.melodin.fast.database.DatabaseHelper.Companion.CHATS_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.CHAT_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.CONVERSATIONS_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.DATE
import ru.melodin.fast.database.DatabaseHelper.Companion.DEACTIVATED
import ru.melodin.fast.database.DatabaseHelper.Companion.DESCRIPTION
import ru.melodin.fast.database.DatabaseHelper.Companion.DISABLED_FOREVER
import ru.melodin.fast.database.DatabaseHelper.Companion.DISABLED_UNTIL
import ru.melodin.fast.database.DatabaseHelper.Companion.FIRST_NAME
import ru.melodin.fast.database.DatabaseHelper.Companion.FRIENDS_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.FRIEND_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.FROM_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.FWD_MESSAGES
import ru.melodin.fast.database.DatabaseHelper.Companion.GROUPS_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.GROUP_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.IMPORTANT
import ru.melodin.fast.database.DatabaseHelper.Companion.IS_ADMIN
import ru.melodin.fast.database.DatabaseHelper.Companion.IS_CLOSED
import ru.melodin.fast.database.DatabaseHelper.Companion.IS_OUT
import ru.melodin.fast.database.DatabaseHelper.Companion.LAST_MESSAGE
import ru.melodin.fast.database.DatabaseHelper.Companion.LAST_NAME
import ru.melodin.fast.database.DatabaseHelper.Companion.LAST_SEEN
import ru.melodin.fast.database.DatabaseHelper.Companion.MEMBERS_COUNT
import ru.melodin.fast.database.DatabaseHelper.Companion.MESSAGES_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.MESSAGE_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.NAME
import ru.melodin.fast.database.DatabaseHelper.Companion.NO_SOUND
import ru.melodin.fast.database.DatabaseHelper.Companion.ONLINE
import ru.melodin.fast.database.DatabaseHelper.Companion.ONLINE_APP
import ru.melodin.fast.database.DatabaseHelper.Companion.ONLINE_MOBILE
import ru.melodin.fast.database.DatabaseHelper.Companion.PEER_ID
import ru.melodin.fast.database.DatabaseHelper.Companion.PHOTO_100
import ru.melodin.fast.database.DatabaseHelper.Companion.PHOTO_200
import ru.melodin.fast.database.DatabaseHelper.Companion.PHOTO_50
import ru.melodin.fast.database.DatabaseHelper.Companion.PINNED_MESSAGE
import ru.melodin.fast.database.DatabaseHelper.Companion.READ_STATE
import ru.melodin.fast.database.DatabaseHelper.Companion.REPLY
import ru.melodin.fast.database.DatabaseHelper.Companion.SCREEN_NAME
import ru.melodin.fast.database.DatabaseHelper.Companion.SEX
import ru.melodin.fast.database.DatabaseHelper.Companion.STATE
import ru.melodin.fast.database.DatabaseHelper.Companion.STATUS
import ru.melodin.fast.database.DatabaseHelper.Companion.TEXT
import ru.melodin.fast.database.DatabaseHelper.Companion.TITLE
import ru.melodin.fast.database.DatabaseHelper.Companion.TYPE
import ru.melodin.fast.database.DatabaseHelper.Companion.UNREAD_COUNT
import ru.melodin.fast.database.DatabaseHelper.Companion.UPDATE_TIME
import ru.melodin.fast.database.DatabaseHelper.Companion.USERS
import ru.melodin.fast.database.DatabaseHelper.Companion.USERS_COUNT
import ru.melodin.fast.database.DatabaseHelper.Companion.USERS_TABLE
import ru.melodin.fast.database.DatabaseHelper.Companion.USER_ID
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import java.util.*

object CacheStorage {

    val users: ArrayList<VKUser>
        get() {
            val cursor = selectCursor(USERS_TABLE)
            val users = ArrayList<VKUser>(cursor.count)
            while (cursor.moveToNext()) {
                users.add(parseUser(cursor))
            }

            cursor.close()
            return users
        }

    val conversations: ArrayList<VKConversation>?
        get() {
            val cursor = selectCursor(CONVERSATIONS_TABLE)
            if (cursor.count <= 0) {
                return arrayListOf()
            }

            val dialogs = ArrayList<VKConversation>(cursor.count)

            if (cursor.moveToFirst())
                do {
                    dialogs.add(parseConversation(cursor))
                } while (cursor.moveToNext())

            cursor.close()
            return dialogs
        }

    val groups: ArrayList<VKGroup>?
        get() {
            val cursor = selectCursor(GROUPS_TABLE)
            if (cursor.count <= 0) {
                return null
            }

            val groups = ArrayList<VKGroup>(cursor.count)
            while (cursor.moveToNext()) {
                groups.add(parseGroup(cursor))
            }

            cursor.close()
            return groups
        }

    private fun selectCursor(table: String, column: String, value: Any): Cursor {
        return QueryBuilder.query()
            .select("*").from(table)
            .where("$column = $value")
            .asCursor(database)
    }

    private fun selectCursor(table: String, where: String): Cursor {
        return QueryBuilder.query()
            .select("*").from(table).where(where)
            .asCursor(database)
    }

    private fun selectCursor(table: String): Cursor {
        return QueryBuilder.query()
            .select("*").from(table)
            .asCursor(database)
    }

    private fun getInt(cursor: Cursor, columnName: String): Int {
        return cursor.getInt(cursor.getColumnIndex(columnName))
    }

    private fun getString(cursor: Cursor, columnName: String): String? {
        return cursor.getString(cursor.getColumnIndex(columnName))
    }

    private fun getLong(cursor: Cursor, columnName: String): Long {
        return cursor.getLong(cursor.getColumnIndex(columnName))
    }

    private fun getBlob(cursor: Cursor, columnName: String): ByteArray? {
        return cursor.getBlob(cursor.getColumnIndex(columnName))
    }

    fun getUser(id: Int): VKUser? {
        val cursor = selectCursor(USERS_TABLE, USER_ID, id)
        if (cursor.moveToFirst()) {
            return parseUser(cursor)
        }
        cursor.close()
        return null
    }

    fun getGroup(id: Int): VKGroup? {
        val cursor = selectCursor(GROUPS_TABLE, GROUP_ID, id)
        if (cursor.moveToFirst()) {
            return parseGroup(cursor)
        }
        cursor.close()
        return null
    }

    fun getUsers(ids: Int): ArrayList<VKUser> {
        val cursor = selectCursor(USERS_TABLE, USER_ID, ids)
        val users = ArrayList<VKUser>(cursor.count)
        while (cursor.moveToNext()) {
            users.add(parseUser(cursor))
        }

        cursor.close()
        return users
    }

    fun getFriends(userId: Int, onlyOnline: Boolean): ArrayList<VKUser> {
        val cursor = QueryBuilder.query()
            .select("*")
            .from(FRIENDS_TABLE)
            .leftJoin(USERS_TABLE)
            .on("friends.friend_id = users.user_id")
            .where("friends.user_id = $userId")
            .asCursor(database)

        val users = ArrayList<VKUser>(cursor.count)

        while (cursor.moveToNext()) {
            val userOnline = getInt(cursor, ONLINE) == 1
            if (onlyOnline && !userOnline) {
                continue
            }

            val user = parseUser(cursor)
            users.add(user)
        }
        cursor.close()

        return users
    }

    fun getChat(chatId: Int): VKChat? {
        val cursor = selectCursor(CHATS_TABLE, CHAT_ID, chatId)
        return if (cursor.moveToFirst()) {
            parseChat(cursor)
        } else null
    }

    fun getConversation(peerId: Int): VKConversation? {
        val cursor = selectCursor(CONVERSATIONS_TABLE, PEER_ID, peerId)
        return if (cursor.moveToFirst()) {
            parseConversation(cursor)
        } else null

    }

    fun getMessages(peerId: Int): ArrayList<VKMessage> {
        val cursor = selectCursor(MESSAGES_TABLE, dialogWhere(peerId))

        val messages = ArrayList<VKMessage>(cursor.count)
        while (cursor.moveToNext()) {
            messages.add(parseMessage(cursor))
        }
        cursor.close()
        return messages
    }

    fun getMessage(mId: Int): VKMessage? {
        val cursor = selectCursor(
            MESSAGES_TABLE,
            String.format(AppGlobal.locale, "%s = %d", MESSAGE_ID, mId)
        )

        return if (cursor.moveToFirst()) parseMessage(cursor) else null

    }

    private fun dialogWhere(peerId: Int): String {
        return String.format(Locale.US, "%s = %d", PEER_ID, peerId)
    }

    fun update(table: String, item: Any, where: Any, vararg args: Any) {
        update(table, ArrayList(setOf(item)), where, *args)
    }

    private fun update(table: String, values: ArrayList<*>, where: Any, vararg args: Any) {
        if (ArrayUtil.isEmpty(values)) return
        database.beginTransaction()

        val cv = ContentValues()
        for (i in values.indices) {
            val item = values[i]
            when (table) {
                USERS_TABLE -> putValues(cv, item as VKUser, false)
                FRIENDS_TABLE -> putValues(cv, item as VKUser, true)
                CONVERSATIONS_TABLE -> putValues(cv, item as VKConversation)
                MESSAGES_TABLE -> putValues(cv, item as VKMessage)
                GROUPS_TABLE -> putValues(cv, item as VKGroup)
                CHATS_TABLE -> putValues(cv, item as VKChat)
            }


            val arguments = arrayOfNulls<String>(args.size)
            for (j in args.indices) {
                arguments[j] = args[j].toString() + ""
            }

            database.update(table, cv, "$where = ?", arguments)
            cv.clear()
        }

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun insert(table: String, values: ArrayList<*>) {
        if (ArrayUtil.isEmpty(values)) return
        database.beginTransaction()

        val cv = ContentValues()
        for (i in values.indices) {
            val item = values[i]
            when (table) {
                USERS_TABLE -> putValues(cv, item as VKUser, false)
                FRIENDS_TABLE -> putValues(cv, item as VKUser, true)
                CONVERSATIONS_TABLE -> putValues(cv, item as VKConversation)
                MESSAGES_TABLE -> putValues(cv, item as VKMessage)
                GROUPS_TABLE -> putValues(cv, item as VKGroup)
                CHATS_TABLE -> putValues(cv, item as VKChat)
            }

            database.insert(table, null, cv)
            cv.clear()
        }

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun insert(table: String, item: Any) {
        insert(table, ArrayList(setOf(item)))
    }

    fun delete(table: String, where: Any, args: Any) {
        database.delete(table, "$where = ?", arrayOf(args.toString()))
    }

    fun delete(table: String) {
        database.delete(table, null, null)
    }

    private fun parseUser(cursor: Cursor): VKUser {
        val user = VKUser()

        user.id = getInt(cursor, USER_ID)
        user.name = getString(cursor, FIRST_NAME)
        user.surname = getString(cursor, LAST_NAME)
        user.lastSeen = getInt(cursor, LAST_SEEN).toLong()
        user.screenName = getString(cursor, SCREEN_NAME)
        user.status = getString(cursor, STATUS)
        user.photo50 = getString(cursor, PHOTO_50)
        user.photo100 = getString(cursor, PHOTO_100)
        user.photo200 = getString(cursor, PHOTO_200)

        user.isOnline = getInt(cursor, ONLINE) == 1
        user.isOnlineMobile = getInt(cursor, ONLINE_MOBILE) == 1
        user.onlineApp = getInt(cursor, ONLINE_APP)
        user.isDeactivated = getInt(cursor, DEACTIVATED) == 1
        user.sex = getInt(cursor, SEX)
        return user
    }

    private fun parseConversation(cursor: Cursor): VKConversation {
        val dialog = VKConversation()

        dialog.peerId = getInt(cursor, PEER_ID)
        dialog.isRead = getInt(cursor, READ_STATE) == 1
        dialog.title = getString(cursor, TITLE)
        dialog.membersCount = getInt(cursor, USERS_COUNT)
        dialog.unread = getInt(cursor, UNREAD_COUNT)
        dialog.state = VKConversation.getState(getString(cursor, STATE))

        dialog.isNoSound = getInt(cursor, NO_SOUND) == 1
        dialog.isDisabledForever = getInt(cursor, DISABLED_FOREVER) == 1
        dialog.disabledUntil = getInt(cursor, DISABLED_UNTIL)

        dialog.type = VKConversation.getType(getString(cursor, TYPE))

        dialog.photo50 = getString(cursor, PHOTO_50)
        dialog.photo100 = getString(cursor, PHOTO_100)
        dialog.photo200 = getString(cursor, PHOTO_200)

        val last = getBlob(cursor, LAST_MESSAGE)
        val pinned = getBlob(cursor, PINNED_MESSAGE)

        dialog.lastMessage = Util.deserialize(last) as VKMessage?
        dialog.pinned = Util.deserialize(pinned) as VKMessage?
        return dialog
    }

    private fun parseMessage(cursor: Cursor): VKMessage {
        val message = VKMessage()
        message.id = getInt(cursor, MESSAGE_ID)
        message.peerId = getInt(cursor, PEER_ID)
        message.fromId = getInt(cursor, FROM_ID)
        message.date = getInt(cursor, DATE).toLong()
        message.text = getString(cursor, TEXT)
        message.isRead = getInt(cursor, READ_STATE) == 1
        message.isOut = getInt(cursor, IS_OUT) == 1
        message.reply = Util.deserialize(getBlob(cursor, REPLY)) as VKReplyMessage?
        message.isImportant = getInt(cursor, IMPORTANT) == 1
        message.status = VKMessage.getStatus(getString(cursor, STATUS))!!
        message.updateTime = getLong(cursor, UPDATE_TIME)
        message.action = VKMessage.getAction(getString(cursor, ACTION_TYPE))
        message.actionText = getString(cursor, ACTION_TEXT)
        message.actionId = getInt(cursor, ACTION_ID)

        val attachments = getBlob(cursor, ATTACHMENTS)
        val forwarded = getBlob(cursor, FWD_MESSAGES)

        message.attachments = Util.deserialize(attachments) as ArrayList<VKModel>
        message.fwdMessages = Util.deserialize(forwarded) as ArrayList<VKMessage>
        return message
    }

    private fun parseGroup(cursor: Cursor): VKGroup {
        val group = VKGroup()
        group.id = getInt(cursor, GROUP_ID)
        group.name = getString(cursor, NAME)
        group.screenName = getString(cursor, SCREEN_NAME)
        group.description = getString(cursor, DESCRIPTION)
        group.status = getString(cursor, STATUS)
        group.type = VKGroup.getType(getString(cursor, TYPE))!!
        group.isClosed = getInt(cursor, IS_CLOSED) == 1
        group.adminLevel = getInt(cursor, ADMIN_LEVEL)
        group.isAdmin = getInt(cursor, IS_ADMIN) == 1
        group.photo50 = getString(cursor, PHOTO_50)
        group.photo100 = getString(cursor, PHOTO_100)
        group.photo200 = getString(cursor, PHOTO_200)
        group.membersCount = getInt(cursor, MEMBERS_COUNT).toLong()
        return group
    }

    private fun parseChat(cursor: Cursor): VKChat {
        val chat = VKChat()
        chat.adminId = getInt(cursor, ADMIN_ID)
        chat.id = getInt(cursor, CHAT_ID)
        chat.photo50 = getString(cursor, PHOTO_50)
        chat.photo100 = getString(cursor, PHOTO_100)
        chat.photo200 = getString(cursor, PHOTO_200)
        chat.state = VKChat.getState(getInt(cursor, STATE))
        chat.title = getString(cursor, TITLE)
        chat.type = VKConversation.getType(getString(cursor, TYPE))
        chat.users = Util.deserialize(getBlob(cursor, USERS)) as ArrayList<VKUser>
        return chat
    }

    private fun putValues(values: ContentValues, chat: VKChat) {
        values.put(ADMIN_ID, chat.adminId)
        values.put(CHAT_ID, chat.id)
        values.put(PHOTO_50, chat.photo50)
        values.put(PHOTO_100, chat.photo100)
        values.put(PHOTO_200, chat.photo200)
        values.put(STATE, VKChat.getIntState(chat.state))
        values.put(TITLE, chat.title)
        values.put(TYPE, VKConversation.getType(chat.type))
        values.put(USERS, Util.serialize(chat.users))
    }

    private fun putValues(values: ContentValues, user: VKUser, friends: Boolean) {
        if (friends) {
            values.put(USER_ID, UserConfig.userId)
            values.put(FRIEND_ID, user.id)
            return
        }

        values.put(USER_ID, user.id)
        values.put(FIRST_NAME, user.name)
        values.put(LAST_NAME, user.surname)
        values.put(SCREEN_NAME, user.screenName)
        values.put(LAST_SEEN, user.lastSeen)
        values.put(ONLINE, user.isOnline)
        values.put(ONLINE_MOBILE, user.isOnlineMobile)
        values.put(ONLINE_APP, user.onlineApp)
        values.put(STATUS, user.status)
        values.put(PHOTO_50, user.photo50)
        values.put(PHOTO_100, user.photo100)
        values.put(PHOTO_200, user.photo200)
        values.put(SEX, user.sex)
    }

    private fun putValues(values: ContentValues, dialog: VKConversation) {
        values.put(PEER_ID, dialog.peerId)
        values.put(UNREAD_COUNT, dialog.unread)
        values.put(READ_STATE, dialog.isRead)
        values.put(USERS_COUNT, dialog.membersCount)
        values.put(TYPE, VKConversation.getType(dialog.type))
        values.put(DISABLED_FOREVER, dialog.isDisabledForever)
        values.put(DISABLED_UNTIL, dialog.disabledUntil)
        values.put(NO_SOUND, dialog.isNoSound)
        values.put(STATE, VKConversation.getState(dialog.state))

        if (dialog.lastMessage != null) {
            values.put(LAST_MESSAGE, Util.serialize(dialog.lastMessage!!))
        }

        if (dialog.pinned != null) {
            values.put(PINNED_MESSAGE, Util.serialize(dialog.pinned!!))
        }

        if (TextUtils.isEmpty(dialog.title)) {
            if (dialog.isGroup) {
                val group = getGroup(dialog.peerId)
                values.put(TITLE, if (group == null) "" else group.name)
            } else {
                val user = getUser(dialog.peerId)
                values.put(TITLE, if (user == null) "" else user.name + " " + user.surname)
            }
        } else {
            values.put(TITLE, dialog.title)
        }

        if (TextUtils.isEmpty(dialog.photo50) && TextUtils.isEmpty(dialog.photo100) && TextUtils.isEmpty(
                dialog.photo200
            )
        ) {
            if (dialog.isGroup) {
                val group = getGroup(dialog.peerId)
                values.put(PHOTO_50, if (group == null) "" else group.photo50)
                values.put(PHOTO_100, if (group == null) "" else group.photo100)
                values.put(PHOTO_200, if (group == null) "" else group.photo200)
            } else {
                val user = getUser(dialog.peerId)
                values.put(PHOTO_50, if (user == null) "" else user.photo50)
                values.put(PHOTO_100, if (user == null) "" else user.photo100)
                values.put(PHOTO_200, if (user == null) "" else user.photo200)
            }
        } else {
            values.put(PHOTO_50, dialog.photo50)
            values.put(PHOTO_100, dialog.photo100)
            values.put(PHOTO_200, dialog.photo200)
        }
    }

    private fun putValues(values: ContentValues, message: VKMessage) {
        values.put(MESSAGE_ID, message.id)
        values.put(PEER_ID, message.peerId)
        values.put(FROM_ID, message.fromId)
        values.put(TEXT, message.text)
        values.put(DATE, message.date)
        values.put(IS_OUT, message.isOut)

        values.put(STATUS, VKMessage.getStatus(message.status))
        values.put(READ_STATE, message.isRead)
        values.put(UPDATE_TIME, message.updateTime)
        values.put(ACTION_TEXT, message.actionText)
        values.put(ACTION_TYPE, VKMessage.getAction(message.action))
        values.put(ACTION_ID, message.actionId)
        values.put(IMPORTANT, message.isImportant)
        values.put(ATTACHMENTS, Util.serialize(message.attachments))
        values.put(FWD_MESSAGES, Util.serialize(message.fwdMessages))
        values.put(REPLY, Util.serialize(message.reply))
    }

    private fun putValues(values: ContentValues, group: VKGroup) {
        values.put(GROUP_ID, group.id)
        values.put(NAME, group.name)
        values.put(SCREEN_NAME, group.screenName)
        values.put(DESCRIPTION, group.description)
        values.put(STATUS, group.status)
        values.put(TYPE, VKGroup.getType(group.type))
        values.put(IS_CLOSED, group.isClosed)
        values.put(ADMIN_LEVEL, group.adminLevel)
        values.put(IS_ADMIN, group.isAdmin)
        values.put(PHOTO_50, group.photo50)
        values.put(PHOTO_100, group.photo100)
        values.put(PHOTO_200, group.photo200)
        values.put(MEMBERS_COUNT, group.membersCount)
    }
}
