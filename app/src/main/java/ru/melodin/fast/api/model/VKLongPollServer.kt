package ru.melodin.fast.api.model

import org.json.JSONObject

import java.io.Serializable

class VKLongPollServer(source: JSONObject) : VKModel(), Serializable {

    var key: String = source.optString("key")
    var server: String = source.optString("server").replace("\\", "")
    var ts: Long = source.optLong("ts")

    companion object {
        private const val serialVersionUID = 1L
    }
}
