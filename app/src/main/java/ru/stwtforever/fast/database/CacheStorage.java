package ru.stwtforever.fast.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.model.VKAudio;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKPhoto;
import ru.stwtforever.fast.api.model.VKPhotoSizes;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;

import static ru.stwtforever.fast.common.AppGlobal.database;
import static ru.stwtforever.fast.database.DatabaseHelper.ADMIN_LEVER;
import static ru.stwtforever.fast.database.DatabaseHelper.ALBUM_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.ARTIST;
import static ru.stwtforever.fast.database.DatabaseHelper.ATTACHMENTS;
import static ru.stwtforever.fast.database.DatabaseHelper.AUDIO_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.CONVERSATION_TYPE;
import static ru.stwtforever.fast.database.DatabaseHelper.DATE;
import static ru.stwtforever.fast.database.DatabaseHelper.DEACTIVATED;
import static ru.stwtforever.fast.database.DatabaseHelper.DESCRIPTION;
import static ru.stwtforever.fast.database.DatabaseHelper.DIALOGS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.DISABLED_FOREVER;
import static ru.stwtforever.fast.database.DatabaseHelper.DISABLED_UNTIL;
import static ru.stwtforever.fast.database.DatabaseHelper.DURATION;
import static ru.stwtforever.fast.database.DatabaseHelper.FAILED_MESSAGES_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.FIRST_NAME;
import static ru.stwtforever.fast.database.DatabaseHelper.FRIENDS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.FRIEND_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.FROM_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.FWD_MESSAGES;
import static ru.stwtforever.fast.database.DatabaseHelper.GROUPS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.GROUP_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.HEIGHT;
import static ru.stwtforever.fast.database.DatabaseHelper.IMPORTANT;
import static ru.stwtforever.fast.database.DatabaseHelper.IS_ADMIN;
import static ru.stwtforever.fast.database.DatabaseHelper.IS_CLOSED;
import static ru.stwtforever.fast.database.DatabaseHelper.IS_OUT;
import static ru.stwtforever.fast.database.DatabaseHelper.LAST_MESSAGE;
import static ru.stwtforever.fast.database.DatabaseHelper.LAST_NAME;
import static ru.stwtforever.fast.database.DatabaseHelper.LAST_SEEN;
import static ru.stwtforever.fast.database.DatabaseHelper.MEMBERS_COUNT;
import static ru.stwtforever.fast.database.DatabaseHelper.MESSAGES_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.MESSAGE_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.NAME;
import static ru.stwtforever.fast.database.DatabaseHelper.NO_SOUND;
import static ru.stwtforever.fast.database.DatabaseHelper.ONLINE;
import static ru.stwtforever.fast.database.DatabaseHelper.ONLINE_APP;
import static ru.stwtforever.fast.database.DatabaseHelper.ONLINE_MOBILE;
import static ru.stwtforever.fast.database.DatabaseHelper.OWNER_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.PEER_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTOS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_100;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_200;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_50;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_SIZES;
import static ru.stwtforever.fast.database.DatabaseHelper.PINNED_MESSAGE;
import static ru.stwtforever.fast.database.DatabaseHelper.READ_STATE;
import static ru.stwtforever.fast.database.DatabaseHelper.SCREEN_NAME;
import static ru.stwtforever.fast.database.DatabaseHelper.SEX;
import static ru.stwtforever.fast.database.DatabaseHelper.STATUS;
import static ru.stwtforever.fast.database.DatabaseHelper.TEXT;
import static ru.stwtforever.fast.database.DatabaseHelper.TITLE;
import static ru.stwtforever.fast.database.DatabaseHelper.TYPE;
import static ru.stwtforever.fast.database.DatabaseHelper.UNREAD_COUNT;
import static ru.stwtforever.fast.database.DatabaseHelper.UPDATE_TIME;
import static ru.stwtforever.fast.database.DatabaseHelper.URL;
import static ru.stwtforever.fast.database.DatabaseHelper.USERS_COUNT;
import static ru.stwtforever.fast.database.DatabaseHelper.USERS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.USER_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.WIDTH;
import static ru.stwtforever.fast.database.DatabaseHelper._ID;

