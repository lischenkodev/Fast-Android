package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKPhoto extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int albumId;
    private int ownerId;
    private int width;
    private int height;
    private String text;
    private long date;
    private String accessKey;
    private ArrayList<VKPhotoSizes.PhotoSize> sizes;

    public VKPhoto() {
    }

    public VKPhoto(JSONObject source) {
        this.id = source.optInt("id");
        this.ownerId = source.optInt("owner_id");
        this.albumId = source.optInt("album_id");
        this.date = source.optLong("date");
        this.width = source.optInt("width");
        this.height = source.optInt("height");
        this.text = source.optString("text");
        this.accessKey = source.optString("access_key");
        this.sizes = new VKPhotoSizes(source.optJSONArray("sizes")).getSizes();
    }

    public String getMaxSize() {
        if (ArrayUtil.isEmpty(sizes)) return null;
        for (int i = sizes.size() - 1; i >= 0; i--) {
            VKPhotoSizes.PhotoSize image = sizes.get(i);
            String src = image.getSrc();
            if (!TextUtils.isEmpty(src)) {
                return src;
            }
        }

        return null;
    }

    public int getId() {
        return id;
    }

    public int getAlbumId() {
        return albumId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getText() {
        return text;
    }

    public long getDate() {
        return date;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public ArrayList<VKPhotoSizes.PhotoSize> getSizes() {
        return sizes;
    }
}
