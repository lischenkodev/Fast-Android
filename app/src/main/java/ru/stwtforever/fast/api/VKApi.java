package ru.stwtforever.fast.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.stwtforever.fast.BuildConfig;
import ru.stwtforever.fast.api.method.AppMethodSetter;
import ru.stwtforever.fast.api.method.MessageMethodSetter;
import ru.stwtforever.fast.api.method.MethodSetter;
import ru.stwtforever.fast.api.method.UserMethodSetter;
import ru.stwtforever.fast.api.model.VKApp;
import ru.stwtforever.fast.api.model.VKAttachments;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKLongPollServer;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKModel;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.net.HttpRequest;
import ru.stwtforever.fast.util.ArrayUtil;

public class VKApi {
    public static final String TAG = "Fast.VKApi";
    public static final String BASE_URL = "https://api.vk.com/method/";
    public static final String API_VERSION = "5.92";

    public static UserConfig config;
    public static String lang = AppGlobal.locale.getLanguage();

    public static <T> ArrayList<T> execute(String url, Class<T> cls) throws Exception {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "url: " + url);
        }

        String buffer = HttpRequest.get(url).asString();

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "json: " + buffer);
        }

        JSONObject json = new JSONObject(buffer);
        try {
            checkError(json, url);
        } catch (VKException ex) {
            if (ex.code == ErrorCodes.TOO_MANY_REQUESTS) {
                return execute(url, cls);
            } else throw ex;
        }

        if (cls == null) {
            return null;
        }

        if (cls == VKLongPollServer.class) {
            VKLongPollServer server = new VKLongPollServer(json.optJSONObject("response"));
            return (ArrayList<T>) ArrayUtil.singletonList(server);
        }

        if (cls == Boolean.class) {
            boolean value = json.optInt("response") == 1;
            return (ArrayList<T>) ArrayUtil.singletonList(value);
        }

        if (cls == Long.class) {
            long value = json.optLong("response");
            return (ArrayList<T>) ArrayUtil.singletonList(value);
        }

        if (cls == Integer.class) {
            int value = json.optInt("response");
            return (ArrayList<T>) ArrayUtil.singletonList(value);
        }

        JSONArray array = optItems(json);
        ArrayList<T> models = new ArrayList<>(array.length());

        if (cls == VKUser.class) {
            if (url.contains("friends.get")) {
                VKUser.count = json.optJSONObject("response").optInt("count");
            }

            for (int i = 0; i < array.length(); i++) {
                models.add((T) new VKUser(array.optJSONObject(i)));
            }
        } else if (cls == VKMessage.class) {
            if (url.contains("messages.getHistory")) {
                VKMessage.lastHistoryCount = json.optJSONObject("response").optInt("count");

                JSONArray groups = json.optJSONObject("response").optJSONArray("groups");
                if (groups != null && groups.length() > 0) {
                    VKMessage.groups = VKGroup.parse(groups);
                }

                JSONArray profiles = json.optJSONObject("response").optJSONArray("profiles");
                if (profiles != null && profiles.length() > 0) {
                    VKMessage.users = VKUser.parse(profiles);
                }
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject source = array.optJSONObject(i);
                int unread = source.optInt("unread");
                if (source.has("message")) {
                    source = source.optJSONObject("message");
                }
                VKMessage message = new VKMessage(source);
                message.unread = unread;
                models.add((T) message);
            }
        } else if (cls == VKGroup.class) {
            for (int i = 0; i < array.length(); i++) {
                models.add((T) new VKGroup(array.optJSONObject(i)));
            }
        } else if (cls == VKApp.class) {
            for (int i = 0; i < array.length(); i++) {
                models.add((T) new VKApp(array.optJSONObject(i)));
            }
        } else if (cls == VKModel.class && url.contains("messages.getHistoryAttachments")) {
            return (ArrayList<T>) VKAttachments.parse(array);
        } else if (cls == VKConversation.class) {
            if (url.contains("messages.getConversations")) {
                VKConversation.count = json.optJSONObject("response").optInt("count");

                JSONArray groups = json.optJSONObject("response").optJSONArray("groups");
                if (groups != null && groups.length() > 0) {
                    VKConversation.groups = VKGroup.parse(groups);
                }

                JSONArray profiles = json.optJSONObject("response").optJSONArray("profiles");
                if (profiles != null && profiles.length() > 0) {
                    VKConversation.users = VKUser.parse(profiles);
                }

            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject source = array.optJSONObject(i);
                JSONObject json_conversation = source.optJSONObject("conversation");
                JSONObject json_last_message = source.optJSONObject("last_message");

                VKConversation conversation = new VKConversation(json_conversation, json_last_message);

                models.add((T) conversation);
            }
        }
        return models;
    }

    public static <E> void execute(final String url, final Class<E> cls,
                                   final OnResponseListener<E> listener) {
        ThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<E> models = execute(url, cls);
                    if (listener != null) {
                        AppGlobal.handler.post(new SuccessCallback<>(listener, models));
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (listener != null) {
                        AppGlobal.handler.post(new ErrorCallback(listener, e));
                    }
                }
            }
        });
    }

    private static JSONArray optItems(JSONObject source) {
        Object response = source.opt("response");
        if (response instanceof JSONArray) {
            return (JSONArray) response;
        }

        if (response instanceof JSONObject) {
            JSONObject json = (JSONObject) response;
            return json.optJSONArray("items");
        }

        return null;
    }

    private static void checkError(JSONObject json, String url) throws VKException {
        if (json.has("error")) {
            JSONObject error = json.optJSONObject("error");

            int code = error.optInt("error_code");
            String message = error.optString("error_msg");

            VKException e = new VKException(url, message, code);
            if (code == ErrorCodes.CAPTCHA_NEEDED) {
                e.captchaImg = error.optString("captcha_img");
                e.captchaSid = error.optString("captcha_sid");
            }
            if (code == ErrorCodes.VALIDATION_REQUIRED) {
                e.redirectUri = error.optString("redirect_uri");
            }
            throw e;
        }
    }

    /**
     * Methods for users
     */
    public static VKUsers users() {
        return new VKUsers();
    }

    /**
     * Methods for friends
     */
    public static VKFriends friends() {
        return new VKFriends();
    }

    /**
     * Methods for messages
     */
    public static VKMessages messages() {
        return new VKMessages();
    }

    /**
     * Methods for conversation_groups
     */
    public static VKGroups groups() {
        return new VKGroups();
    }

    /**
     * Methods for apps
     */
    public static VKApps apps() {
        return new VKApps();
    }

    /**
     * Methods for account
     */
    public static VKAccounts account() {
        return new VKAccounts();
    }

    public static VKStats stats() {
        return new VKStats();
    }

    public static class VKStats {
        public VKStats() {
        }

        public MethodSetter trackVisitor() {
            return new MethodSetter("stats.trackVisitor");
        }
    }

    public static class VKFriends {
        private VKFriends() {

        }

        public MethodSetter get() {
            return new MethodSetter("friends.get");
        }

        public MethodSetter delete() {
            return new MethodSetter("friends.delete");
        }
    }

    public static class VKUsers {
        private VKUsers() {

        }

        public UserMethodSetter get() {
            return new UserMethodSetter("users.get");
        }
    }

    public static class VKMessages {
        private VKMessages() {

        }

        /**
         * Returns the list of dialogs of the current user
         */
        public MessageMethodSetter getConversations() {
            return new MessageMethodSetter("getConversations");
        }

        /**
         * Returns messages by their IDs
         */
        public MessageMethodSetter getById() {
            return new MessageMethodSetter("getById");
        }

        public MessageMethodSetter edit() {
            return new MessageMethodSetter("edit");
        }

        public MessageMethodSetter unpin() {
            return new MessageMethodSetter("unpin");
        }

        public MessageMethodSetter pin() {
            return new MessageMethodSetter("pin");
        }

        /**
         * Returns a list of the current user's private messages,
         * that match search criteria
         */
        public MessageMethodSetter search() {
            return new MessageMethodSetter("search");
        }

        /**
         * Returns a list of the current user's private messages,
         * that match search criteria
         */
        public MessageMethodSetter getHistory() {
            return new MessageMethodSetter("getHistory");
        }

        /**
         * Returns media files from the dialog or group chat
         * <p/>
         * Result:
         * Returns a list of photo, video, audio or doc objects depending
         * on media_type parameter value
         * and additional next_from field containing new offset value
         */
        public MessageMethodSetter getHistoryAttachments() {
            return new MessageMethodSetter("getHistoryAttachments");
        }

        /**
         * Sends a message
         */
        public MessageMethodSetter send() {
            return new MessageMethodSetter("send");
        }

        /**
         * Sends a sticker
         * <p/>
         * Result:
         * After successful execution, returns the sent message ID (id).
         * <p/>
         * Error codes:
         * 900	Cannot send sticker to user from blacklist
         */
        public MessageMethodSetter sendSticker() {
            return new MessageMethodSetter("sendSticker");
        }

        /**
         * Deletes one or more messages
         * <p/>
         * http://vk.com/dev/messages.delete
         */
        public MessageMethodSetter delete() {
            return new MessageMethodSetter("delete");
        }

        /**
         * Deletes all private messages in a conversation
         * NOTE: If the number of messages exceeds the maximum,
         * the method shall be called several times
         */
        public MessageMethodSetter deleteConversation() {
            return new MessageMethodSetter("deleteConversation");
        }

        /**
         * Restores a deleted message
         */
        public MessageMethodSetter restore() {
            return new MessageMethodSetter("restore");
        }

        /**
         * Marks messages as read
         */
        public MessageMethodSetter markAsRead() {
            return new MessageMethodSetter("markAsRead");
        }

        /**
         * Marks and unmarks messages as important (starred)
         */
        public MessageMethodSetter markAsImportant() {
            return new MessageMethodSetter("markAsImportant");
        }

        /**
         * Returns data required for connection to a Long Poll server.
         * With Long Poll connection,
         * you can immediately know about incoming messages and other events.
         * <p/>
         * Result:
         * Returns an object with key, server, ts fields.
         * With such data you can connect to an instant message server
         * to immediately receive incoming messages and other events
         */
        public MessageMethodSetter getLongPollServer() {
            return new MessageMethodSetter("getLongPollServer");
        }

        /**
         * Returns updates in user's private messages.
         * To speed up handling of private messages,
         * it can be useful to cache previously loaded messages on
         * a user's mobile device/desktop, to prevent re-receipt at each call.
         * With this method, you can synchronize a local copy of
         * the message list with the actual version.
         * <p/>
         * Result:
         * Returns an object that contains the following fields:
         * 1 — history:     An array similar to updates field returned
         * from the Long Poll server,
         * with these exceptions:
         * - For events with code 4 (addition of a new message),
         * there are no fields except the first three.
         * - There are no events with codes 8, 9 (friend goes online/offline)
         * or with codes 61, 62 (typing during conversation/chat).
         * <p/>
         * 2 — messages:    An array of private message objects that were found
         * among events with code 4 (addition of a new message)
         * from the history field.
         * Each object of message contains a set of fields described here.
         * The first array element is the total number of messages
         */
        public MessageMethodSetter getLongPollHistory() {
            return new MessageMethodSetter(("getLongPollHistory"));
        }

        /**
         * Returns information about a chat
         * <p/>
         * Returns a list of chat objects.
         * If the fields parameter is set,
         * the users field contains a list of user objects with
         * an additional invited_by field containing the ID of the user who
         * invited the current user to chat.
         * <p/>
         * http://vk.com/dev/messages.getChat
         */
        public MessageMethodSetter getChat() {
            return new MessageMethodSetter("getChat");
        }

        /**
         * Creates a chat with several participants
         * <p/>
         * Returns the ID of the created chat (chat_id).
         * <p/>
         * Errors:
         * 9	Flood control
         * http://vk.com/dev/messages.createChat
         */
        public MessageMethodSetter createChat() {
            return new MessageMethodSetter("createChat");
        }

        /**
         * Edits the title of a chat
         * <p/>
         * Result:
         * Returns 1
         * <p/>
         * http://vk.com/dev/messages.editChat
         */
        public MessageMethodSetter editChat() {
            return new MessageMethodSetter("editChat");
        }

        /**
         * Returns a list of IDs of users participating in a chat
         * <p/>
         * Result:
         * Returns a list of IDs of chat participants.
         * <p/>
         * If fields is set, the user fields contains a list of user objects
         * with an additional invited_by field containing the ID
         * of the user who invited the current user to chat.
         * <p/>
         * http://vk.com/dev/messages.getChatUsers
         */
        public MessageMethodSetter getChatUsers() {
            return new MessageMethodSetter("getChatUsers");
        }

        /**
         * Changes the status of a user as typing in a conversation
         * <p/>
         * Result:
         * Returns 1.
         * "User N is typing..." is shown for 10 seconds
         * after the method is called, or until the message is sent.
         * <p/>
         * http://vk.com/dev/messages.setActivity
         */
        public MessageMethodSetter setActivity() {
            return new MessageMethodSetter("setActivity").type(true);
        }

        /**
         * Adds a new user to a chat.
         * <p/>
         * Result:
         * Returns 1.
         * <p/>
         * Errors:
         * 103	Out of limits
         * <p/>
         * See https://vk.com/dev/messages.addChatUser
         */
        public MessageMethodSetter addChatUser() {
            return new MessageMethodSetter("addChatUser");
        }

        /**
         * Allows the current user to leave a chat or, if the current user started the chat,
         * allows the user to remove another user from the chat.
         * <p/>
         * Result:
         * Returns 1
         */
        public MessageMethodSetter removeChatUser() {
            return new MessageMethodSetter("removeChatUser");
        }
    }

    public static class VKGroups {
        public MethodSetter getById() {
            return new MethodSetter("conversation_groups.getById");
        }

        public MethodSetter join() {
            return new MethodSetter("conversation_groups.join");
        }
    }

    public static class VKApps {
        /**
         * Returns information about applications on platform vk
         */
        public AppMethodSetter get() {
            return new AppMethodSetter("apps.get");
        }
    }

    public static class VKAccounts {

        /**
         * Marks a current user as offline.
         */
        public MethodSetter setOffline() {
            return new MethodSetter("account.setOffline");
        }

        /**
         * Marks the current user as online for 15 minutes.
         */
        public MethodSetter setOnline() {
            return new MethodSetter("account.setOnline");
        }
    }

    /**
     * Callback for Async execute
     */
    public interface OnResponseListener<E> {
        /**
         * Called when successfully receiving the response from web
         *
         * @param models parsed json objects
         */
        void onSuccess(ArrayList<E> models);

        /**
         * Called when an error occurs on the server side
         * Visit website to get description of error codes: http://vk.com/dev/errors
         * and {@link ErrorCodes}
         * It is useful if the server requires you to enter a captcha
         *
         * @param ex the information of error
         */
        void onError(Exception ex);
    }

    private static class SuccessCallback<E> implements Runnable {
        private ArrayList<E> models;
        private OnResponseListener<E> listener;

        public SuccessCallback(OnResponseListener<E> listener, ArrayList<E> models) {
            this.models = models;
            this.listener = listener;
        }

        @Override
        public void run() {
            if (listener == null) {
                return;
            }

            listener.onSuccess(models);
        }
    }

    private static class ErrorCallback implements Runnable {
        private OnResponseListener listener;
        private Exception ex;

        public ErrorCallback(OnResponseListener listener, Exception ex) {
            this.listener = listener;
            this.ex = ex;
        }

        @Override
        public void run() {
            if (listener == null) {
                return;
            }

            listener.onError(ex);
        }
    }
}
