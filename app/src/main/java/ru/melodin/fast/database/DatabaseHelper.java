package ru.melodin.fast.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.melodin.fast.common.AppGlobal;

public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Tables
     */
    public static final String USERS_TABLE = "users";
    public static final String FRIENDS_TABLE = "friends";
    public static final String DIALOGS_TABLE = "dialogs";
    public static final String MESSAGES_TABLE = "messages";
    public static final String GROUPS_TABLE = "groups";
    static final String GROUP_ID = "group_id";
    static final String FRIEND_ID = "friend_id";
    static final String FROM_ID = "from_id";
    public static final String PEER_ID = "peer_id";
    public static final String MESSAGE_ID = "message_id";
    static final String TITLE = "title";
    static final String UNREAD_COUNT = "unread_count";
    static final String IMPORTANT = "important";
    static final String ATTACHMENTS = "attachments";
    static final String FWD_MESSAGES = "fwd_messages";
    static final String USERS_COUNT = "users_count";
    static final String IS_OUT = "is_out";
    static final String READ_STATE = "read_state";
    static final String DATE = "date";
    static final String FIRST_NAME = "first_name";
    static final String LAST_NAME = "last_name";
    private static final String NICKNAME = "nickname";
    static final String ONLINE = "online_status";
    static final String ONLINE_MOBILE = "online_mobile";
    static final String ONLINE_APP = "online_app";
    static final String STATUS = "status";
    private static final String IS_FRIEND = "is_friend";
    static final String TEXT = "text";
    static final String PHOTO_50 = "photo_50";
    static final String PHOTO_100 = "photo_100";
    static final String PHOTO_200 = "photo_200";
    private static final String PHOTO_MAX = "photo_max";
    static final String DEACTIVATED = "deactivated";
    static final String LAST_SEEN = "last_seen";
    static final String SEX = "sex";
    static final String NAME = "name";
    static final String SCREEN_NAME = "screen_name";
    static final String IS_CLOSED = "is_closed";
    static final String IS_ADMIN = "is_admin";
    static final String ADMIN_LEVER = "admin_level";
    static final String TYPE = "type";
    static final String DESCRIPTION = "description";
    static final String MEMBERS_COUNT = "members_count";
    static final String PINNED_MESSAGE = "pinned_message";
    static final String LAST_MESSAGE = "last_message";
    static final String UPDATE_TIME = "update_time";
    static final String DISABLED_FOREVER = "disabled_forever";
    static final String DISABLED_UNTIL = "disabled_until";
    static final String NO_SOUND = "no_sound";
    static final String USERS = "users";
    static final String GROUPS = "groups";
    private static final String _ID = "_id";
    static final String CONVERSATION_TYPE = "type";
    public static final String USER_ID = "user_id";
    static final String ACTION_TYPE = "action_type";
    static final String ACTION_USER_ID = "action_user_id";
    static final String ACTION_TEXT = "action_text";

    private static final int DATABASE_VERSION = 24;
    private static final String DATABASE_NAME = "cache.db";

    /**
     * Columns
     */

    private static final String SQL_CREATE_TABLE_USERS = "CREATE TABLE " + USERS_TABLE +
            " (" + USER_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
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
            " [" + DEACTIVATED + "] VARCHAR(255), " +
            " [" + SEX + "] INTEGER" +
            ");";

    private static final String SQL_CREATE_TABLE_FRIENDS = "CREATE TABLE " + FRIENDS_TABLE +
            " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " [" + USER_ID + "] INTEGER, " +
            " [" + FRIEND_ID + "] INTEGER " +
            ");";

    private static final String SQL_CREATE_TABLE_DIALOGS = "CREATE TABLE " + DIALOGS_TABLE +
            " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            " [" + PEER_ID + "] INTEGER UNIQUE ON CONFLICT REPLACE, " +
            " [" + CONVERSATION_TYPE + "] VARCHAR(5), " +
            " [" + TITLE + "] VARCHAR(255), " +
            " [" + READ_STATE + "] INTEGER, " +
            " [" + USERS_COUNT + "] INTEGER, " +
            " [" + UNREAD_COUNT + "] INTEGER, " +
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
            ");";

    private final static String SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE " + MESSAGES_TABLE +
            " (" + _ID + "  INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
            " [" + MESSAGE_ID + "] INTEGER, " +
            " [" + PEER_ID + "] INTEGER, " +
            " [" + FROM_ID + "] INTEGER, " +
            " [" + TEXT + "] VARCHAR(255), " +
            " [" + STATUS + "] INTEGER, " +
            " [" + UPDATE_TIME + "] LONG, " +
            " [" + DATE + "] INTEGER, " +
            " [" + READ_STATE + "] INTEGER, " +
            " [" + IS_OUT + "] INTEGER, " +
            " [" + IMPORTANT + "] INTEGER, " +
            " [" + ACTION_TYPE + "] VARCHAT(255), " +
            " [" + ACTION_TEXT + "] TEXT, " +
            " [" + ACTION_USER_ID + "] INTEGER, " +
            " [" + USERS + "] BLOB, " +
            " [" + GROUPS + "] BLOB, " +
            " [" + ATTACHMENTS + "] BLOB, " +
            " [" + FWD_MESSAGES + "] BLOB" +
            ");";

    private final static String SQL_CREATE_TABLE_GROUPS = "CREATE TABLE " + GROUPS_TABLE +
            " (" + GROUP_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
            " [" + NAME + "] VARCHAR(255), " +
            " [" + SCREEN_NAME + "] VARCHAR(255), " +
            " [" + DESCRIPTION + "] VARCHAR(255), " +
            " [" + STATUS + "] VARCHAR(255), " +
            " [" + TYPE + "] INTEGER, " +
            " [" + IS_CLOSED + "] INTEGER, " +
            " [" + IS_ADMIN + "] INTEGER, " +
            " [" + ADMIN_LEVER + "] INTEGER, " +
            " [" + PHOTO_50 + "] VARCHAR(255), " +
            " [" + PHOTO_100 + "] VARCHAR(255), " +
            " [" + PHOTO_200 + "] VARCHAR(255), " +
            " [" + MEMBERS_COUNT + "] INTEGER " +
            ");";

    private static final String SQL_DELETE_USERS = "DROP TABLE IF EXISTS " + USERS_TABLE;
    private static final String SQL_DELETE_GROUPS = "DROP TABLE IF EXISTS " + GROUPS_TABLE;
    private static final String SQL_DELETE_FRIENDS = "DROP TABLE IF EXISTS " + FRIENDS_TABLE;
    private static final String SQL_DELETE_DIALOGS = "DROP TABLE IF EXISTS " + DIALOGS_TABLE;
    private static final String SQL_DELETE_MESSAGES = "DROP TABLE IF EXISTS " + MESSAGES_TABLE;

    private static DatabaseHelper instance;

    private DatabaseHelper() {
        super(AppGlobal.context(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_USERS);
        db.execSQL(SQL_CREATE_TABLE_GROUPS);
        db.execSQL(SQL_CREATE_TABLE_DIALOGS);
        db.execSQL(SQL_CREATE_TABLE_FRIENDS);
        db.execSQL(SQL_CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db);
    }

    public void dropMessagesTable(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_DIALOGS);
        db.execSQL(SQL_DELETE_MESSAGES);
        db.execSQL(SQL_CREATE_TABLE_DIALOGS);
        db.execSQL(SQL_CREATE_TABLE_MESSAGES);
    }

    public void dropUsersTable(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_USERS);
        db.execSQL(SQL_CREATE_TABLE_USERS);
    }

    public void dropTables(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_USERS);
        db.execSQL(SQL_DELETE_GROUPS);
        db.execSQL(SQL_DELETE_FRIENDS);
        db.execSQL(SQL_DELETE_DIALOGS);
        db.execSQL(SQL_DELETE_MESSAGES);
    }
}
