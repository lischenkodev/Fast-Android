package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKChat extends VKModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private int adminId;
    private ArrayList<VKUser> users = new ArrayList<>();
    private String photo50, photo100, photo200;

    private VKConversation.State state = VKConversation.State.IN;
    private VKConversation.Type type = VKConversation.Type.USER;

    public VKChat() {
    }

    public VKChat(JSONObject o) {
        this.id = o.optInt("id", -1);
        this.title = o.optString("title");
        this.adminId = o.optInt("admin_id", -1);

        JSONArray users = o.optJSONArray("users");
        if (!ArrayUtil.INSTANCE.isEmpty(users))
            this.users = VKUser.parse(users);

        this.photo50 = o.optString("photo_50");
        this.photo100 = o.optString("photo_100");
        this.photo200 = o.optString("photo_200");
        this.state = o.has("left") ? VKConversation.State.LEFT : o.has("kicked") ? VKConversation.State.KICKED : VKConversation.State.IN;
        this.type = VKConversation.getType(o.optString("type"));
    }

    public static int getIntState(VKConversation.State state) {
        if (state == null) return -1;
        switch (state) {
            case IN:
                return 0;
            case LEFT:
                return 1;
            case KICKED:
                return 2;
            default:
                return -1;
        }
    }

    public static VKConversation.State getState(int state) {
        switch (state) {
            default:
            case 0:
                return VKConversation.State.IN;
            case 1:
                return VKConversation.State.LEFT;
            case 2:
                return VKConversation.State.KICKED;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public ArrayList<VKUser> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<VKUser> users) {
        this.users = users;
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

    public VKConversation.State getState() {
        return state;
    }

    public void setState(VKConversation.State state) {
        this.state = state;
    }

    public VKConversation.Type getType() {
        return type;
    }

    public void setType(VKConversation.Type type) {
        this.type = type;
    }
}
