package ru.stwtforever.fast.api.method;

import android.support.v4.util.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import java.io.*;
import java.net.*;
import java.util.*;
import android.text.*;

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

    public String getSignedUrl() {
        return getSignedUrl(false);
    }

    public String getSignedUrl(boolean isPost) {
        if (!params.containsKey("access_token")) {
            params.put("access_token", VKApi.config.accessToken);
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

    public <E> ArrayList<E> execute(Class<E> cls) throws Exception {
        return VKApi.execute(getSignedUrl(), cls);
    }

    public <E> void execute(Class<E> cls, VKApi.OnResponseListener<E> listener) {
        VKApi.execute(getSignedUrl(), cls, listener);
    }

    public <E extends VKModel> ArrayList<E> tryExecute(Class<E> cls) {
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

    public MethodSetter userIds(int... ids) {
        return put("user_ids", ArrayUtil.toString(ids));
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

    /**
     * ID of community.
     */
    public MethodSetter groupId(int value) {
        return put("group_id", value);
    }

    public MethodSetter groupIds(int... ids) {
        return put("group_ids", ArrayUtil.toString(ids));
    }

    public MethodSetter groupIds(Integer... ids) {
        return put("group_ids", ArrayUtil.toString(ids));
    }

    /**
     * Profile fields separated by ','
     */
    public MethodSetter fields(String values) {
        return put("fields", values);
    }

    /**
     * Number of users/messages/audios... to return
     * NOTE: even when using the offset parameter for information available
     * only the first 1000 results!.
     */
    public MethodSetter count(int value) {
        return put("count", value);
    }

    /**
     * Sort order.
     */
    public MethodSetter sort(int value) {
        put("sort", value);
        return this;
    }

    /**
     * Order to return a list
     */
    public MethodSetter order(String value) {
        put("order", value);
        return this;
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
        return put("access_token", config.accessToken);
    }
}
