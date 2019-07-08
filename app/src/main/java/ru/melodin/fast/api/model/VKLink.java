package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;

import ru.melodin.fast.api.model.attachment.VKPhoto;

public class VKLink extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private String title;
    private String caption;
    private String description;
    private String previewUrl;

    private VKPhoto photo;

    public VKLink(@NonNull JSONObject source) {
        this.url = source.optString("url");
        this.title = source.optString("title");
        this.caption = source.optString("caption");
        this.previewUrl = source.optString("preview_url");
        this.description = source.optString("description");

        JSONObject linkPhoto = source.optJSONObject("photo");
        if (linkPhoto != null) {
            this.photo = new VKPhoto(linkPhoto);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public VKPhoto getPhoto() {
        return photo;
    }
}

