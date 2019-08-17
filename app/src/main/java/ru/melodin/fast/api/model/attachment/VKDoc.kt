package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKDoc(source: JSONObject) : VKModel(), Serializable {
    val id = source.optInt("id")
    val ownerId = source.optInt("owner_id")
    val title: String = source.optString("title")
    val size = source.optInt("size")
    val ext: String = source.optString("ext")
    val url: String = source.optString("url")
    val accessKey: String = source.optString("access_key")
    var type = source.optInt("type")
        private set
    var sizes: ArrayList<VKPhotoSizes.PhotoSize>? = null
        private set

    val src: String?
        get() = if (ArrayUtil.isEmpty(sizes)) null else sizes!![0].src

    val maxSize: String?
        get() {
            if (ArrayUtil.isEmpty(sizes)) return null
            for (i in sizes!!.indices.reversed()) {
                val image = sizes!![i]
                val src = image.src
                if (!TextUtils.isEmpty(src)) {
                    return src
                }
            }

            return null
        }

    init {
        val preview = source.optJSONObject("preview")

        if (preview != null) {
            val photo = preview.optJSONObject("photo")
            if (photo != null) {
                sizes = VKPhotoSizes(photo.optJSONArray("sizes")!!).sizes
            }
        }
    }

    private fun toAttachmentString(): String {
        val result = StringBuilder("doc").append(ownerId).append('_').append(id)
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

        val TYPE_NONE = 0
        val TYPE_TEXT = 1
        val TYPE_ARCHIVE = 2
        val TYPE_GIF = 3
        val TYPE_IMAGE = 4
        val TYPE_AUDIO = 5
        val TYPE_VIDEO = 6
        val TYPE_BOOK = 7
        val TYPE_UNKNOWN = 8
        private const val serialVersionUID = 1L
    }
}
