package ru.melodin.fast.api.model.attachment

import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import java.io.Serializable

class VKLink(source: JSONObject) : VKModel(), Serializable {

    val url: String = source.optString("url")
    val title: String = source.optString("title")
    val caption: String = source.optString("caption")
    val description: String = source.optString("description")
    val previewUrl: String = source.optString("preview_url")

    var photo: VKPhoto? = null
        private set

    init {
        initAttachmentType()

        val linkPhoto = source.optJSONObject("photo")
        if (linkPhoto != null) {
            this.photo = VKPhoto(linkPhoto)
        }
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}

