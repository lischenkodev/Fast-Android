package ru.stwtforever.fast.api;


import android.text.TextUtils;

import androidx.annotation.Nullable;

import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.database.CacheStorage;

public class UserConfig {

    public static final int FAST_ID = 6964679;

    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_ID = "user_id";
    private static final String EMAIL = "email";
    private static final String API_ID = "api_id";

    public static VKUser user;

    public static String accessToken;
    private static String email;
    public static int userId;
    private static int apiId;

    public UserConfig(String accessToken, @Nullable String email, int userId, int apiId) {
        UserConfig.accessToken = accessToken;
        UserConfig.email = email;
        UserConfig.userId = userId;
        UserConfig.apiId = apiId;
    }

    public static boolean isLoggedIn() {
        return (userId > 0 && !TextUtils.isEmpty(accessToken));
    }

    public static VKUser getUser() {
        return CacheStorage.getUser(userId);
    }

    public static void updateUser() {
        user = getUser();
    }

    public UserConfig() {
    }

    public boolean save() {
        AppGlobal.preferences.edit()
                .putInt(USER_ID, userId)
                .putInt(API_ID, apiId)
                .putString(ACCESS_TOKEN, accessToken)
                .putString(EMAIL, email)
                .apply();

        return true;
    }

    public static UserConfig restore() {
        int userId = AppGlobal.preferences.getInt(USER_ID, -1);
        int apiId = AppGlobal.preferences.getInt(API_ID, -1);
        String accessToken = AppGlobal.preferences.getString(ACCESS_TOKEN, null);
        String email = AppGlobal.preferences.getString(EMAIL, null);

        return new UserConfig(accessToken, email, userId, apiId);
    }

    public static void clear() {
        AppGlobal.preferences.edit()
                .remove(ACCESS_TOKEN)
                .remove(API_ID)
                .remove(USER_ID)
                .remove(EMAIL)
                .apply();
    }
}