public class CacheStorage {
    public static void checkOpen() {
        if (!database.isOpen()) {
            database = DatabaseHelper.getInstance().getWritableDatabase();
        }
    }

    private static Cursor selectCursor(String table, String column, Object value) {
        return QueryBuilder.query()
                .select("*").from(table)
                .where(column.concat(" = ").concat(String.valueOf(value)))
                .asCursor(database);
    }

    private static Cursor selectCursor(String table, String column, int... ids) {
        StringBuilder where = new StringBuilder(5 * ids.length);

        where.append(column);
        where.append(" = ");
        where.append(ids[0]);
        for (int i = 1; i < ids.length; i++) {
            where.append(" OR ");
            where.append(column);
            where.append(" = ");
            where.append(ids[i]);
        }
        return selectCursor(table, where.toString());
    }

    private static Cursor selectCursor(String table, String where) {
        return QueryBuilder.query()
                .select("*").from(table).where(where)
                .asCursor(database);
    }

    private static Cursor selectCursor(String table) {
        checkOpen();
        return QueryBuilder.query()
                .select("*").from(table)
                .asCursor(database);
    }

    private static int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    private static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private static Long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    private static byte[] getBlob(Cursor cursor, String columnName) {
        return cursor.getBlob(cursor.getColumnIndex(columnName));
    }

    public static VKUser getUser(int id) {
        Cursor cursor = selectCursor(USERS_TABLE, USER_ID, id);
        if (cursor.moveToFirst()) {
            return parseUser(cursor);
        }
        cursor.close();
        return null;
    }

    public static VKGroup getGroup(int id) {
        Cursor cursor = selectCursor(GROUPS_TABLE, GROUP_ID, id);
        if (cursor.moveToFirst()) {
            return parseGroup(cursor);
        }
        cursor.close();
        return null;
    }

    public static VKPhoto getPhoto(int id) {
        Cursor cursor = selectCursor(PHOTOS_TABLE, _ID, id);
        if (cursor.moveToFirst()) {
            return parsePhoto(cursor);
        }

        cursor.close();
        return null;
    }

    public static ArrayList<VKMessage> getFailedMessages(int peerId) {
        Cursor c = selectCursor(FAILED_MESSAGES_TABLE, PEER_ID, peerId);
        ArrayList<VKMessage> failed = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            failed.add(parseFailed(c));
        }

