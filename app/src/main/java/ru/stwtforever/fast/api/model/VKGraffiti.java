package ru.stwtforever.fast.api.model;

import org.json.JSONObject;

import java.io.Serializable;

public class VKGraffiti extends VKModel implements Serializable {
	
    public String src; 
    public int width, height;

    VKGraffiti(JSONObject o) {
		tag = VKAttachments.TYPE_DOC;
        this.src = o.optString("src");
		this.width = o.optInt("width");
		this.height = o.optInt("height");
    }
}
