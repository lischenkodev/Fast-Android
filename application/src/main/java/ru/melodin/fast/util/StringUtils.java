package ru.melodin.fast.util;

public class StringUtils {

    public static String unescape(String input) {
        return input
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&quot;", "\"")
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("amp;", "&");
    }

}