package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKVideo extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int ownerId;
    private String title;
    private String description;
    private int duration;
    private long date;

    private String player;

    private String photo130;
    private String photo320;
    private String photo640;
    private String photo800;
    private String photo1280;

    private String accessKey;

    private int maxWidth;
    private String maxSize;

    private ArrayList<Size> sizes = new ArrayList<>();

    public VKVideo(JSONObject source) {
        this.id = source.optInt("id");
        this.ownerId = source.optInt("owner_id");
        this.title = source.optString("title");
        this.description = source.optString("description");
        this.duration = source.optInt("duration");
        this.date = source.optLong("date");
        this.player = source.optString("player");
        this.accessKey = source.optString("access_key");

        this.photo130 = source.optString("photo_130");
        this.photo320 = source.optString("photo_320");
        this.photo640 = source.optString("photo_640");
        this.photo800 = source.optString("photo_800");
        this.photo1280 = source.optString("photo_1280");

        sizes.add(new Size(130, photo130));
        sizes.add(new Size(320, photo320));
        sizes.add(new Size(640, photo640));
        sizes.add(new Size(800, photo800));
        sizes.add(new Size(1280, photo1280));

        maxSize = findMaxSize();
    }

    private String findMaxSize() {
        if (ArrayUtil.isEmpty(sizes)) return null;
        for (int i = sizes.size() - 1; i >= 0; i--) {
            Size size = sizes.get(i);
            String image = size.getUrl();
            if (!TextUtils.isEmpty(image)) {
                maxWidth = size.getWidth();
                return image;
            }
        }

        return null;
    }

    public CharSequence toAttachmentString() {
        StringBuilder result = new StringBuilder("video").append(ownerId).append('_').append(id);
        if (!TextUtils.isEmpty(accessKey)) {
            result.append('_');
            result.append(accessKey);
        }
        return result;
    }

    @Override
    public String toString() {
        return title;
    }


    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public long getDate() {
        return date;
    }

    public String getPlayer() {
        return player;
    }

    public String getPhoto130() {
        return photo130;
    }

    public String getPhoto320() {
        return photo320;
    }

    public String getPhoto640() {
        return photo640;
    }

    public String getPhoto800() {
        return photo800;
    }

    public String getPhoto1280() {
        return photo1280;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public class Size implements Serializable {
        private static final long serialVersionUID = 1L;

        private int width;
        private String url;

        public Size(int width, String url) {
            this.width = width;
            this.url = url;
        }

        public int getWidth() {
            return width;
        }

        public String getUrl() {
            return url;
        }
    }
}
