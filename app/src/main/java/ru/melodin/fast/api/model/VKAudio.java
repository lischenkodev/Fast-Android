package ru.melodin.fast.api.model;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.Serializable;


public class VKAudio extends VKModel implements Serializable {

    public long id;
    public long owner_id;
    public String artist;
    public String title;
    public int duration;
    public String url;
    public long lyrics_id;
    public long album_id;
    public long genre_id;
    public String access_key;

    public VKAudio() {
    }

    public VKAudio(int peerId, int attId) {
        this.owner_id = peerId;
        this.id = attId;
    }

    public VKAudio(JSONObject source) {
        tag = VKAttachments.TYPE_AUDIO;

        this.id = source.optLong("id");
        this.owner_id = source.optLong("owner_id");
        this.artist = source.optString("artist");
        this.title = source.optString("title");
        this.duration = source.optInt("duration");
        this.url = source.optString("url");
        this.album_id = source.optInt("album_id");
        this.lyrics_id = source.optLong("lyrics_id");
        this.access_key = source.optString("access_key", null);
        this.genre_id = source.optLong("genre_id", -1);
    }

    public CharSequence toAttachmentString() {
        StringBuilder result = new StringBuilder("audio").append(owner_id).append('_').append(id);
        if (!TextUtils.isEmpty(access_key)) {
            result.append('_');
            result.append(access_key);
        }
        return result;
    }

    @Override
    public String toString() {
        return artist + " - " + title;
    }

    public final static class Genre {
        public final static int ROCK = 1;
        public final static int POP = 2;
        public final static int RAP_AND_HIPHOP = 3;
        public final static int EASY_LISTENING = 4;
        public final static int DANCE_AND_HOUSE = 5;
        public final static int INSTRUMENTAL = 6;
        public final static int METAL = 7;
        public final static int DUBSTEP = 8;
        public final static int JAZZ_AND_BLUES = 9;
        public final static int DRUM_AND_BASS = 10;
        public final static int TRANCE = 11;
        public final static int CHANSON = 12;
        public final static int ETHNIC = 13;
        public final static int ACOUSTIC_AND_VOCAL = 14;
        public final static int REGGAE = 15;
        public final static int CLASSICAL = 16;
        public final static int INDIE_POP = 17;
        public final static int OTHER = 18;
        public final static int SPEECH = 19;
        public final static int ALTERNATIVE = 21;
        public final static int ELECTROPOP_AND_DISCO = 22;

        private Genre() {
        }
    }
}
