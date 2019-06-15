package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKVoice extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int duration;
    private ArrayList<Integer> waveform;

    private String link_ogg, link_mp3;

    public VKVoice(JSONObject o) {
        duration = o.optInt("duration");
        link_mp3 = o.optString("link_mp3");
        link_ogg = o.optString("link_ogg");
        waveform = new ArrayList<>();

        JSONArray waveform = o.optJSONArray("waveform");
        for (int i = 0; i < waveform.length(); i++) {
            this.waveform.add((Integer) waveform.opt(i));
        }
    }

    public int getDuration() {
        return duration;
    }

    public ArrayList<Integer> getWaveform() {
        return waveform;
    }

    public String getLink_ogg() {
        return link_ogg;
    }

    public String getLink_mp3() {
        return link_mp3;
    }
}
