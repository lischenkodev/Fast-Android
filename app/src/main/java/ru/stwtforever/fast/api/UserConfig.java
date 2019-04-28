package ru.stwtforever.fast.api;


import androidx.annotation.*;
import android.text.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.io.*;
import ru.stwtforever.fast.api.model.*;
import java.io.*;
import org.json.*;
import ru.stwtforever.fast.database.*;

public class UserConfig {
	
    public static final int FAST_ID = 6964679;
    
    public static final String ACCESS_TOKEN = "access_token";
    public static final String USER_ID = "user_id";
    public static final String EMAIL = "email";
    public static final String API_ID = "api_id";
	
    public static VKUser user;
	
    public static String accessToken;
    public static String email;
    public static int userId;
    public static int apiId;
	
    public UserConfig(String accessToken, @Nullable String email, int userId, int apiId) {
        this.accessToken = accessToken;
        this.email = email;
        this.userId = userId;
        this.apiId = apiId;
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
	
    public UserConfig(File file) {
        this.restore(file);
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
	
    public boolean save(File file) {
        JSONObject json = new JSONObject();
        try {
            json.putOpt(ACCESS_TOKEN, accessToken);
            json.putOpt(USER_ID, userId);
            json.putOpt(API_ID, apiId);
            json.putOpt(EMAIL, email);

            FileStreams.write(json.toString(), file);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
	
    public static UserConfig restore() {
        int userId = AppGlobal.preferences.getInt(USER_ID, -1);
        int apiId = AppGlobal.preferences.getInt(API_ID, -1);
        String accessToken = AppGlobal.preferences.getString(ACCESS_TOKEN, null);
        String email = AppGlobal.preferences.getString(EMAIL, null);

        return new UserConfig(accessToken, email, userId, apiId);
    }
	
    public UserConfig restore(File file) {
        try {
            String readText = FileStreams.read(file);
            JSONObject json = new JSONObject(readText);

            this.accessToken = json.optString(ACCESS_TOKEN);
            this.email = json.optString(EMAIL);
            this.userId = json.optInt(USER_ID);
            this.apiId = json.optInt(API_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
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
