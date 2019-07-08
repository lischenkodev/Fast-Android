package ru.melodin.fast.api

import android.text.TextUtils
import ru.melodin.fast.io.Charsets
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.regex.Pattern

class Auth {

    companion object {
        const val REDIRECT_URL = "https://oauth.vk.com/blank.html"

        @Throws(Exception::class)
        fun parseRedirectUrl(url: String): Array<String> {
            val accessToken = extractPattern(url, "access_token=(.*?)&")
            val userId = extractPattern(url, "id=(\\d*)")
            if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(accessToken)) {
                throw Exception("Failed to parse redirect url $url")
            }
            return arrayOf(accessToken!!, userId!!)
        }

        @Throws(UnsupportedEncodingException::class)
        fun getUrl(apiId: Int, settings: String): String {
            return ("https://oauth.vk.com/authorize?client_id="
                    + apiId + "&display=mobile&scope="
                    + settings + "&redirect_uri="
                    + URLEncoder.encode(REDIRECT_URL, Charsets.UTF_8.toString())
                    + "&response_type=token"
                    + "&v=" + URLEncoder.encode(VKApi.API_VERSION, Charsets.UTF_8.toString()))
        }


        private fun extractPattern(string: String, pattern: String): String? {
            val p = Pattern.compile(pattern)
            val m = p.matcher(string)
            return if (m.find()) {
                m.group(1)
            } else null
        }
    }

}
