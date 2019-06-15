package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;

public class VKVideo extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int owner_id;
    private String title;
    private String description;
    private int duration;
    private long date;

    private String player;

    private String photo130;
    private String photo320;
    private String photo640;
    private String photo800;
    private String photo1280;

    private String access_key;

    public VKVideo() {
    }

    public VKVideo(JSONObject source) {
        this.id = source.optInt("id");
        this.owner_id = source.optInt("owner_id");
        this.title = source.optString("title");
        this.description = source.optString("description");
        this.duration = source.optInt("duration");
        this.date = source.optLong("date");
        this.player = source.optString("player");
        this.access_key = source.optString("access_key");

        this.photo130 = source.optString("photo_130");
        this.photo320 = source.optString("photo_320");
        this.photo640 = source.optString("photo_640");
        this.photo800 = source.optString("photo_800");
        this.photo1280 = source.optString("photo_1280");
    }

    public String getMaxSize() {
        return
                TextUtils.isEmpty(photo1280) ?
                        TextUtils.isEmpty(photo800) ?
                                TextUtils.isEmpty(photo640) ?
                                        TextUtils.isEmpty(photo320) ? photo130 :
                                                photo320 :
                                        photo640 :
                                photo800 :
                        photo1280;
    }

    public CharSequence toAttachmentString() {
        StringBuilder result = new StringBuilder("video").append(owner_id).append('_').append(id);
        if (!TextUtils.isEmpty(access_key)) {
            result.append('_');
            result.append(access_key);
        }
        return result;
    }

    @Override
    public String toString() {
        return title;
    }


    public int getId() {
        return id;
    }

    public int getOwner_id() {
        return owner_id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public long getDate() {
        return date;
    }

    public String getPlayer() {
        return player;
    }

    public String getPhoto130() {
        return photo130;
    }

    public String getPhoto320() {
        return photo320;
    }

    public String getPhoto640() {
        return photo640;
    }

    public String getPhoto800() {
        return photo800;
    }

    public String getPhoto1280() {
        return photo1280;
    }

    public String getAccess_key() {
        return access_key;
    }
}
