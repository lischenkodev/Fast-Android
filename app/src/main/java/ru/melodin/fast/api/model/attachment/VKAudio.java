package ru.melodin.fast.api.model.attachment;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;

import ru.melodin.fast.api.model.VKModel;


public class VKAudio extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long ownerId;
    private String artist;
    private String title;
    private int duration;
    private String url;
    private String accessKey;

    public VKAudio(@NonNull JSONObject source) {
        this.id = source.optLong("id");
        this.ownerId = source.optLong("owner_id");
        this.artist = source.optString("artist");
        this.title = source.optString("title");
        this.duration = source.optInt("duration");
        this.url = source.optString("url");
        this.accessKey = source.optString("access_key");
    }

    @NonNull
    private String toAttachmentString() {
        StringBuilder result = new StringBuilder("audio").append(ownerId).append('_').append(id);
        if (!TextUtils.isEmpty(accessKey)) {
            result.append('_');
            result.append(accessKey);
        }
        return result.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return toAttachmentString();
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public String getUrl() {
        return url;
    }

    public String getAccessKey() {
        return accessKey;
    }
}