package ru.melodin.fast.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import ru.melodin.fast.common.AppGlobal

class DatabaseHelper private constructor() : SQLiteOpenHelper(AppGlobal.context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_USERS)
        db.execSQL(SQL_CREATE_TABLE_GROUPS)
        db.execSQL(SQL_CREATE_TABLE_DIALOGS)
        db.execSQL(SQL_CREATE_TABLE_FRIENDS)
        db.execSQL(SQL_CREATE_TABLE_MESSAGES)
        db.execSQL(SQL_CREATE_TABLE_CHATS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dropTables(db)
        onCreate(db)
    }

    fun dropMessagesTable(db: SQLiteDatabase) {
        db.execSQL(SQL_DELETE_DIALOGS)
        db.execSQL(SQL_DELETE_MESSAGES)
        db.execSQL(SQL_CREATE_TABLE_DIALOGS)
        db.execSQL(SQL_CREATE_TABLE_MESSAGES)
        db.execSQL(SQL_DELETE_CHATS)
        db.execSQL(SQL_CREATE_TABLE_CHATS)
    }

    fun dropUsersTable(db: SQLiteDatabase) {
        db.execSQL(SQL_DELETE_USERS)
        db.execSQL(SQL_CREATE_TABLE_USERS)
    }

    fun dropGroupsTable(db: SQLiteDatabase) {
        db.execSQL(SQL_DELETE_GROUPS)
        db.execSQL(SQL_CREATE_TABLE_GROUPS)
    }

    fun dropTables(db: SQLiteDatabase) {
        db.execSQL(SQL_DELETE_USERS)
        db.execSQL(SQL_DELETE_GROUPS)
        db.execSQL(SQL_DELETE_FRIENDS)
        db.execSQL(SQL_DELETE_DIALOGS)
        db.execSQL(SQL_DELETE_MESSAGES)
        db.execSQL(SQL_DELETE_CHATS)
    }

    companion object {

        const val USERS_TABLE = "users"
        const val FRIENDS_TABLE = "friends"
        const val CONVERSATIONS_TABLE = "conversations"
        const val MESSAGES_TABLE = "messages"
        const val GROUPS_TABLE = "groups"
        const val CHATS_TABLE = "chats"
        const val PEER_ID = "peer_id"
        const val MESSAGE_ID = "message_id"
        const val USER_ID = "user_id"
        const val CHAT_ID = "chat_id"

        internal const val GROUP_ID = "group_id"
        internal const val FRIEND_ID = "friend_id"
        internal const val FROM_ID = "from_id"
        internal const val TITLE = "title"
        internal const val REPLY = "reply"
        internal const val UNREAD_COUNT = "unread_count"
        internal const val IMPORTANT = "important"
        internal const val ATTACHMENTS = "attachments"
        internal const val FWD_MESSAGES = "fwd_messages"
        internal const val USERS_COUNT = "users_count"
        internal const val IS_OUT = "is_out"
        internal const val READ_STATE = "read_state"
        internal const val DATE = "date"
        internal const val FIRST_NAME = "first_name"
        internal const val LAST_NAME = "last_name"
        internal const val ONLINE = "online_status"
        internal const val ONLINE_MOBILE = "online_mobile"
        internal const val ONLINE_APP = "online_app"
        internal const val STATUS = "status"
        internal const val TEXT = "text"
        internal const val PHOTO_50 = "photo_50"
        internal const val PHOTO_100 = "photo_100"
        internal const val PHOTO_200 = "photo_200"
        internal const val DEACTIVATED = "deactivated"
        internal const val LAST_SEEN = "last_seen"
        internal const val SEX = "sex"
        internal const val NAME = "name"
        internal const val SCREEN_NAME = "screen_name"
        internal const val IS_CLOSED = "is_closed"
        internal const val IS_ADMIN = "is_admin"
        internal const val ADMIN_LEVEL = "admin_level"
        internal const val TYPE = "type"
        internal const val DESCRIPTION = "description"
        internal const val MEMBERS_COUNT = "members_count"
        internal const val PINNED_MESSAGE = "pinned_message"
        internal const val LAST_MESSAGE = "last_message"
        internal const val UPDATE_TIME = "update_time"
        internal const val DISABLED_FOREVER = "disabled_forever"
        internal const val DISABLED_UNTIL = "disabled_until"
        internal const val NO_SOUND = "no_sound"
        internal const val ACTION_TYPE = "action_type"
        internal const val ACTION_ID = "action_id"
        internal const val ACTION_TEXT = "action_text"
        internal const val STATE = "state"
        internal const val ADMIN_ID = "admin_id"
        internal const val USERS = "users"

        private const val NICKNAME = "nickname"
        private const val IS_FRIEND = "is_friend"
        private const val PHOTO_MAX = "photo_max"
        private const val GROUPS = "groups"

        private const val DATABASE_VERSION = 46
        private const val DATABASE_NAME = "cache.db"

        private const val SQL_CREATE_TABLE_USERS = "CREATE TABLE " + USERS_TABLE +
                " (" + USER_ID + " INTEGER UNIQUE PRIMARY KEY ON CONFLICT REPLACE, " +
                " [" + FIRST_NAME + "] VARCHAR(255), " +
                " [" + LAST_NAME + "] VARCHAR(255), " +
                " [" + SCREEN_NAME + "] VARCHAR(255), " +
                " [" + NICKNAME + "] VARCHAR(255), " +
                " [" + ONLINE + "] INTEGER, " +
                " [" + ONLINE_MOBILE + "] INTEGER, " +
                " [" + ONLINE_APP + "] INTEGER, " +
                " [" + STATUS + "] VARCHAR(255), " +
                " [" + IS_FRIEND + "] VARCHAR(255), " +
                " [" + LAST_SEEN + "] INTEGER, " +
                " [" + PHOTO_50 + "] VARCHAR(255), " +
                " [" + PHOTO_100 + "] VARCHAR(255), " +
                " [" + PHOTO_200 + "] VARCHAR(255), " +
                " [" + PHOTO_MAX + "] VARCHAR(255), " +
                " [" + DEACTIVATED + "] INTEGER, " +
                " [" + SEX + "] INTEGER" +
                ");"

        private const val SQL_CREATE_TABLE_FRIENDS = "CREATE TABLE " + FRIENDS_TABLE +
                " (" + USER_ID + " INTEGER UNIQUE ON CONFLICT REPLACE, " +
                " [" + FRIEND_ID + "] INTEGER UNIQUE ON CONFLICT REPLACE " +
                ");"

        private const val SQL_CREATE_TABLE_CHATS = "CREATE TABLE " + CHATS_TABLE +
                " (" + CHAT_ID + " INTEGER UNIQUE ON CONFLICT REPLACE, " +
                " [" + TITLE + "] VARCHAR(255), " +
                " [" + ADMIN_ID + "] INTEGER, " +
                " [" + USERS + "] BLOB, " +
                " [" + PHOTO_50 + "] VARCHAR(255), " +
                " [" + PHOTO_100 + "] VARCHAR(255), " +
                " [" + PHOTO_200 + "] VARCHAR(255), " +
                " [" + TYPE + "] VARCHAR(255), " +
                " [" + STATE + "] INTEGER" +
                ");"

        private const val SQL_CREATE_TABLE_DIALOGS = "CREATE TABLE " + CONVERSATIONS_TABLE +
                " (" + PEER_ID + " INTEGER UNIQUE ON CONFLICT REPLACE, " +
                " [" + TYPE + "] VARCHAR(255), " +
                " [" + TITLE + "] VARCHAR(255), " +
                " [" + READ_STATE + "] INTEGER, " +
                " [" + USERS_COUNT + "] INTEGER, " +
                " [" + UNREAD_COUNT + "] INTEGER, " +
                " [" + STATE + "] VARCHAR(255), " +
                " [" + PHOTO_50 + "] VARCHAR(255), " +
                " [" + PHOTO_100 + "] VARCHAR(255)," +
                " [" + PHOTO_200 + "] VARCHAR(255), " +
                " [" + DISABLED_FOREVER + "] INTEGER, " +
                " [" + DISABLED_UNTIL + "] INTEGER, " +
                " [" + NO_SOUND + "] INTEGER, " +
                " [" + USERS + "] BLOB, " +
                " [" + GROUPS + "] BLOB, " +
                " [" + PINNED_MESSAGE + "] BLOB, " +
                " [" + LAST_MESSAGE + "] BLOB" +
                ");"

        private const val SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE " + MESSAGES_TABLE +
                " (" + MESSAGE_ID + " INTEGER UNIQUE ON CONFLICT REPLACE, " +
                " [" + PEER_ID + "] INTEGER, " +
                " [" + FROM_ID + "] INTEGER, " +
                " [" + TEXT + "] VARCHAR(255), " +
                " [" + STATUS + "] VARCHAR(255), " +
                " [" + UPDATE_TIME + "] LONG, " +
                " [" + DATE + "] INTEGER, " +
                " [" + READ_STATE + "] INTEGER, " +
                " [" + IS_OUT + "] INTEGER, " +
                " [" + IMPORTANT + "] INTEGER, " +
                " [" + ACTION_TYPE + "] VARCHAR(255), " +
                " [" + ACTION_TEXT + "] TEXT, " +
                " [" + ACTION_ID + "] INTEGER, " +
                " [" + REPLY + "] BLOB, " +
                " [" + ATTACHMENTS + "] BLOB, " +
                " [" + FWD_MESSAGES + "] BLOB" +
                ");"

        private const val SQL_CREATE_TABLE_GROUPS = "CREATE TABLE " + GROUPS_TABLE +
                " (" + GROUP_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
                " [" + NAME + "] VARCHAR(255), " +
                " [" + SCREEN_NAME + "] VARCHAR(255), " +
                " [" + DESCRIPTION + "] VARCHAR(255), " +
                " [" + STATUS + "] VARCHAR(255), " +
                " [" + TYPE + "] VARCHAR(255), " +
                " [" + IS_CLOSED + "] INTEGER, " +
                " [" + IS_ADMIN + "] INTEGER, " +
                " [" + ADMIN_LEVEL + "] INTEGER, " +
                " [" + PHOTO_50 + "] VARCHAR(255), " +
                " [" + PHOTO_100 + "] VARCHAR(255), " +
                " [" + PHOTO_200 + "] VARCHAR(255), " +
                " [" + MEMBERS_COUNT + "] INTEGER " +
                ");"

        private const val SQL_DELETE_USERS = "DROP TABLE IF EXISTS $USERS_TABLE"
        private const val SQL_DELETE_GROUPS = "DROP TABLE IF EXISTS $GROUPS_TABLE"
        private const val SQL_DELETE_FRIENDS = "DROP TABLE IF EXISTS $FRIENDS_TABLE"
        private const val SQL_DELETE_DIALOGS = "DROP TABLE IF EXISTS $CONVERSATIONS_TABLE"
        private const val SQL_DELETE_MESSAGES = "DROP TABLE IF EXISTS $MESSAGES_TABLE"
        private const val SQL_DELETE_CHATS = "DROP TABLE IF EXISTS $CHATS_TABLE"

        private var instance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper()
            }

            return instance as DatabaseHelper
        }
    }
}
