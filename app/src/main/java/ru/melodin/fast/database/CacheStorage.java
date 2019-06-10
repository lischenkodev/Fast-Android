package ru.melodin.fast.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

import static ru.melodin.fast.common.AppGlobal.database;
import static ru.melodin.fast.database.DatabaseHelper.ACTION_TEXT;
import static ru.melodin.fast.database.DatabaseHelper.ACTION_TYPE;
import static ru.melodin.fast.database.DatabaseHelper.ACTION_USER_ID;
import static ru.melodin.fast.database.DatabaseHelper.ADMIN_LEVER;
import static ru.melodin.fast.database.DatabaseHelper.ATTACHMENTS;
import static ru.melodin.fast.database.DatabaseHelper.CONVERSATION_TYPE;
import static ru.melodin.fast.database.DatabaseHelper.DATE;
import static ru.melodin.fast.database.DatabaseHelper.DEACTIVATED;
import static ru.melodin.fast.database.DatabaseHelper.DESCRIPTION;
import static ru.melodin.fast.database.DatabaseHelper.DIALOGS_TABLE;
import static ru.melodin.fast.database.DatabaseHelper.DISABLED_FOREVER;
import static ru.melodin.fast.database.DatabaseHelper.DISABLED_UNTIL;
import static ru.melodin.fast.database.DatabaseHelper.FIRST_NAME;
import static ru.melodin.fast.database.DatabaseHelper.FRIENDS_TABLE;
import static ru.melodin.fast.database.DatabaseHelper.FRIEND_ID;
import static ru.melodin.fast.database.DatabaseHelper.FROM_ID;
import static ru.melodin.fast.database.DatabaseHelper.FWD_MESSAGES;
import static ru.melodin.fast.database.DatabaseHelper.GROUPS;
import static ru.melodin.fast.database.DatabaseHelper.GROUPS_TABLE;
import static ru.melodin.fast.database.DatabaseHelper.GROUP_ID;
import static ru.melodin.fast.database.DatabaseHelper.IMPORTANT;
import static ru.melodin.fast.database.DatabaseHelper.IS_ADMIN;
import static ru.melodin.fast.database.DatabaseHelper.IS_CLOSED;
import static ru.melodin.fast.database.DatabaseHelper.IS_OUT;
import static ru.melodin.fast.database.DatabaseHelper.LAST_MESSAGE;
import static ru.melodin.fast.database.DatabaseHelper.LAST_NAME;
import static ru.melodin.fast.database.DatabaseHelper.LAST_SEEN;
import static ru.melodin.fast.database.DatabaseHelper.MEMBERS_COUNT;
import static ru.melodin.fast.database.DatabaseHelper.MESSAGES_TABLE;
import static ru.melodin.fast.database.DatabaseHelper.MESSAGE_ID;
import static ru.melodin.fast.database.DatabaseHelper.NAME;
import static ru.melodin.fast.database.DatabaseHelper.NO_SOUND;
import static ru.melodin.fast.database.DatabaseHelper.ONLINE;
import static ru.melodin.fast.database.DatabaseHelper.ONLINE_APP;
import static ru.melodin.fast.database.DatabaseHelper.ONLINE_MOBILE;
import static ru.melodin.fast.database.DatabaseHelper.PEER_ID;
import static ru.melodin.fast.database.DatabaseHelper.PHOTO_100;
import static ru.melodin.fast.database.DatabaseHelper.PHOTO_200;
import static ru.melodin.fast.database.DatabaseHelper.PHOTO_50;
import static ru.melodin.fast.database.DatabaseHelper.PINNED_MESSAGE;
import static ru.melodin.fast.database.DatabaseHelper.READ_STATE;
import static ru.melodin.fast.database.DatabaseHelper.SCREEN_NAME;
import static ru.melodin.fast.database.DatabaseHelper.SEX;
import static ru.melodin.fast.database.DatabaseHelper.STATUS;
import static ru.melodin.fast.database.DatabaseHelper.TEXT;
import static ru.melodin.fast.database.DatabaseHelper.TITLE;
import static ru.melodin.fast.database.DatabaseHelper.TYPE;
import static ru.melodin.fast.database.DatabaseHelper.UNREAD_COUNT;
import static ru.melodin.fast.database.DatabaseHelper.UPDATE_TIME;
import static ru.melodin.fast.database.DatabaseHelper.USERS;
import static ru.melodin.fast.database.DatabaseHelper.USERS_COUNT;
import static ru.melodin.fast.database.DatabaseHelper.USERS_TABLE;
import static ru.melodin.fast.database.DatabaseHelper.USER_ID;

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

    public static ArrayList<VKUser> getUsers() {
        Cursor cursor = selectCursor(USERS_TABLE);
        ArrayList<VKUser> users = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            users.add(parseUser(cursor));
        }

        cursor.close();
        return users;
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

    public static VKConversation getConversation(int peerId) {
        Cursor cursor = selectCursor(DIALOGS_TABLE, PEER_ID, peerId);
        if (cursor.moveToFirst()) {
            return parseDialog(cursor);
        }

        return null;
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
        Cursor cursor = selectCursor(MESSAGES_TABLE, dialogWhere(peerId));

        ArrayList<VKMessage> messages = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            messages.add(parseMessage(cursor));
        }
        cursor.close();
        return messages;
    }

    public static VKMessage getMessage(int mId) {
        Cursor cursor = selectCursor(MESSAGES_TABLE, String.format(AppGlobal.locale, "%s = %d", MESSAGE_ID, mId));

        if (cursor.moveToFirst())
            return parseMessage(cursor);

        return null;
    }

    private static String dialogWhere(int peerId) {
        return String.format(Locale.US, "%s = %d", PEER_ID, peerId);
    }

    public static void update(String table, Object item, Object where, Object... args) {
        update(table, new ArrayList(Collections.singleton(item)), where, args);
    }

    public static void update(String table, ArrayList values, Object where, Object... args) {
        if (ArrayUtil.isEmpty(values)) return;
        database.beginTransaction();

        ContentValues cv = new ContentValues();
        for (int i = 0; i < values.size(); i++) {
            Object item = values.get(i);
            switch (table) {
                case USERS_TABLE:
                    putValues(cv, (VKUser) item, false);
                    break;
                case FRIENDS_TABLE:
                    putValues(cv, (VKUser) item, true);
                    break;
                case DIALOGS_TABLE:
                    putValues(cv, (VKConversation) item);
                    break;
                case MESSAGES_TABLE:
                    putValues(cv, (VKMessage) item);
                    break;
                case GROUPS_TABLE:
                    putValues(cv, (VKGroup) item);
                    break;
            }


            String[] arguments = new String[args.length];
            for (int j = 0; j < args.length; j++) {
                arguments[j] = args[j] + "";
            }
            database.update(table, cv, where + " = ?", arguments);
            cv.clear();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public static void insert(String table, ArrayList values) {
        if (ArrayUtil.isEmpty(values)) return;
        database.beginTransaction();

        ContentValues cv = new ContentValues();
        for (int i = 0; i < values.size(); i++) {
            Object item = values.get(i);
            switch (table) {
                case USERS_TABLE:
                    putValues(cv, (VKUser) item, false);
                    break;
                case FRIENDS_TABLE:
                    putValues(cv, (VKUser) item, true);
                    break;
                case DIALOGS_TABLE:
                    putValues(cv, (VKConversation) item);
                    break;
                case MESSAGES_TABLE:
                    putValues(cv, (VKMessage) item);
                    break;
                case GROUPS_TABLE:
                    putValues(cv, (VKGroup) item);
                    break;
            }

            database.insert(table, null, cv);
            cv.clear();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public static void insert(String table, Object item) {
        insert(table, new ArrayList(Collections.singleton(item)));
    }

    public static void delete(String table, Object where, Object args) {
        database.delete(table, where + " = ?", new String[]{args + ""});
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

        dialog.setRead(getInt(cursor, READ_STATE) == 1);
        dialog.setTitle(getString(cursor, TITLE));
        dialog.setMembersCount(getInt(cursor, USERS_COUNT));
        dialog.setUnread(getInt(cursor, UNREAD_COUNT));

        dialog.setNoSound(getInt(cursor, NO_SOUND) == 1);
        dialog.setDisabledForever(getInt(cursor, DISABLED_FOREVER) == 1);
        dialog.setDisabledUntil(getInt(cursor, DISABLED_UNTIL));

        dialog.setType(VKConversation.getType(getString(cursor, CONVERSATION_TYPE)));

        dialog.setPhoto50(getString(cursor, PHOTO_50));
        dialog.setPhoto100(getString(cursor, PHOTO_100));
        dialog.setPhoto200(getString(cursor, PHOTO_200));

        byte[] last = getBlob(cursor, LAST_MESSAGE);
        byte[] pinned = getBlob(cursor, PINNED_MESSAGE);
        byte[] users = getBlob(cursor, USERS);
        byte[] groups = getBlob(cursor, GROUPS);

        if (last != null)
            dialog.setLast((VKMessage) Util.deserialize(last));
        if (pinned != null)
            dialog.setPinned((VKMessage) Util.deserialize(pinned));
        if (users != null)
            dialog.setConversationUsers((ArrayList) Util.deserialize(users));
        if (groups != null)
            dialog.setConversationGroups((ArrayList) Util.deserialize(groups));
        return dialog;
    }

    @SuppressWarnings("unchecked")
    private static VKMessage parseMessage(Cursor cursor) {
        VKMessage message = new VKMessage();
        message.setId(getInt(cursor, MESSAGE_ID));
        message.setPeerId(getInt(cursor, PEER_ID));
        message.setFromId(getInt(cursor, FROM_ID));
        message.setDate(getInt(cursor, DATE));
        message.setText(getString(cursor, TEXT));
        message.setRead(getInt(cursor, READ_STATE) == 1);
        message.setOut(getInt(cursor, IS_OUT) == 1);
        message.setImportant(getInt(cursor, IMPORTANT) == 1);
        //message.status = getInt(cursor, STATUS);
        message.setUpdateTime(getLong(cursor, UPDATE_TIME));
        message.setAction(VKMessage.getAction(getString(cursor, ACTION_TYPE)));
        message.setActionText(getString(cursor, ACTION_TEXT));
        message.setActionUserId(getInt(cursor, ACTION_USER_ID));

        byte[] attachments = getBlob(cursor, ATTACHMENTS);
        byte[] forwarded = getBlob(cursor, FWD_MESSAGES);
        byte[] users = getBlob(cursor, USERS);
        byte[] groups = getBlob(cursor, GROUPS);

        if (attachments != null)
            message.setAttachments((ArrayList) Util.deserialize(attachments));
        if (forwarded != null)
            message.setFwdMessages((ArrayList) Util.deserialize(forwarded));
        if (users != null)
            message.setHistoryUsers((ArrayList) Util.deserialize(users));
        if (groups != null)
            message.setHistoryGroups((ArrayList) Util.deserialize(groups));
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
        values.put(PEER_ID, dialog.getLast().getPeerId());
        values.put(UNREAD_COUNT, dialog.getUnread());
        values.put(READ_STATE, dialog.isRead());
        values.put(USERS_COUNT, dialog.getMembersCount());
        values.put(CONVERSATION_TYPE, VKConversation.getType(dialog.getType()));
        values.put(DISABLED_FOREVER, dialog.isDisabledForever());
        values.put(DISABLED_UNTIL, dialog.getDisabledUntil());
        values.put(NO_SOUND, dialog.isNoSound());

        if (!ArrayUtil.isEmpty(dialog.getConversationGroups())) {
            values.put(GROUPS, Util.serialize(dialog.getConversationGroups()));
        }

        if (!ArrayUtil.isEmpty(dialog.getConversationUsers())) {
            values.put(USERS, Util.serialize(dialog.getConversationUsers()));
        }

        if (dialog.getLast() != null) {
            values.put(LAST_MESSAGE, Util.serialize(dialog.getLast()));
        }

        if (dialog.getPinned() != null) {
            values.put(PINNED_MESSAGE, Util.serialize(dialog.getPinned()));
        }

        if (TextUtils.isEmpty(dialog.getTitle())) {
            if (dialog.isGroup()) {
                VKGroup group = getGroup(dialog.getLast().getPeerId());
                values.put(TITLE, group == null ? "" : group.name);
            } else {
                VKUser user = getUser(dialog.getLast().getPeerId());
                values.put(TITLE, user == null ? "" : user.name + " " + user.surname);
            }
        } else {
            values.put(TITLE, dialog.getTitle());
        }

        if (TextUtils.isEmpty(dialog.getPhoto50()) && TextUtils.isEmpty(dialog.getPhoto100()) && TextUtils.isEmpty(dialog.getPhoto200())) {
            if (dialog.isGroup()) {
                VKGroup group = getGroup(dialog.getLast().getPeerId());
                values.put(PHOTO_50, group == null ? "" : group.photo_50);
                values.put(PHOTO_100, group == null ? "" : group.photo_100);
                values.put(PHOTO_200, group == null ? "" : group.photo_200);
            } else {
                VKUser user = getUser(dialog.getLast().getPeerId());
                values.put(PHOTO_50, user == null ? "" : user.photo_50);
                values.put(PHOTO_100, user == null ? "" : user.photo_100);
                values.put(PHOTO_200, user == null ? "" : user.photo_200);
            }
        } else {
            values.put(PHOTO_50, dialog.getPhoto50());
            values.put(PHOTO_100, dialog.getPhoto100());
            values.put(PHOTO_200, dialog.getPhoto200());
        }
    }

    private static void putValues(ContentValues values, VKMessage message) {
        values.put(MESSAGE_ID, message.getId());
        values.put(PEER_ID, message.getPeerId());
        values.put(TEXT, message.getText());
        values.put(DATE, message.getDate());
        values.put(IS_OUT, message.isOut());
        values.put(STATUS, -1);
        values.put(READ_STATE, message.isRead());
        values.put(UPDATE_TIME, message.getUpdateTime());
        values.put(ACTION_TEXT, message.getActionText());
        values.put(ACTION_TYPE, VKMessage.getAction(message.getAction()));
        values.put(ACTION_USER_ID, message.getActionUserId());
        values.put(IMPORTANT, message.isImportant());

        if (!ArrayUtil.isEmpty(message.getHistoryGroups())) {
            values.put(GROUPS, Util.serialize(message.getHistoryGroups()));
        }

        if (!ArrayUtil.isEmpty(message.getHistoryUsers())) {
            values.put(USERS, Util.serialize(message.getHistoryUsers()));
        }

        if (!ArrayUtil.isEmpty(message.getAttachments())) {
            values.put(ATTACHMENTS, Util.serialize(message.getAttachments()));
        }
        if (!ArrayUtil.isEmpty(message.getFwdMessages())) {
            values.put(FWD_MESSAGES, Util.serialize(message.getFwdMessages()));
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
        values.put(PHOTO_200, group.photo_200);
        values.put(MEMBERS_COUNT, group.members_count);
    }
}
