package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKGroup extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIELDS_DEFAULT = "city,country,place,description,wiki_page,market,members_count,counters,start_date,finish_date,can_post,can_see_all_posts,activity,status,contacts,links,fixed_post,verified,site,ban_info,cover";

    public static final VKGroup EMPTY = new VKGroup() {
        @Override
        public String toString() {
            return "Group";
        }
    };

    private int id;
    private String name;
    private String screenName;
    private boolean closed;
    private boolean admin;
    private int adminLevel;
    private boolean member;
    private Type type;
    private boolean verified;
    private String photo50;
    private String photo100;
    private String photo200;
    private String description;
    private long membersCount;
    private String status;

    public VKGroup() {

    }

    public enum Type {
        GROUP, PAGE, EVENT
    }

    public VKGroup(JSONObject source) {
        this.id = source.optInt("id");

        this.name = source.optString("name");
        this.screenName = source.optString("screen_name");
        this.closed = source.optInt("is_closed") == 1;
        this.admin = source.optInt("is_admin") == 1;
        this.member = source.optInt("is_member") == 1;
        this.verified = source.optInt("verified") == 1;
        this.adminLevel = source.optInt("admin_level");

        this.type = VKGroup.getType(source.optString("type", "group"));

        this.photo50 = source.optString("photo_50");
        this.photo100 = source.optString("photo_100");
        this.photo200 = source.optString("photo_200");

        this.description = source.optString("description");
        this.status = source.optString("status");
        this.membersCount = source.optLong("members_count");
    }

    public static ArrayList<VKGroup> parse(JSONArray array) {
        ArrayList<VKGroup> groups = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            groups.add(new VKGroup((JSONObject) array.opt(i)));
        }

        return groups;
    }

    public static Type getType(String type) {
        if (TextUtils.isEmpty(type)) return null;

        switch (type) {
            case "group":
                return Type.GROUP;
            case "page":
                return Type.PAGE;
            case "event":
                return Type.EVENT;
        }

        return null;
    }

    public static String getType(Type type) {
        if (type == null) return null;
        switch (type) {
            case GROUP:
                return "group";
            case PAGE:
                return "page";
            case EVENT:
                return "event";
        }

        return null;
    }

    public static int toGroupId(int id) {
        return id < 0 ? Math.abs(id) : 1_000_000_000 - id;
    }

    public static boolean isGroupId(int id) {
        return id < 0;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getScreenName() {
        return screenName;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isAdmin() {
        return admin;
    }

    public int getAdminLevel() {
        return adminLevel;
    }

    public boolean isMember() {
        return member;
    }

    public Type getType() {
        return type;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getPhoto50() {
        return photo50;
    }

    public String getPhoto100() {
        return photo100;
    }

    public String getPhoto200() {
        return photo200;
    }

    public String getDescription() {
        return description;
    }

    public long getMembersCount() {
        return membersCount;
    }

    public String getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setAdminLevel(int adminLevel) {
        this.adminLevel = adminLevel;
    }

    public void setMember(boolean member) {
        this.member = member;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setPhoto50(String photo50) {
        this.photo50 = photo50;
    }

    public void setPhoto100(String photo100) {
        this.photo100 = photo100;
    }

    public void setPhoto200(String photo200) {
        this.photo200 = photo200;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMembersCount(long membersCount) {
        this.membersCount = membersCount;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
