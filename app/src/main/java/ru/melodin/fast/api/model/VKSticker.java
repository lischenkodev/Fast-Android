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
    private ArrayList<String> images = new ArrayList<>();
    private ArrayList<String> backgroundImages = new ArrayList<>();

    public VKSticker(JSONObject source) {
        this.id = source.optInt("sticker_id");
        this.productId = source.optInt("product_id");

        JSONArray images = source.optJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject size = images.optJSONObject(i);
            String url = size.optString("url");
            this.images.add(url);
        }

        JSONArray backgroundImages = source.optJSONArray("images_with_background");
        for (int i = 0; i < backgroundImages.length(); i++) {
            JSONObject size = backgroundImages.optJSONObject(i);
            String url = size.optString("url");
            this.backgroundImages.add(url);
        }
    }

    public String getMaxSize() {
        if (ArrayUtil.isEmpty(images)) return null;
        for (int i = images.size() - 1; i >= 0; i--) {
            String image = images.get(i);
            if (!TextUtils.isEmpty(image)) {
                return image;
            }
        }

        return null;
    }

    public String getMaxBackgroundSize() {
        if (ArrayUtil.isEmpty(backgroundImages)) return null;
        for (int i = backgroundImages.size() - 1; i >= 0; i--) {
            String image = backgroundImages.get(i);
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

    public ArrayList<String> getImages() {
        return images;
    }
}
