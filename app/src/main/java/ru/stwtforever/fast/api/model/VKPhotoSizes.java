package ru.stwtforever.fast.api.model;

import java.io.*;
import java.util.*;
import org.json.*;

public class VKPhotoSizes extends VKModel implements Serializable {
	
    private ArrayList<PhotoSize> sizes;
	
    public VKPhotoSizes(JSONArray array) {
        sizes = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            sizes.add(new PhotoSize(array.optJSONObject(i)));
        }
    }

    public PhotoSize forType(String type) {
        for (PhotoSize size : sizes) {
            if (size.type.equals(type)) {
                return size;
            }
        }

        return null;
    }

    public static class PhotoSize extends VKModel implements Serializable {
        public String src;
        public int width;
        public int height;
        public String type;

        public PhotoSize(JSONObject source) {
            this.src = source.optString("url");
            this.width = source.optInt("width");
            this.height = source.optInt("height");
            this.type = source.optString("type");
        }
    }
}
