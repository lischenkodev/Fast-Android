package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject

import java.io.Serializable
import java.util.ArrayList

import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil

class VKPhoto(source: JSONObject) : VKModel(), Serializable {

    val id: Int
    val albumId: Int
    val ownerId: Int
    val width: Int
    val height: Int
    val text: String
    val date: Long
    val accessKey: String
    val sizes: ArrayList<VKPhotoSizes.PhotoSize>?
    var maxWidth: Int = 0
        private set
    var maxHeight: Int = 0
        private set
    val maxSize: String?

    init {
        this.id = source.optInt("id")
        this.ownerId = source.optInt("owner_id")
        this.albumId = source.optInt("album_id")
        this.date = source.optLong("date")
        this.width = source.optInt("width")
        this.height = source.optInt("height")
        this.text = source.optString("text")
        this.accessKey = source.optString("access_key")
        this.sizes = VKPhotoSizes(source.optJSONArray("sizes")!!).sizes

        maxSize = findMaxSize()
    }

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
