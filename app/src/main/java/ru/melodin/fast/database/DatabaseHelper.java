package ru.melodin.fast.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ru.melodin.fast.common.AppGlobal;

public class DatabaseHelper extends SQLiteOpenHelper {

    //last_db_change date = 30.12.18
    private static final int DATABASE_VERSION = 16;
    private static final String DATABASE_NAME = "cache.db";

    /**
     * Tables
     */
    public static final String USERS_TABLE = "users";
    public static final String FRIENDS_TABLE = "friends";
    public static final String DIALOGS_TABLE = "dialogs";
    public static final String MESSAGES_TABLE = "messages";
    public static final String GROUPS_TABLE = "groups";

    /**
     * Columns
     */
    static final String _ID = "_id";
    static final String CONVERSATION_TYPE = "type";
    static final String USER_ID = "user_id";
    public static final String GROUP_ID = "group_id";
    public static final String FRIEND_ID = "friend_id";
    public static final String FROM_ID = "from_id";
    public static final String PEER_ID = "peer_id";
    public static final String MESSAGE_ID = "message_id";
    public static final String TITLE = "title";
    public static final String UNREAD_COUNT = "unread_count";
    public static final String IMPORTANT = "important";
    public static final String ATTACHMENTS = "attachments";
    public static final String FWD_MESSAGES = "fwd_messages";
    public static final String USERS_COUNT = "users_count";
    public static final String IS_OUT = "is_out";
    public static final String READ_STATE = "read_state";
    public static final String DATE = "date";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String NICKNAME = "nickname";
    public static final String ONLINE = "online_status";
    public static final String ONLINE_MOBILE = "online_mobile";
    public static final String ONLINE_APP = "online_app";
    public static final String STATUS = "status";
    public static final String IS_FRIEND = "is_friend";
    public static final String TEXT = "text";
    public static final String PHOTO_50 = "photo_50";
    public static final String PHOTO_100 = "photo_100";
    public static final String PHOTO_200 = "photo_200";
    public static final String PHOTO_MAX = "photo_max";
    public static final String DEACTIVATED = "deactivated";
    public static final String LAST_SEEN = "last_seen";
    public static final String SEX = "sex";
    public static final String NAME = "name";
    public static final String SCREEN_NAME = "screen_name";
    public static final String IS_CLOSED = "is_closed";
    public static final String IS_ADMIN = "is_admin";
    public static final String ADMIN_LEVER = "admin_level";
    public static final String TYPE = "type";
    public static final String DESCRIPTION = "description";
    public static final String MEMBERS_COUNT = "members_count";
    public static final String PINNED_MESSAGE = "pinned_message";
    public static final String LAST_MESSAGE = "last_message";
    public static final String UPDATE_TIME = "update_time";
    public static final String DISABLED_FOREVER = "disabled_forever";
    public static final String DISABLED_UNTIL = "disabled_until";
    public static final String NO_SOUND = "no_sound";
    public static final String USERS = "users";
    public static final String GROUPS = "groups";

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
            " [" + MESSAGE_ID + "] INTEGER, " +
            " [" + PEER_ID + "] INTEGER, " +
            " [" + CONVERSATION_TYPE + "] VARCHAR(5), " +
            " [" + FROM_ID + "] INTEGER, " +
            " [" + TITLE + "] VARCHAR(255), " +
            " [" + TEXT + "] VARCHAR(255), " +
            " [" + IS_OUT + "] INTEGER, " +
            " [" + READ_STATE + "] INTEGER, " +
            " [" + USERS_COUNT + "] INTEGER, " +
            " [" + UNREAD_COUNT + "] INTEGER, " +
            " [" + DATE + "] INTEGER , " +
            " [" + PHOTO_50 + "] VARCHAR(255), " +
            " [" + PHOTO_100 + "] VARCHAR(255)," +
            " [" + PHOTO_200 + "] VARCHAR(255), " +
            " [" + DISABLED_FOREVER + "] INTEGER, " +
            " [" + DISABLED_UNTIL + "] INTEGER, " +
            " [" + NO_SOUND + "] INTEGER, " +
            " [" + USERS + "] BLOB, " +
            " [" + GROUPS + "] BLOB, " +
            " [" + ATTACHMENTS + "] BLOB, " +
            " [" + FWD_MESSAGES + "] BLOB, " +
            " [" + PINNED_MESSAGE + "] BLOB, " +
            " [" + LAST_MESSAGE + "] BLOB" +
            ");";

    private final static String SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE " + MESSAGES_TABLE +
            " (" + _ID + " INTEGER PRIMARY KEY, " +
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

    public synchronized static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }

        return instance;
    }

    private DatabaseHelper() {
        super(AppGlobal.context, DATABASE_NAME, null, DATABASE_VERSION);
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

    public void dropTables(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_USERS);
        db.execSQL(SQL_DELETE_GROUPS);
        db.execSQL(SQL_DELETE_FRIENDS);
        db.execSQL(SQL_DELETE_DIALOGS);
        db.execSQL(SQL_DELETE_MESSAGES);
    }
}
