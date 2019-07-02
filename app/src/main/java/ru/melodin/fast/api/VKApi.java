package ru.melodin.fast.api;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.melodin.fast.BuildConfig;
import ru.melodin.fast.LoginActivity;
import ru.melodin.fast.api.method.AppMethodSetter;
import ru.melodin.fast.api.method.MessageMethodSetter;
import ru.melodin.fast.api.method.MethodSetter;
import ru.melodin.fast.api.method.UserMethodSetter;
import ru.melodin.fast.api.model.VKApp;
import ru.melodin.fast.api.model.VKAttachments;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKLongPollServer;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.net.HttpRequest;
import ru.melodin.fast.service.LongPollService;
import ru.melodin.fast.util.ArrayUtil;

public class VKApi {
    public static final String BASE_URL = "https://api.vk.com/method/";
    public static final String API_VERSION = "5.103";
    private static final String TAG = "Fast.VKApi";
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
            if (ex.getCode() == ErrorCodes.TOO_MANY_REQUESTS) {
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
                message.setUnread(unread);
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
            if (url.contains("messages.getConversations?")) {
                JSONObject response = json.optJSONObject("response");
                VKConversation.count = response.optInt("count");

                JSONArray groups = response.optJSONArray("groups");
                if (groups != null && groups.length() > 0) {
                    VKConversation.groups = VKGroup.parse(groups);
                }

                JSONArray profiles = response.optJSONArray("profiles");
                if (profiles != null && profiles.length() > 0) {
                    VKConversation.users = VKUser.parse(profiles);
                }

            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject source = array.optJSONObject(i);
                VKConversation conversation;
                if (url.contains("getConversations?")) {
                    JSONObject json_conversation = source.optJSONObject("conversation");
                    JSONObject json_last_message = source.optJSONObject("last_message");

                    conversation = new VKConversation(json_conversation, json_last_message);
                } else {
                    conversation = new VKConversation(source, null);
                }
                models.add((T) conversation);
            }
        }
        return models;
    }

    public static <E> void execute(final String url, final Class<E> cls,
                                   final OnResponseListener<E> listener) {
        ThreadExecutor.execute(() -> {
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

    public static void checkError(Activity activity, Exception e) {
        if (!(e instanceof VKException)) return;
        VKException exception = (VKException) e;
        if (exception.getCode() == ErrorCodes.USER_AUTHORIZATION_FAILED) {
            activity.finishAffinity();
            activity.startActivity(new Intent(activity, LoginActivity.class));
            activity.stopService(new Intent(activity, LongPollService.class));
            UserConfig.clear();
            DatabaseHelper.getInstance().dropTables(AppGlobal.database);
            DatabaseHelper.getInstance().onCreate(AppGlobal.database);
        }
    }

    private static void checkError(JSONObject json, String url) throws VKException {
        if (json.has("error")) {
            JSONObject error = json.optJSONObject("error");

            int code = error.optInt("error_code");
            String message = error.optString("error_msg");

            VKException e = new VKException(url, message, code);
            if (code == ErrorCodes.CAPTCHA_NEEDED) {
                e.setCaptchaImg(error.optString("captcha_img"));
                e.setCaptchaSid(error.optString("captcha_sid"));
            }
            if (code == ErrorCodes.VALIDATION_REQUIRED) {
                e.setRedirectUri(error.optString("redirect_uri"));
            }
            throw e;
        }
    }

    public static VKUsers users() {
        return new VKUsers();
    }

    public static VKFriends friends() {
        return new VKFriends();
    }

    public static VKMessages messages() {
        return new VKMessages();
    }

    public static VKGroups groups() {
        return new VKGroups();
    }

    public static VKApps apps() {
        return new VKApps();
    }

    public static VKAccounts account() {
        return new VKAccounts();
    }

    public static VKStats stats() {
        return new VKStats();
    }

    public interface OnResponseListener<E> {
        void onSuccess(ArrayList<E> models);

        void onError(Exception e);
    }

    public static class VKStats {
        VKStats() {
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

        public MessageMethodSetter getConversations() {
            return new MessageMethodSetter("getConversations");
        }

        public MessageMethodSetter getConversationsById() {
            return new MessageMethodSetter("getConversationsById");
        }

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

        public MessageMethodSetter search() {
            return new MessageMethodSetter("search");
        }

        public MessageMethodSetter getHistory() {
            return new MessageMethodSetter("getHistory");
        }

        public MessageMethodSetter getHistoryAttachments() {
            return new MessageMethodSetter("getHistoryAttachments");
        }

        public MessageMethodSetter send() {
            return new MessageMethodSetter("send");
        }

        public MessageMethodSetter sendSticker() {
            return new MessageMethodSetter("sendSticker");
        }

        public MessageMethodSetter delete() {
            return new MessageMethodSetter("delete");
        }

        public MessageMethodSetter deleteConversation() {
            return new MessageMethodSetter("deleteConversation");
        }

        public MessageMethodSetter restore() {
            return new MessageMethodSetter("restore");
        }

        public MessageMethodSetter markAsRead() {
            return new MessageMethodSetter("markAsRead");
        }

        public MessageMethodSetter markAsImportant() {
            return new MessageMethodSetter("markAsImportant");
        }

        public MessageMethodSetter getLongPollServer() {
            return new MessageMethodSetter("getLongPollServer");
        }

        public MessageMethodSetter getLongPollHistory() {
            return new MessageMethodSetter(("getLongPollHistory"));
        }

        public MessageMethodSetter getChat() {
            return new MessageMethodSetter("getChat");
        }

        public MessageMethodSetter createChat() {
            return new MessageMethodSetter("createChat");
        }

        public MessageMethodSetter editChat() {
            return new MessageMethodSetter("editChat");
        }

        public MessageMethodSetter getChatUsers() {
            return new MessageMethodSetter("getChatUsers");
        }

        public MessageMethodSetter setActivity() {
            return new MessageMethodSetter("setActivity").type(true);
        }

        public MessageMethodSetter addChatUser() {
            return new MessageMethodSetter("addChatUser");
        }

        public MessageMethodSetter removeChatUser() {
            return new MessageMethodSetter("removeChatUser");
        }
    }

    public static class VKGroups {
        public MethodSetter getById() {
            return new MethodSetter("groups.getById");
        }

        public MethodSetter join() {
            return new MethodSetter("groups.join");
        }
    }

    public static class VKApps {
        public AppMethodSetter get() {
            return new AppMethodSetter("apps.get");
        }
    }

    public static class VKAccounts {
        public MethodSetter setOffline() {
            return new MethodSetter("account.setOffline");
        }

        public MethodSetter setOnline() {
            return new MethodSetter("account.setOnline");
        }

        public MethodSetter setSilenceMode() {
            return new MethodSetter("account.setSilenceMode");
        }
    }

    private static class SuccessCallback<E> implements Runnable {
        private ArrayList<E> models;
        private OnResponseListener<E> listener;

        SuccessCallback(OnResponseListener<E> listener, ArrayList<E> models) {
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

        ErrorCallback(OnResponseListener listener, Exception ex) {
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
