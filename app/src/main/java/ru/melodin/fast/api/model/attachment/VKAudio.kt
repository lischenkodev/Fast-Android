package ru.melodin.fast.api.model.attachment

import android.text.TextUtils

import org.json.JSONObject

import java.io.Serializable

import ru.melodin.fast.api.model.VKModel


class VKAudio(source: JSONObject) : VKModel(), Serializable {

    val id: Long
    val ownerId: Long
    val artist: String
    val title: String
    val duration: Int
    val url: String
    val accessKey: String

    init {
        this.id = source.optLong("id")
        this.ownerId = source.optLong("owner_id")
        this.artist = source.optString("artist")
        this.title = source.optString("title")
        this.duration = source.optInt("duration")
        this.url = source.optString("url")
        this.accessKey = source.optString("access_key")
    }

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
