package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKUser extends VKModel implements Serializable {

    public static final String FIELDS_DEFAULT = "photo_50,photo_100,photo_200,status,screen_name,online,online_mobile,last_seen,verified,sex";
    public static final VKUser EMPTY = new VKUser() {

        public String photo_100 = "", photo_50 = "", photo_200 = "";
        public String first_name = "", last_name = "";

        @Override
        public String toString() {
            return "User";
        }
    };
    public static int count;
    public int friends_count;
    public int id;
    public String name;
    public String surname;
    public String fullNname;
    public String screen_name;
    public boolean online;
    public boolean online_mobile;
    public int online_app;
    public String photo_50;
    public String photo_100;
    public String photo_200;
    public String status;
    public long last_seen;
    public boolean verified;
    public String deactivated;
    public int sex;

    public VKUser() {
        this.name = "...";
        this.surname = "";
    }

    public VKUser(JSONObject source) {
        friends_count = count;

        this.id = source.optInt("id");
        this.name = source.optString("first_name", "...");
        this.surname = source.optString("last_name", "");

        fullNname = name + " " + surname;

        this.deactivated = source.optString("deactivated");

        this.photo_50 = source.optString("photo_50");
        this.photo_100 = source.optString("photo_100");
        this.photo_200 = source.optString("photo_200");

        this.screen_name = source.optString("screen_name");
        this.online = source.optInt("online") == 1;
        this.status = source.optString("status");
        this.online_mobile = source.optInt("online_mobile") == 1;
        this.verified = source.optInt("verified") == 1;

        this.sex = source.optInt("sex");
        if (this.online_mobile) {
            this.online_app = source.optInt("online_app");
        }
        JSONObject lastSeen = source.optJSONObject("last_seen");
        if (lastSeen != null) {
            this.last_seen = lastSeen.optLong("time");
        }
    }

    public static ArrayList<VKUser> parse(JSONArray array) {
        ArrayList<VKUser> users = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            users.add(new VKUser((JSONObject) array.opt(i)));
        }

        return users;
    }

    @Override
    public String toString() {
        return name.concat(" ").concat(surname);
    }

    public boolean isDeactivated() {
        return !TextUtils.isEmpty(deactivated);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getScreenName() {
        return screen_name;
    }

    public void setScreenName(String screen_name) {
        this.screen_name = screen_name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnlineMobile() {
        return online_mobile;
    }

    public void setOnlineMobile(boolean online_mobile) {
        this.online_mobile = online_mobile;
    }

    public int getOnlineApp() {
        return online_app;
    }

    public void setOnlineApp(int online_app) {
        this.online_app = online_app;
    }

    public String getPhoto50() {
        return photo_50;
    }

    public void setPhoto50(String photo_50) {
        this.photo_50 = photo_50;
    }

    public String getPhoto100() {
        return photo_100;
    }

    public void setPhoto100(String photo_100) {
        this.photo_100 = photo_100;
    }

    public String getPhoto200() {
        return photo_200;
    }

    public void setPhoto200(String photo_200) {
        this.photo_200 = photo_200;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastSeen() {
        return last_seen;
    }

    public void setLastSeen(long last_seen) {
        this.last_seen = last_seen;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getDeactivated() {
        return deactivated;
    }

    public void setDeactivated(String deactivated) {
        this.deactivated = deactivated;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public static class Sex {
        public static final int NONE = 0;
        public static final int FEMALE = 1;
        public static final int MALE = 2;
    }

}
