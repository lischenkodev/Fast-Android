package ru.melodin.fast.api.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Serializable;

public class VKGraffiti extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    public String url;
    public int width, height;

    VKGraffiti(@NonNull JSONObject o) {
        this.url = o.optString("url");
        this.width = o.optInt("width");
        this.height = o.optInt("height");
    }
}
