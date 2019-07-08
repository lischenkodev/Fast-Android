package ru.melodin.fast.api.model.attachment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.api.model.VKModel;

public class VKPhotoSizes extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

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

        return null;
    }

    public ArrayList<PhotoSize> getSizes() {
        return sizes;
    }

    public static class PhotoSize extends VKModel implements Serializable {

        private String src;
        private int width;
        private int height;
        private String type;

        PhotoSize(JSONObject o) {
            this(o, false);
        }

        PhotoSize(JSONObject source, boolean doc) {
            this.src = doc ? source.optString("url") : source.optString("url");
            this.width = source.optInt("width");
            this.height = source.optInt("height");
            this.type = source.optString("type");
        }

        public String getSrc() {
            return src;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getType() {
            return type;
        }
    }
}