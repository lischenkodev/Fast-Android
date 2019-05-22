package ru.melodin.fast.api.model;

import android.util.Log;

import org.json.JSONObject;

import java.io.Serializable;

public class VKGraffiti extends VKModel implements Serializable {

    public String url;
    public int width, height;

    VKGraffiti(JSONObject o) {
        tag = VKAttachments.TYPE_DOC;
        this.url = o.optString("url");
        this.width = o.optInt("width");
        this.height = o.optInt("height");

        Log.d("FVKGraffiti", o.toString());
    }
}
