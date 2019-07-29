package ru.melodin.fast.api.model

import org.json.JSONObject
import java.io.Serializable

class VKStory(source: JSONObject) : VKModel(), Serializable {

    val id = source.optLong("id", -1)
    val ownerId = source.optInt("owner_id", -1)
    val data = source.optInt("date", -1)
    val expiresAt = source.optInt("expires_at", -1)
    val expired = source.optBoolean("is_expired", false)

    companion object {
        private const val serialVersionUID = 1L
    }
}