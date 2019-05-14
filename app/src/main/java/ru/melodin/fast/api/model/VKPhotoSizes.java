package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKPhotoSizes extends VKModel implements Serializable {

    private ArrayList<PhotoSize> sizes;

    VKPhotoSizes(JSONArray array) {
        sizes = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            sizes.add(new PhotoSize(array.optJSONObject(i)));
        }
    }

    VKPhotoSizes(JSONArray array, boolean doc) {
        sizes = new ArrayList<>(array.length());

        for (int i = 0; i < array.length(); i++) {
            sizes.add(new PhotoSize(array.optJSONObject(i), doc));
        }
    }

    public PhotoSize forType(String type) {
        for (PhotoSize size : sizes) {
            if (size.type.equals(type)) {

                return size;
            }
        }

        return PhotoSize.EMPTY;
    }

    public static class PhotoSize extends VKModel implements Serializable {
        public String src;
        public int width;
        public int height;
        public String type;

        PhotoSize() {
        }

        PhotoSize(JSONObject o) {
            this(o, false);
        }

        PhotoSize(JSONObject source, boolean doc) {
            this.src = doc ? source.optString("src") : source.optString("url");
            this.width = source.optInt("width");
            this.height = source.optInt("height");
            this.type = source.optString("type");
        }

        static PhotoSize EMPTY = new PhotoSize() {
            public String src = "";
            public int width = 0;
            public int height = 0;
            public String type = "none";
        };
    }
}
