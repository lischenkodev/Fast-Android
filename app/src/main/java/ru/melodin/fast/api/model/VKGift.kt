package ru.melodin.fast.api.model

import org.json.JSONObject

import java.io.Serializable

class VKGift(source: JSONObject) : VKModel(), Serializable {

    val fromId: Int
    val id: Long
    val message: String? = null
    val date: Long
    val thumb48: String
    val thumb96: String
    val thumb256: String

    init {
        var source = source
        this.id = source.optLong("id")
        this.fromId = source.optInt("from_id")
        this.date = source.optLong("date")

        if (source.has("gift")) {
            source = source.optJSONObject("gift")
        }

        this.thumb48 = source.optString("thumb_48")
        this.thumb96 = source.optString("thumb_96")
        this.thumb256 = source.optString("thumb_256")
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}
