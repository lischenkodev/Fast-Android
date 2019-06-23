package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKVoice extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int duration;
    private ArrayList<Integer> waveform;

    private String linkOgg, linkMp3;

    public VKVoice(JSONObject o) {
        duration = o.optInt("duration");
        linkMp3 = o.optString("link_mp3");
        linkOgg = o.optString("link_ogg");
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

    public String getLinkOgg() {
        return linkOgg;
    }

    public String getLinkMp3() {
        return linkMp3;
    }
}
