package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKWall extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int ownerId;
    private int fromId;
    private int createdBy;
    private int date;
    private String text;
    private int replyOwnerId;
    private int replyPostId;
    private boolean friendsOnly;
    private ArrayList<VKModel> attachments;

    VKWall(@NonNull JSONObject o) {
        id = o.optInt("id", -1);
        ownerId = o.optInt("owner_id", -1);
        fromId = o.optInt("from_id", -1);
        createdBy = o.optInt("created_by", -1);
        date = o.optInt("date", -1);
        text = o.optString("text", "");
        replyOwnerId = o.optInt("reply_owner_id", -1);
        replyPostId = o.optInt("reply_post_id", -1);
        friendsOnly = o.optInt("friends_only", -1) == 1;

        JSONArray attachments = o.optJSONArray("attachments");
        if (attachments != null && attachments.length() > 0)
            this.attachments = VKAttachments.parse(o.optJSONArray("attachments"));
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getFromId() {
        return fromId;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public int getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public int getReplyOwnerId() {
        return replyOwnerId;
    }

    public int getReplyPostId() {
        return replyPostId;
    }

    public boolean isFriendsOnly() {
        return friendsOnly;
    }

    public ArrayList<VKModel> getAttachments() {
        return attachments;
    }
}
