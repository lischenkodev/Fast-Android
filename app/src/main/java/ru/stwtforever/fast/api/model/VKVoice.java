package ru.stwtforever.fast.api.model;
import java.io.*;
import java.util.*;
import org.json.*;

public class VKVoice extends VKModel implements Serializable {
	
	public int duration; //seconds
	public ArrayList<Integer> waveform;
	
	public String link_ogg, link_mp3;
	
	public VKVoice(JSONObject o) {
		duration = o.optInt("duration");
		link_mp3 = o.optString("link_mp3");
		link_ogg = o.optString("link_ogg");
		waveform = new ArrayList<>();
		
		JSONArray j_waveform = o.optJSONArray("waveform");
		for (int i = 0; i < j_waveform.length(); i++) {
			waveform.add((Integer) j_waveform.opt(i));
		}
	}
}
