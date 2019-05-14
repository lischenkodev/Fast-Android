package ru.stwtforever.fast.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Locale;

import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Util;

import static ru.stwtforever.fast.common.AppGlobal.database;
import static ru.stwtforever.fast.database.DatabaseHelper.ADMIN_LEVER;
import static ru.stwtforever.fast.database.DatabaseHelper.ATTACHMENTS;
import static ru.stwtforever.fast.database.DatabaseHelper.CONVERSATION_TYPE;
import static ru.stwtforever.fast.database.DatabaseHelper.DATE;
import static ru.stwtforever.fast.database.DatabaseHelper.DEACTIVATED;
import static ru.stwtforever.fast.database.DatabaseHelper.DESCRIPTION;
import static ru.stwtforever.fast.database.DatabaseHelper.DIALOGS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.DISABLED_FOREVER;
import static ru.stwtforever.fast.database.DatabaseHelper.DISABLED_UNTIL;
import static ru.stwtforever.fast.database.DatabaseHelper.FIRST_NAME;
import static ru.stwtforever.fast.database.DatabaseHelper.FRIENDS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.FRIEND_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.FROM_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.FWD_MESSAGES;
import static ru.stwtforever.fast.database.DatabaseHelper.GROUPS;
import static ru.stwtforever.fast.database.DatabaseHelper.GROUPS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.GROUP_ID;
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
import static ru.stwtforever.fast.database.DatabaseHelper.PEER_ID;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_100;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_200;
import static ru.stwtforever.fast.database.DatabaseHelper.PHOTO_50;
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
import static ru.stwtforever.fast.database.DatabaseHelper.USERS;
import static ru.stwtforever.fast.database.DatabaseHelper.USERS_COUNT;
import static ru.stwtforever.fast.database.DatabaseHelper.USERS_TABLE;
import static ru.stwtforever.fast.database.DatabaseHelper.USER_ID;

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

    public static ArrayList<VKConversation> getConversations() {
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
        if (ArrayUtil.isEmpty(values)) return;
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

    @SuppressWarnings("unchecked")
    private static VKConversation parseDialog(Cursor cursor) {
        VKConversation dialog = new VKConversation();

        dialog.read = getInt(cursor, READ_STATE) == 1;
        dialog.title = getString(cursor, TITLE);
        dialog.membersCount = getInt(cursor, USERS_COUNT);
        dialog.unread = getInt(cursor, UNREAD_COUNT);

        dialog.no_sound = getInt(cursor, NO_SOUND) == 1;
        dialog.disabled_forever = getInt(cursor, DISABLED_FOREVER) == 1;
        dialog.disabled_until = getInt(cursor, DISABLED_UNTIL);

        dialog.type = getString(cursor, CONVERSATION_TYPE);

        dialog.photo_50 = getString(cursor, PHOTO_50);
        dialog.photo_100 = getString(cursor, PHOTO_100);
        dialog.photo_200 = getString(cursor, PHOTO_200);

        dialog.last = (VKMessage) Util.deserialize(getBlob(cursor, LAST_MESSAGE));
        dialog.pinned = (VKMessage) Util.deserialize(getBlob(cursor, PINNED_MESSAGE));
        dialog.conversation_users = (ArrayList) Util.deserialize(getBlob(cursor, USERS));
        dialog.conversation_groups = (ArrayList) Util.deserialize(getBlob(cursor, GROUPS));
        return dialog;
    }

    @SuppressWarnings("unchecked")
    private static VKMessage parseMessage(Cursor cursor) {
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
        message.attachments = (ArrayList) Util.deserialize(getBlob(cursor, ATTACHMENTS));
        message.fwd_messages = (ArrayList) Util.deserialize(getBlob(cursor, FWD_MESSAGES));
        message.history_users = (ArrayList) Util.deserialize(getBlob(cursor, USERS));
        message.history_groups = (ArrayList) Util.deserialize(getBlob(cursor, GROUPS));
        return message;
    }

    private static VKGroup parseGroup(Cursor cursor) {
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

        if (!ArrayUtil.isEmpty(dialog.conversation_groups)) {
            values.put(GROUPS, Util.serialize(dialog.conversation_groups));
        }

        if (!ArrayUtil.isEmpty(dialog.conversation_users)) {
            values.put(USERS, Util.serialize(dialog.conversation_users));
        }

        if (dialog.last != null) {
            values.put(LAST_MESSAGE, Util.serialize(dialog.last));
        }

        if (dialog.pinned != null) {
            values.put(PINNED_MESSAGE, Util.serialize(dialog.pinned));
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

        if (!ArrayUtil.isEmpty(message.history_groups)) {
            values.put(GROUPS, Util.serialize(message.history_groups));
        }

        if (!ArrayUtil.isEmpty(message.history_users)) {
            values.put(USERS, Util.serialize(message.history_users));
        }

        if (!ArrayUtil.isEmpty(message.attachments)) {
            values.put(ATTACHMENTS, Util.serialize(message.attachments));
        }
        if (!ArrayUtil.isEmpty(message.fwd_messages)) {
            values.put(FWD_MESSAGES, Util.serialize(message.fwd_messages));
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
}
