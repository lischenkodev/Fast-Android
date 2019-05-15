package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;

public class VKVideo extends VKModel implements Serializable {

    public int id;
    public int owner_id;
    public int album_id;
    public String title;
    public String description;
    public int duration;
    public String link;
    public long date;
    public int views;
    public String player;
    public String photo_130;
    public String photo_320;
    public String photo_640;
    public String access_key;
    public int comments;
    public boolean can_comment;
    public boolean can_repost;
    public boolean user_likes;
    public boolean repeat;
    public int likes;
    public int privacy_view;
    public int privacy_comment;
    public String mp4_240;
    public String mp4_360;
    public String mp4_480;
    public String mp4_720;
    public String mp4_1080;
    public String external;

    public VKVideo() {
    }

    public VKVideo(int peerId, int attId) {
        this.owner_id = peerId;
        this.id = attId;
    }

    public VKVideo(JSONObject source) {
        tag = VKAttachments.TYPE_VIDEO;
        this.id = source.optInt("id");
        this.owner_id = source.optInt("owner_id");
        this.title = source.optString("title");
        this.description = source.optString("description");
        this.duration = source.optInt("duration");
        this.link = source.optString("link");
        this.date = source.optLong("date");
        this.views = source.optInt("views");
        this.comments = source.optInt("comments");
        this.player = source.optString("player");
        this.access_key = source.optString("access_key");
        this.album_id = source.optInt("album_id");

        this.photo_130 = source.optString("photo_130");
        this.photo_320 = source.optString("photo_320");
        this.photo_640 = source.optString("photo_640");

        JSONObject likes = source.optJSONObject("likes");
        if (likes != null) {
            this.likes = likes.optInt("count");
            this.user_likes = likes.optInt("user_likes") == 1;
        }
        this.can_comment = source.optInt("can_comment") == 1;
        this.can_repost = source.optInt("can_repost") == 1;
        this.repeat = source.optInt("repeat") == 1;

        JSONObject files = source.optJSONObject("files");
        if (files != null) {
            this.mp4_240 = files.optString("mp4_240");
            this.mp4_360 = files.optString("mp4_360");
            this.mp4_480 = files.optString("mp4_480");
            this.mp4_720 = files.optString("mp4_720");
            this.mp4_1080 = files.optString("mp4_1080");
            this.external = files.optString("external");
        }
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
}
