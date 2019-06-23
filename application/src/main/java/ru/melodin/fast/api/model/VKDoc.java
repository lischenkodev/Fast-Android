package ru.melodin.fast.api.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKDoc extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_ARCHIVE = 2;
    public static final int TYPE_GIF = 3;
    public static final int TYPE_IMAGE = 4;
    public static final int TYPE_AUDIO = 5;
    public static final int TYPE_VIDEO = 6;
    public static final int TYPE_BOOK = 7;
    public static final int TYPE_UNKNOWN = 8;

    private int id;
    private int ownerId;
    private String title;
    private int size;
    private String ext;
    private String url;
    private String accessKey;
    private int type;
    private ArrayList<VKPhotoSizes.PhotoSize> sizes;

    VKDoc(JSONObject source) {
        this.id = source.optInt("id");
        this.ownerId = source.optInt("owner_id");
        this.title = source.optString("title");
        this.url = source.optString("url");
        this.size = source.optInt("size");
        this.type = source.optInt("type");
        this.ext = source.optString("ext");
        this.accessKey = source.optString("access_key");
        this.type = source.optInt("type");

        JSONObject preview = source.optJSONObject("preview");

        if (preview != null) {
            JSONObject photo = preview.optJSONObject("photo");
            if (photo != null) {
                sizes = new VKPhotoSizes(photo.optJSONArray("sizes")).getSizes();
            }
        }
    }

    public String getSrc() {
        return ArrayUtil.isEmpty(sizes) ? null : sizes.get(0).getSrc();
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

    public String toAttachmentString() {
        StringBuilder result = new StringBuilder("doc").append(ownerId).append('_').append(id);
        if (!TextUtils.isEmpty(accessKey)) {
            result.append('_');
            result.append(accessKey);
        }
        return result.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public String getExt() {
        return ext;
    }

    public String getUrl() {
        return url;
    }


    public int getType() {
        return type;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public ArrayList<VKPhotoSizes.PhotoSize> getSizes() {
        return sizes;
    }
}
