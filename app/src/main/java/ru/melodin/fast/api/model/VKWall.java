package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class VKWall extends VKModel {

    public int id;
    public int ownerId;
    public int fromId;
    public int createdBy;
    public int date;
    public String text;
    public int replyOwnerId;
    public int replyPostId;
    public boolean friendsOnly;
    public ArrayList<VKModel> attachments;

    VKWall(JSONObject o) {
        id = o.optInt("id", -1);
        ownerId = o.optInt("owner_id", -1);
        fromId = o.optInt("from_id", -1);
        createdBy = o.optInt("created_by", -1);
        date = o.optInt("date", -1);
        text = o.optString("text", "");
        replyOwnerId = o.optInt("reply_owner_id", -1);
        replyPostId = o.optInt("reply_post_id", -1);
        friendsOnly = o.optInt("friends_only", -1) == 1;

        JSONArray attachs = o.optJSONArray("attachments");
        if (attachs != null && attachs.length() > 0)
            attachments = VKAttachments.parse(o.optJSONArray("attachments"));
    }

}
