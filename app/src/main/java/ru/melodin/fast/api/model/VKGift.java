package ru.melodin.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

public class VKGift extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int fromId;
    private long id;
    private String message;
    private long date;
    private String thumb48;
    private String thumb96;
    private String thumb256;

    public VKGift(JSONObject source) {
        this.id = source.optLong("id");
        this.fromId = source.optInt("from_id");
        this.date = source.optLong("date");

        if (source.has("gift")) {
            source = source.optJSONObject("gift");
        }

        this.thumb48 = source.optString("thumb_48");
        this.thumb96 = source.optString("thumb_96");
        this.thumb256 = source.optString("thumb_256");
    }

    public int getFromId() {
        return fromId;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public long getDate() {
        return date;
    }

    public String getThumb48() {
        return thumb48;
    }

    public String getThumb96() {
        return thumb96;
    }

    public String getThumb256() {
        return thumb256;
    }
}
