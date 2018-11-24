package ru.stwtforever.fast.api.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

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
	
	public VKDoc() {}
	
	public VKDoc(int peerId, int attId) {
		this.owner_id = peerId;
		this.id = attId;
	}

    public VKDoc(JSONObject source) {
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
        if (preview != null && preview.has("photo")) {
            JSONArray sizes = preview.optJSONObject("photo")
				.optJSONArray("sizes");

            photo_sizes = new VKPhotoSizes(sizes);
        }
		
		if (preview != null && preview.has("audio_message")) {
			voice = new VKVoice(preview.optJSONObject("audio_message"));
		}
		
		if (preview != null && preview.has("graffiti")) {
			graffiti = new VKGraffiti(preview.optJSONObject("graffiti"));
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

    @Override
    public String toString() {
        return title;
	}
}
