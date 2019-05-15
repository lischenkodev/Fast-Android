package ru.melodin.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

public class VKLink extends VKModel implements Serializable {

    public String url;
    public String title;
    public String caption;
    public String description;
    public String preview_page;
    public String preview_url;

    public VKPhoto photo;

    public VKLink() {
    }

    public VKLink(JSONObject source) {
        tag = VKAttachments.TYPE_LINK;
        this.url = source.optString("url");
        this.title = source.optString("title");
        this.caption = source.optString("caption");
        this.preview_url = source.optString("preview_url");

        JSONObject linkPhoto = source.optJSONObject("photo");
        if (linkPhoto != null) {
            this.photo = new VKPhoto(linkPhoto);
        }
    }
}

