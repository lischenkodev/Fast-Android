package ru.stwtforever.fast.api;

import java.util.ArrayList;

public class Scopes {
	
    private static final String NOTIFY = "notify";
    private static final String FRIENDS = "friends";
    private static final String PHOTOS = "photos";
    private static final String AUDIO = "audio";
    private static final String VIDEO = "video";
    private static final String DOCS = "docs";
    private static final String NOTES = "notes";
    private static final String PAGES = "pages";
    private static final String STATUS = "status";
    private static final String WALL = "wall";
    private static final String GROUPS = "conversation_groups";
    private static final String MESSAGES = "messages";
    private static final String NOTIFICATIONS = "notifications";
    private static final String STATS = "stats";
    private static final String ADS = "ads";
    private static final String OFFLINE = "offline";
    private static final String EMAIL = "email";
	
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
