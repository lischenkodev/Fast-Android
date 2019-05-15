package ru.melodin.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

public class VKGift extends VKModel implements Serializable {

    public int from_id;
    public long id;
    public String message;
    public long date;
    public String thumb_48;
    public String thumb_96;
    public String thumb_256;

    public VKGift() {
    }

    public VKGift(int peerId, int attId) {
        this.from_id = peerId;
        this.id = attId;
    }

    public VKGift(JSONObject source) {
        tag = VKAttachments.TYPE_GIFT;
        this.id = source.optLong("id");
        this.from_id = source.optInt("from_id");
        this.date = source.optLong("date");

        if (source.has("gift")) {
            source = source.optJSONObject("gift");
        }

        this.thumb_48 = source.optString("thumb_48");
        this.thumb_96 = source.optString("thumb_96");
        this.thumb_256 = source.optString("thumb_256");
    }

}
