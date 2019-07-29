package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import java.io.Serializable


class VKAudio(source: JSONObject) : VKModel(), Serializable {

    val id = source.optLong("id")
    val ownerId = source.optLong("owner_id")
    val artist = source.optString("artist")
    val title = source.optString("title")
    val duration = source.optInt("duration")
    val url = source.optString("url")
    val accessKey = source.optString("access_key")

    private fun toAttachmentString(): String {
        val result = StringBuilder("audio").append(ownerId).append('_').append(id)
        if (!TextUtils.isEmpty(accessKey)) {
            result.append('_')
            result.append(accessKey)
        }
        return result.toString()
    }

    override fun toString(): String {
        return toAttachmentString()
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
