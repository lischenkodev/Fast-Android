package ru.melodin.fast.api;


import android.text.TextUtils;

import androidx.annotation.Nullable;

import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.database.CacheStorage;

public class UserConfig {

    public static final int FAST_ID = 6964679;
    public static final int VK_DESKTOP_ID = 6717234;

    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_ID = "user_id";
    private static final String EMAIL = "email";
    private static final String API_ID = "api_id";

    public static VKUser user;

    public static String accessToken;
    public static int userId;
    private static String email;
    private static int apiId;

    public UserConfig(String accessToken, @Nullable String email, int userId, int apiId) {
        UserConfig.accessToken = accessToken;
        UserConfig.email = email;
        UserConfig.userId = userId;
        UserConfig.apiId = apiId;
    }

    public UserConfig() {
    }

    public static boolean isLoggedIn() {
        return (userId > 0 && !TextUtils.isEmpty(accessToken.trim()));
    }

    public static VKUser getUser() {
        user = CacheStorage.getUser(userId);
        return user;
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

    public static void save() {
        AppGlobal.preferences.edit()
                .putInt(USER_ID, userId)
                .putInt(API_ID, apiId)
                .putString(ACCESS_TOKEN, accessToken)
                .putString(EMAIL, email)
                .apply();

    }
}
