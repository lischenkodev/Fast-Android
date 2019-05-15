package ru.melodin.fast.util;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class JSONParser {

    public static JSONObject parse(String url, String charset) {
        BufferedReader reader;
        HttpURLConnection connection;
        StringBuilder builder;
        JSONObject result = null;
        try {
            connection = (HttpURLConnection) new java.net.URL(url).openConnection();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
            builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            connection.disconnect();
            reader.close();
            result = new JSONObject(builder.toString().trim());
            log(url, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static JSONObject parse(String url) {
        return parse(url, "utf-8");
    }

    static void log(String url, JSONObject result) {
        Log.d("JSONParser result", "Url = " + url + "\n" + result.toString());
    }
}

