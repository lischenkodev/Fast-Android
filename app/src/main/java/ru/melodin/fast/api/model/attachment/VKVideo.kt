package ru.melodin.fast.api.model.attachment

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKVideo(source: JSONObject) : VKModel(), Serializable {

    val id: Int
    var ownerId: Int = 0
        private set
    val title: String
    val description: String
    val duration: Int
    val date: Long

    val player: String

    val photo130: String
    val photo320: String
    val photo640: String
    val photo800: String
    val photo1280: String

    val accessKey: String

    var maxWidth: Int = 0
        private set
    val maxSize: String?

    private val sizes = ArrayList<Size>()

    init {
        this.id = source.optInt("id")
        this.ownerId = source.optInt("owner_id")

        if (this.ownerId < 0)
            this.ownerId *= -1

        this.title = source.optString("title")
        this.description = source.optString("description")
        this.duration = source.optInt("duration")
        this.date = source.optLong("date")
        this.player = source.optString("player")
        this.accessKey = source.optString("access_key")

        this.photo130 = source.optString("photo_130")
        this.photo320 = source.optString("photo_320")
        this.photo640 = source.optString("photo_640")
        this.photo800 = source.optString("photo_800")
        this.photo1280 = source.optString("photo_1280")

        sizes.add(Size(130, photo130))
        sizes.add(Size(320, photo320))
        sizes.add(Size(640, photo640))
        sizes.add(Size(800, photo800))
        sizes.add(Size(1280, photo1280))

        maxSize = findMaxSize()
    }

    private fun findMaxSize(): String? {
        if (ArrayUtil.isEmpty(sizes)) return null
        for (i in sizes.indices.reversed()) {
            val size = sizes[i]
            val image = size.url
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

    inner class Size internal constructor(val width: Int, val url: String) : Serializable

    companion object {
        private const val serialVersionUID = 1L
    }
}
