package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKPhoto(source: JSONObject) : VKModel(), Serializable {

    val id = source.optInt("id")
    val albumId = source.optInt("album_id")
    val ownerId = source.optInt("owner_id")
    val width = source.optInt("width")
    val height = source.optInt("height")
    val text: String = source.optString("text")
    val date = source.optLong("date")
    val accessKey: String = source.optString("access_key")
    val sizes: ArrayList<VKPhotoSizes.PhotoSize>? = VKPhotoSizes(source.optJSONArray("sizes")!!).sizes

    var maxWidth = 0
        private set
    var maxHeight = 0
        private set

    val maxSize: String? = findMaxSize()

    private fun findMaxSize(): String? {
        if (ArrayUtil.isEmpty(sizes)) return null
        for (i in sizes!!.indices.reversed()) {
            val image = sizes[i]
            val src = image.src
            if (!TextUtils.isEmpty(src)) {
                maxWidth = image.width
                maxHeight = image.height
                return src
            }
        }

        return null
    }

    private fun toAttachmentString(): String {
        val result = StringBuilder("photo").append(ownerId).append('_').append(id)
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
