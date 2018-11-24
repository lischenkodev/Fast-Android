package ru.stwtforever.fast.api.model;
import java.io.*;
import org.json.*;

public class VKLongPollServer extends VKModel implements Serializable {
    
    public String key;
    public String server;
    public long ts;

    public VKLongPollServer(JSONObject source) {
        this.key = source.optString("key");
        this.server = source.optString("server").replace("\\", "");
        this.ts = source.optLong("ts");
    }
}
