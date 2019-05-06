package ru.stwtforever.fast.api;

import java.util.ArrayList;

public class Scopes {
	
    public static final String NOTIFY = "notify";
    public static final String FRIENDS = "friends";
    public static final String PHOTOS = "photos";
    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String DOCS = "docs";
    public static final String NOTES = "notes";
    public static final String PAGES = "pages";
    public static final String STATUS = "status";
    public static final String WALL = "wall";
    public static final String GROUPS = "conversation_groups";
    public static final String MESSAGES = "messages";
    public static final String NOTIFICATIONS = "notifications";
    public static final String STATS = "stats";
    public static final String ADS = "ads";
    public static final String OFFLINE = "offline";
    public static final String EMAIL = "email";
    public static final String DIRECT = "direct";
	
    public static ArrayList<String> parse(int permissions) {
        ArrayList<String> res = new ArrayList<>(16);
        if ((permissions & 1) > 0) res.add(NOTIFY);
        if ((permissions & 2) > 0) res.add(FRIENDS);
        if ((permissions & 4) > 0) res.add(PHOTOS);
        if ((permissions & 8) > 0) res.add(AUDIO);
        if ((permissions & 16) > 0) res.add(VIDEO);
        if ((permissions & 128) > 0) res.add(PAGES);
        if ((permissions & 1024) > 0) res.add(STATUS);
        if ((permissions & 2048) > 0) res.add(NOTES);
        if ((permissions & 4096) > 0) res.add(MESSAGES);
        if ((permissions & 8192) > 0) res.add(WALL);
        if ((permissions & 32768) > 0) res.add(ADS);
        if ((permissions & 65536) > 0) res.add(OFFLINE);
        if ((permissions & 131072) > 0) res.add(DOCS);
        if ((permissions & 262144) > 0) res.add(GROUPS);
        if ((permissions & 524288) > 0) res.add(NOTIFICATIONS);
        if ((permissions & 1048576) > 0) res.add(STATS);
        if ((permissions & 4194304) > 0) res.add(EMAIL);
        return res;
    }
	
    public static String all() {
        return NOTIFY + ',' + FRIENDS
                + ',' + PHOTOS + ',' + AUDIO
                + ',' + VIDEO + ',' + PAGES
                + ',' + STATUS + ',' + NOTES
                + ',' + WALL
                + ',' + ADS + ',' + OFFLINE
                + ',' + DOCS + ',' + GROUPS
                + ',' + NOTIFICATIONS + ','
                //',' + MESSAGES +
                + STATS;
    }
}
