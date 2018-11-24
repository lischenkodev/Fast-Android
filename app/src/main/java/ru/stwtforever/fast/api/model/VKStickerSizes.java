package ru.stwtforever.fast.api.model;

import java.io.*;
import java.util.*;
import org.json.*;

public class VKStickerSizes extends VKModel implements Serializable {

    private ArrayList<StickerSizes> sizes;

    public VKStickerSizes(JSONArray array) {
        sizes = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            sizes.add(new StickerSizes (array.optJSONObject(i)));
        }
    }

    public StickerSizes forType(String type) {
        for (StickerSizes size : sizes) {
            if (size.type.equals(type)) {
                return size;
            }
        }

        return null;
    }

    public static class StickerSizes extends VKModel implements Serializable {
        public String src;
        public int width;
        public int height;
        public String type;

        public StickerSizes(JSONObject source) {
            this.src = source.optString("src");
            this.width = source.optInt("width");
            this.height = source.optInt("height");
            this.type = source.optString("type");
        }
    }
}
