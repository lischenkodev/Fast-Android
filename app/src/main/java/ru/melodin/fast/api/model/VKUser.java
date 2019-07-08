package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKUser extends VKModel implements Serializable {

    public static final String FIELDS_DEFAULT = "photo_50,photo_100,photo_200,status,screen_name,online,online_mobile,last_seen,verified,sex";

    public static final VKUser EMPTY = new VKUser() {
        @NonNull
        @Override
        public String toString() {
            return "...";
        }
    };

    private static final long serialVersionUID = 1L;

    public static int count;

    private int id;
    private int invitedBy;
    private String name;
    private String surname;
    private String fullName;
    private String screenName;
    private boolean online;
    private boolean onlineMobile;
    private int onlineApp;
    private String photo50;
    private String photo100;
    private String photo200;
    private String status;
    private long lastSeen;
    private boolean verified;
    private boolean deactivated;
    private int sex;

    public VKUser() {
        this.name = "...";
        this.surname = "";
    }

    public VKUser(@NonNull JSONObject source) {

        this.id = source.optInt("id");
        this.invitedBy = source.optInt("invited_by", -1);
        this.name = source.optString("first_name", "...");
        this.surname = source.optString("last_name", "");

        fullName = name + " " + surname;

        String deactivated = source.optString("deactivated");
        this.deactivated = deactivated.equals("deleted") || deactivated.equals("banned");

        this.photo50 = source.optString("photo_50");
        this.photo100 = source.optString("photo_100");
        this.photo200 = source.optString("photo_200");

        this.screenName = source.optString("screen_name");
        this.online = source.optInt("online") == 1;
        this.status = source.optString("status");
        this.onlineMobile = source.optInt("online_mobile") == 1;
        this.verified = source.optInt("verified") == 1;

        this.sex = source.optInt("sex");
        if (this.onlineMobile) {
            this.onlineApp = source.optInt("online_app");
        }

        JSONObject lastSeen = source.optJSONObject("last_seen");
        if (lastSeen != null) {
            this.lastSeen = lastSeen.optLong("time");
        }
    }

    public static ArrayList<VKUser> parse(@NonNull JSONArray array) {
        ArrayList<VKUser> users = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            users.add(new VKUser((JSONObject) array.opt(i)));
        }

        return users;
    }

    @NonNull
    @Override
    public String toString() {
        return name + " " + surname;
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

    public String getFullName() {
        return fullName;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnlineMobile() {
        return onlineMobile;
    }

    public void setOnlineMobile(boolean onlineMobile) {
        this.onlineMobile = onlineMobile;
    }

    public int getOnlineApp() {
        return onlineApp;
    }

    public void setOnlineApp(int onlineApp) {
        this.onlineApp = onlineApp;
    }

    public String getPhoto50() {
        return photo50;
    }

    public void setPhoto50(String photo50) {
        this.photo50 = photo50;
    }

    public String getPhoto100() {
        return photo100;
    }

    public void setPhoto100(String photo100) {
        this.photo100 = photo100;
    }

    public String getPhoto200() {
        return photo200;
    }

    public void setPhoto200(String photo200) {
        this.photo200 = photo200;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public int getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(int invitedBy) {
        this.invitedBy = invitedBy;
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
