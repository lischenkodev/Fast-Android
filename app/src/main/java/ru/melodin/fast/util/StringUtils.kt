package ru.melodin.fast.util

object StringUtils {

    fun unescape(input: String): String {
        return input
            .replace("&gt;", ">")
            .replace("&lt;", "<")
            .replace("&quot;", "\"")
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("amp;", "&")
    }

    fun isEmpty(input: String?): Boolean {
        return input == null || input.trim().isEmpty()
    }
}