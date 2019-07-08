package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKSticker extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int productId;
    private ArrayList<Size> images = new ArrayList<>();
    private ArrayList<Size> backgroundImages = new ArrayList<>();
    private int maxWidth;
    private int maxHeight;
    private String maxSize;
    private String maxBackgroundSize;

    public VKSticker(JSONObject source) {
        this.id = source.optInt("sticker_id");
        this.productId = source.optInt("product_id");

        JSONArray images = source.optJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject size = images.optJSONObject(i);
            this.images.add(new Size(size));
        }

        JSONArray backgroundImages = source.optJSONArray("images_with_background");
        for (int i = 0; i < backgroundImages.length(); i++) {
            JSONObject size = backgroundImages.optJSONObject(i);
            this.backgroundImages.add(new Size(size));
        }

        maxSize = findMaxSize();
        maxBackgroundSize = findMaxBackgroundSize();
    }

    public String size(int width) {
        if (ArrayUtil.INSTANCE.isEmpty(images)) return null;
        for (Size size : images) {
            if (size.getWidth() == width)
                return size.getUrl();
        }

        return null;
    }

    public String backgroundSize(int width) {
        if (ArrayUtil.INSTANCE.isEmpty(backgroundImages)) return null;
        for (Size size : backgroundImages) {
            if (size.getWidth() == width)
                return size.getUrl();
        }

        return null;

    }

    private String findMaxSize() {
        if (ArrayUtil.INSTANCE.isEmpty(images)) return null;
        for (int i = images.size() - 1; i >= 0; i--) {
            Size size = images.get(i);
            String image = size.getUrl();
            if (!TextUtils.isEmpty(image)) {
                maxWidth = size.getWidth();
                maxHeight = size.getHeight();
                return image;
            }
        }

        return null;
    }

    private String findMaxBackgroundSize() {
        if (ArrayUtil.INSTANCE.isEmpty(backgroundImages)) return null;
        for (int i = backgroundImages.size() - 1; i >= 0; i--) {
            String image = backgroundImages.get(i).getUrl();
            if (!TextUtils.isEmpty(image)) {
                return image;
            }
        }

        return null;
    }

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public ArrayList<Size> getImages() {
        return images;
    }

    public String getMaxBackgroundSize() {
        return maxBackgroundSize;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public class Size implements Serializable {
        private static final long serialVersionUID = 1L;

        private int width;
        private int height;
        private String url;

        public Size(JSONObject o) {
            this.width = o.optInt("width");
            this.height = o.optInt("height");
            this.url = o.optString("url");
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getUrl() {
            return url;
        }
    }
}