        c.close();
        return failed;
    }

    public static ArrayList<VKUser> getUsers(int ids) {
        Cursor cursor = selectCursor(USERS_TABLE, USER_ID, ids);
        ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            users.add(parseUser(cursor));
        }

        cursor.close();
        return users;
    }

    public static ArrayList<VKUser> getFriends(int userId, boolean onlyOnline) {
        Cursor cursor = QueryBuilder.query()
                .select("*")
                .from(FRIENDS_TABLE)
                .leftJoin(USERS_TABLE)
                .on("friends.friend_id = users.user_id")
                .where("friends.user_id = " + userId)
                .asCursor(database);

        ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            boolean userOnline = getInt(cursor, ONLINE) == 1;
            if (onlyOnline && !userOnline) {
                continue;
            }

            VKUser user = parseUser(cursor);
            users.add(user);
        }
        cursor.close();

        return users;
    }

    public static ArrayList<VKConversation> getDialogs() {
        Cursor cursor = selectCursor(DIALOGS_TABLE);
        if (cursor.getCount() <= 0) {
            return null;
        }

        ArrayList<VKConversation> dialogs = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            dialogs.add(parseDialog(cursor));
        }

        cursor.close();
        return dialogs;
    }

    public static ArrayList<VKGroup> getGroups() {
        Cursor cursor = selectCursor(GROUPS_TABLE);
        if (cursor.getCount() <= 0) {
            return null;
        }

        ArrayList<VKGroup> groups = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            groups.add(parseGroup(cursor));
        }

        cursor.close();
        return groups;
    }

    public static ArrayList<VKMessage> getMessages(int peerId) {
        Cursor cursor = selectCursor(MESSAGES_TABLE, String.format(AppGlobal.locale, "%s = %d", PEER_ID, peerId));

        ArrayList<VKMessage> messages = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            messages.add(parseMessage(cursor));
        }
        cursor.close();
        return messages;
    }

    private static String dialogWhere(int peerId) {
        String where = String.format(Locale.US, "%s = %d", PEER_ID, peerId);

        return where;
    }

    public static void deleteMessages(int peerId) {
        String where = dialogWhere(peerId);
        delete(MESSAGES_TABLE, where);
    }

    public static void deleteDialog(int peerId) {
        String where = dialogWhere(peerId);
        delete(DIALOGS_TABLE, where);
    }


    public static void insert(String table, ArrayList values) {
        if (!database.isOpen()) database = DatabaseHelper.getInstance().getWritableDatabase();
        if (database.isDbLockedByCurrentThread()) return;
        database.beginTransaction();

        ContentValues cv = new ContentValues();
        for (int i = 0; i < values.size(); i++) {
            switch (table) {
                case USERS_TABLE:
                    putValues(cv, (VKUser) values.get(i), false);
                    break;
                case FRIENDS_TABLE:
                    putValues(cv, (VKUser) values.get(i), true);
                    break;
                case DIALOGS_TABLE:
                    putValues(cv, (VKConversation) values.get(i));
                    break;
                case MESSAGES_TABLE:
                    putValues(cv, (VKMessage) values.get(i));
                    break;
                case GROUPS_TABLE:
                    putValues(cv, (VKGroup) values.get(i));
                    break;
                case PHOTOS_TABLE:
                    putValues(cv, (VKPhoto) values.get(i));
                    break;
            }

            database.insert(table, null, cv);
            cv.clear();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public static void delete(String table, String where) {
        database.delete(table, where, null);
    }

    public static void delete(String table) {
        database.delete(table, null, null);
    }

    private static VKUser parseUser(Cursor cursor) {
        VKUser user = new VKUser();

        user.id = getInt(cursor, USER_ID);
        user.name = getString(cursor, FIRST_NAME);
        user.surname = getString(cursor, LAST_NAME);
        user.last_seen = getInt(cursor, LAST_SEEN);
        user.screen_name = getString(cursor, SCREEN_NAME);
        user.status = getString(cursor, STATUS);
        user.photo_50 = getString(cursor, PHOTO_50);
        user.photo_100 = getString(cursor, PHOTO_100);
        user.photo_200 = getString(cursor, PHOTO_200);

        user.online = getInt(cursor, ONLINE) == 1;
        user.online_mobile = getInt(cursor, ONLINE_MOBILE) == 1;
        user.online_app = getInt(cursor, ONLINE_APP);
        user.deactivated = getString(cursor, DEACTIVATED);
        user.sex = getInt(cursor, SEX);
        return user;
    }

    public static VKConversation parseDialog(Cursor cursor) {
        VKConversation c = new VKConversation();

        c.read = getInt(cursor, READ_STATE) == 1;
        c.title = getString(cursor, TITLE);
        c.membersCount = getInt(cursor, USERS_COUNT);
        c.unread = getInt(cursor, UNREAD_COUNT);

        c.no_sound = getInt(cursor, NO_SOUND) == 1;
        c.disabled_forever = getInt(cursor, DISABLED_FOREVER) == 1;
        c.disabled_until = getInt(cursor, DISABLED_UNTIL);

        c.type = getString(cursor, CONVERSATION_TYPE);

        c.photo_50 = getString(cursor, PHOTO_50);
        c.photo_100 = getString(cursor, PHOTO_100);
        c.photo_200 = getString(cursor, PHOTO_200);

        c.last = (VKMessage) Utils.deserialize(getBlob(cursor, LAST_MESSAGE));
        c.pinned = (VKMessage) Utils.deserialize(getBlob(cursor, PINNED_MESSAGE));
        return c;
    }

    @SuppressWarnings("unchecked")
    public static VKMessage parseMessage(Cursor cursor) {
        VKMessage message = new VKMessage();
        message.id = getInt(cursor, MESSAGE_ID);
        message.peerId = getInt(cursor, PEER_ID);
        message.fromId = getInt(cursor, FROM_ID);
        message.date = getInt(cursor, DATE);
        message.text = getString(cursor, TEXT);
        message.read = getInt(cursor, READ_STATE) == 1;
        message.out = getInt(cursor, IS_OUT) == 1;
        message.important = getInt(cursor, IMPORTANT) == 1;
        message.status = getInt(cursor, STATUS);
        message.update_time = getLong(cursor, UPDATE_TIME);
        message.attachments = (ArrayList) Utils.deserialize(getBlob(cursor, ATTACHMENTS));
        message.fwd_messages = (ArrayList) Utils.deserialize(getBlob(cursor, FWD_MESSAGES));
        return message;
    }

    public static VKMessage parseFailed(Cursor c) {
        VKMessage m = new VKMessage();
        m.peerId = getInt(c, PEER_ID);
        m.fromId = getInt(c, FROM_ID);
        m.text = getString(c, TEXT);
        m.status = VKMessage.STATUS_ERROR;

        return m;
    }

    public static VKGroup parseGroup(Cursor cursor) {
        VKGroup group = new VKGroup();
        group.id = getInt(cursor, GROUP_ID);
        group.name = getString(cursor, NAME);
        group.screen_name = getString(cursor, SCREEN_NAME);
        group.description = getString(cursor, DESCRIPTION);
        group.status = getString(cursor, STATUS);
        group.type = getInt(cursor, TYPE);
        group.is_closed = getInt(cursor, IS_CLOSED);
        group.admin_level = getInt(cursor, ADMIN_LEVER);
        group.is_admin = getInt(cursor, IS_ADMIN) == 1;
        group.photo_50 = getString(cursor, PHOTO_50);
        group.photo_100 = getString(cursor, PHOTO_100);
        group.photo_200 = getString(cursor, PHOTO_200);
        group.members_count = getInt(cursor, MEMBERS_COUNT);
        return group;
    }

    public static VKPhoto parsePhoto(Cursor cursor) {
        VKPhoto photo = new VKPhoto();
        photo.id = getInt(cursor, _ID);
        photo.album_id = getInt(cursor, ALBUM_ID);
        photo.owner_id = getInt(cursor, OWNER_ID);
        photo.text = getString(cursor, TEXT);
        photo.date = getInt(cursor, DATE);
        photo.width = getInt(cursor, WIDTH);
        photo.height = getInt(cursor, HEIGHT);
        photo.sizes = (VKPhotoSizes) Utils.deserialize(getBlob(cursor, PHOTO_SIZES));
        return photo;
    }

    private static void putValues(ContentValues values, VKUser user, boolean friends) {
        if (friends) {
            values.put(USER_ID, UserConfig.userId);
            values.put(FRIEND_ID, user.id);
            return;
        }

        values.put(USER_ID, user.id);
        values.put(FIRST_NAME, user.name);
        values.put(LAST_NAME, user.surname);
        values.put(SCREEN_NAME, user.screen_name);
        values.put(LAST_SEEN, user.last_seen);
        values.put(ONLINE, user.online);
        values.put(ONLINE_MOBILE, user.online_mobile);
        values.put(ONLINE_APP, user.online_app);
        values.put(STATUS, user.status);
        values.put(PHOTO_50, user.photo_50);
        values.put(PHOTO_100, user.photo_100);
        values.put(PHOTO_200, user.photo_200);
        values.put(SEX, user.sex);
    }

    private static void putValues(ContentValues values, VKConversation dialog) {
        values.put(UNREAD_COUNT, dialog.unread);
        values.put(READ_STATE, dialog.read);
        values.put(USERS_COUNT, dialog.membersCount);
        values.put(PHOTO_50, dialog.photo_50);
        values.put(PHOTO_100, dialog.photo_100);
        values.put(PHOTO_200, dialog.photo_200);
        values.put(CONVERSATION_TYPE, dialog.type);
        values.put(DISABLED_FOREVER, dialog.disabled_forever);
        values.put(DISABLED_UNTIL, dialog.disabled_until);
        values.put(NO_SOUND, dialog.no_sound);

        if (dialog.last != null) {
            values.put(LAST_MESSAGE, Utils.serialize(dialog.last));
        }

        if (dialog.pinned != null) {
            values.put(PINNED_MESSAGE, Utils.serialize(dialog.pinned));
        }

        if (TextUtils.isEmpty(dialog.title)) {
            if (dialog.last.fromId < 0) {
                VKGroup g = getGroup(dialog.last.peerId);
                if (g != null) {
                    values.put(TITLE, g.name);
                } else {
                    values.put(TITLE, "");
                }
            } else {
                VKUser u = getUser(dialog.last.peerId);
                if (u != null) {
                    values.put(TITLE, u.toString());
                } else {
                    values.put(TITLE, "");
                }
            }
        } else {
            values.put(TITLE, dialog.title);
        }
    }

    private static void putValues(ContentValues values, VKMessage message) {
        values.put(MESSAGE_ID, message.id);
        values.put(PEER_ID, message.peerId);
        values.put(TEXT, message.text);
        values.put(DATE, message.date);
        values.put(IS_OUT, message.out);
        values.put(STATUS, message.status);
        values.put(READ_STATE, message.read);
        values.put(UPDATE_TIME, message.update_time);

        values.put(IMPORTANT, message.important);
        if (!ArrayUtil.isEmpty(message.attachments)) {
            values.put(ATTACHMENTS, Utils.serialize(message.attachments));
        }
        if (!ArrayUtil.isEmpty(message.fwd_messages)) {
            values.put(FWD_MESSAGES, Utils.serialize(message.fwd_messages));
        }

    }

    private static void putValues(ContentValues values, VKGroup group) {
        values.put(GROUP_ID, group.id);
        values.put(NAME, group.name);
        values.put(SCREEN_NAME, group.screen_name);
        values.put(DESCRIPTION, group.description);
        values.put(STATUS, group.status);
        values.put(TYPE, group.type);
        values.put(IS_CLOSED, group.is_closed);
        values.put(ADMIN_LEVER, group.admin_level);
        values.put(IS_ADMIN, group.is_admin);
        values.put(PHOTO_50, group.photo_50);
        values.put(PHOTO_100, group.photo_100);
        values.put(MEMBERS_COUNT, group.members_count);
    }

    private static void putValues(ContentValues values, VKPhoto photo) {
        values.put(_ID, photo.id);
        values.put(ALBUM_ID, photo.album_id);
        values.put(OWNER_ID, photo.owner_id);
        values.put(WIDTH, photo.width);
        values.put(HEIGHT, photo.height);
        values.put(TEXT, photo.text);
        values.put(DATE, photo.date);
        values.put(PHOTO_SIZES, Utils.serialize(photo.sizes));
    }

    private static void putValues(ContentValues values, VKAudio audio) {
        values.put(AUDIO_ID, audio.id);
        values.put(OWNER_ID, audio.owner_id);
        values.put(ARTIST, audio.artist);
        values.put(TITLE, audio.title);
        values.put(DURATION, audio.duration);
        values.put(URL, audio.url);
    }
}
