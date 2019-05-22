package ru.melodin.fast.api.model;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;

public class VKDoc extends VKModel implements Serializable {

    public static final int TYPE_NONE = 0;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_ARCHIVE = 2;
    public static final int TYPE_GIF = 3;
    public static final int TYPE_IMAGE = 4;
    public static final int TYPE_AUDIO = 5;
    public static final int TYPE_VIDEO = 6;
    public static final int TYPE_BOOK = 7;
    public static final int TYPE_UNKNOWN = 8;

    public int id;
    public int owner_id;
    public String title;
    public int size;
    public String ext;
    public String url;
    public String access_key;
    public int type;
    public VKPhotoSizes photo_sizes;

    public VKVoice voice;
    public VKGraffiti graffiti;

    public VKDoc() {
    }

    VKDoc(int peerId, int attId) {
        this.owner_id = peerId;
        this.id = attId;
    }

    VKDoc(JSONObject source) {
        tag = VKAttachments.TYPE_DOC;

        this.id = source.optInt("id");
        this.owner_id = source.optInt("owner_id");
        this.title = source.optString("title");
        this.url = source.optString("url");
        this.size = source.optInt("size");
        this.type = source.optInt("type");
        this.ext = source.optString("ext");
        this.access_key = source.optString("access_key");
        this.type = source.optInt("type");

        JSONObject preview = source.optJSONObject("preview");

        if (preview != null) {
            if (preview.has("photo")) {
                photo_sizes = new VKPhotoSizes(preview.optJSONObject("photo").optJSONArray("sizes"));
            }
        }
    }

    public String toAttachmentString() {
        StringBuilder result = new StringBuilder("doc").append(owner_id).append('_').append(id);
        if (!TextUtils.isEmpty(access_key)) {
            result.append('_');
            result.append(access_key);
        }
        return result.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }
}
