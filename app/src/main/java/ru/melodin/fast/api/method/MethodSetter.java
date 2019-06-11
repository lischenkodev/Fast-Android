package ru.melodin.fast.api.method;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.util.ArrayUtil;

public class MethodSetter {
    private String name;
    private ArrayMap<String, String> params;

    /**
     * Creates a new Method Setter
     *
     * @param name the vk method name, e.g. users.get
     */
    public MethodSetter(String name) {
        this.name = name;
        this.params = new ArrayMap<>();
    }

    public MethodSetter put(String key, Object value) {
        this.params.put(key, String.valueOf(value));
        return this;
    }

    public MethodSetter put(String key, int value) {
        this.params.put(key, String.valueOf(value));
        return this;
    }

    public MethodSetter put(String key, long value) {
        this.params.put(key, String.valueOf(value));
        return this;
    }

    public MethodSetter put(String key, boolean value) {
        this.params.put(key, value ? "1" : "0");
        return this;
    }

    private String getSignedUrl() {
        return getSignedUrl(false);
    }

    private String getSignedUrl(boolean isPost) {
        if (!params.containsKey("access_token")) {
            params.put("access_token", UserConfig.accessToken);
        }
        if (!params.containsKey("v")) {
            params.put("v", VKApi.API_VERSION);
        }
        if (!params.containsKey("lang")) {
            params.put("lang", VKApi.lang);
        }

        return VKApi.BASE_URL + name + "?" + (isPost ? "" : getParams());
    }

    public String getParams() {
        if (params == null || (params != null && params.size() == 0)) return "";

        StringBuilder buffer = new StringBuilder();
        try {

            for (int i = 0; i < params.size(); i++) {
                String key = params.keyAt(i);
                String value = params.valueAt(i);

                if (buffer.length() != 0) {
                    buffer.append("&");
                }

                buffer.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public void execute() throws Exception {
        VKApi.execute(getSignedUrl(), null);
    }

    public <E> ArrayList<E> execute(@Nullable Class<E> cls) throws Exception {
        return VKApi.execute(getSignedUrl(), cls);
    }

    public <E> void execute(@Nullable Class<E> cls, VKApi.OnResponseListener<E> listener) {
        VKApi.execute(getSignedUrl(), cls, listener);
    }

    public <E extends VKModel> ArrayList<E> tryExecute(@Nullable Class<E> cls) {
        try {
            return execute(cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * User ID. By default, the current user ID
     */
    public MethodSetter userId(int value) {
        return put("user_id", value);
    }

    public MethodSetter userIds(Integer... ids) {
        return put("user_ids", ArrayUtil.toString(ids));
    }

    public MethodSetter userIds(Collection<Integer> ids) {
        return put("user_ids", ArrayUtil.toString(ids.toArray()));
    }

    /**
     * ID of the user or community, e.g. audios.get
     */
    public MethodSetter ownerId(int value) {
        return put("owner_id", value);
    }

    public MethodSetter peerId(int value) {
        return put("peer_id", value);
    }

    public MethodSetter groupId(int value) {
        return put("group_id", value);
    }

    public MethodSetter groupIds(int... ids) {
        return put("group_ids", ArrayUtil.toString(ids));
    }

    public MethodSetter fields(String values) {
        return put("fields", values);
    }

    public MethodSetter count(int value) {
        return put("count", value);
    }

    public MethodSetter sound(boolean value) {
        return put("sound", value);
    }

    public MethodSetter sort(int value) {
        return put("sort", value);
    }

    public MethodSetter time(int value) {
        return put("time", value);
    }

    /**
     * Order to return a list
     */
    public MethodSetter order(String value) {
        return put("order", value);
    }

    /**
     * Offset needed to return a specific subset.
     */
    public MethodSetter offset(int value) {
        return put("offset", value);
    }

    /**
     * Case for declension of user name and surname:
     * nom — nominative (default)
     * gen — genitive
     * dat — dative
     * acc — accusative
     * ins — instrumental
     * abl — prepositional
     */
    public MethodSetter nameCase(String value) {
        return put("name_case", value);
    }

    /**
     * Captcha Sid, specifies for Captcha needed error.
     */
    public MethodSetter captchaSid(String value) {
        return put("captcha_sid", value);
    }

    /**
     * Captcha key, specifies for Captcha needed error.
     */
    public MethodSetter captchaKey(String value) {
        return put("captcha_key", value);
    }

    /**
     * Uses a separate config for this request (Access Token)
     */
    public MethodSetter withConfig(UserConfig config) {
        return put("access_token", UserConfig.accessToken);
    }

    public MethodSetter extended(boolean value) {
        return put("extended", value);
    }
}
