package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKVideo(source: JSONObject) : VKModel(), Serializable {

    val id: Int = source.optInt("id")
    var ownerId = source.optInt("owner_id")
    val title: String = source.optString("title")
    val description: String? = source.optString("description")
    val duration = source.optInt("duration")
    val date = source.optLong("date")

    private val accessKey: String = source.optString("access_key")

    var maxWidth = 0
    val maxSize: String?

    val sizes = ArrayList<VKPhotoSizes.PhotoSize?>()

    init {
        initAttachmentType()

        if (this.ownerId < 0)
            this.ownerId *= -1


        val image = source.optJSONArray("image")!!
        for (i in 0 until image.length()) {
            sizes.add(VKPhotoSizes.PhotoSize(image.optJSONObject(i)))
        }

        maxSize = findMaxSize()
    }

    private fun findMaxSize(): String? {
        if (ArrayUtil.isEmpty(sizes)) return null
        for (i in sizes.indices.reversed()) {
            val size = sizes[i]!!
            val image = size.src
            if (!TextUtils.isEmpty(image)) {
                maxWidth = size.width
                return image
            }
        }

        return null
    }

    private fun toAttachmentString(): String {
        val result = StringBuilder("video").append(ownerId).append('_').append(id)
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
