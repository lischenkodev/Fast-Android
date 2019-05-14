package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;

public class VKPhoto extends VKModel implements Serializable {

    public int id;
    public int album_id;
    public int owner_id;
    public int width;
    public int height;
    public String text;
    public long date;
    public boolean user_likes;
    public boolean can_comment;
    public int likes;
    public int comments;
    public int tags;
    public String access_key;

    public String photo_75;
    public String photo_130;
    public String photo_604;
    public String photo_807;
    public String photo_1280;
    public String photo_2560;

    public VKPhoto() {
    }

    public VKPhoto(int peerId, int attId) {
        this.owner_id = peerId;
        this.id = attId;
    }

    public final static String TAG = "FVKPhoto";

    public VKPhotoSizes sizes;

    public VKPhoto(JSONObject source) {
        tag = VKAttachments.TYPE_PHOTO;
        this.id = source.optInt("id");
        this.owner_id = source.optInt("owner_id");
        this.album_id = source.optInt("album_id");
        this.date = source.optLong("date");
        this.width = source.optInt("width");
        this.height = source.optInt("height");
        this.text = source.optString("text");
        this.access_key = source.optString("access_key");
        this.can_comment = source.optInt("can_comment") == 1;
        this.sizes = new VKPhotoSizes(source.optJSONArray("sizes"));

        JSONObject likes = source.optJSONObject("likes");
        if (likes != null) {
            this.likes = likes.optInt("count");
            this.user_likes = likes.optInt("user_likes") == 1;
        }
        JSONObject comments = source.optJSONObject("comments");
        if (comments != null) {
            this.comments = comments.optInt("count");
        }
    }

    public static VKPhoto parseFromAttach(JSONObject o) {
        VKPhoto p = new VKPhoto();
        p.height = o.optInt("heigh");
        p.width = o.optInt("width");
        p.id = o.optInt("id");
        p.album_id = o.optInt("album_id");
        p.owner_id = o.optInt("owner_id");
        p.sizes = new VKPhotoSizes(o.optJSONArray("sizes"));
        p.text = o.optString("text");
        p.date = o.optLong("date");
        p.access_key = o.optString("access_key");

        return p;
    }

    public String getMaxSize() {
        String p_75 = sizes.forType("s").src;
        String p_130 = sizes.forType("m").src;
        String p_604 = sizes.forType("x").src;
        String p_807 = sizes.forType("y").src;
        String p_1080_1024 = sizes.forType("z").src;
        String p_2560_2048 = sizes.forType("w").src;

        return
                TextUtils.isEmpty(p_2560_2048) ?
                        TextUtils.isEmpty(p_1080_1024) ?
                                TextUtils.isEmpty(p_807) ?
                                        TextUtils.isEmpty(p_604) ?
                                                TextUtils.isEmpty(p_130) ?
                                                        TextUtils.isEmpty(p_75) ? ""
                                                                : p_75
                                                        : p_130
                                                : p_604
                                        : p_807
                                : p_1080_1024 :
                        p_2560_2048;
    }
}
