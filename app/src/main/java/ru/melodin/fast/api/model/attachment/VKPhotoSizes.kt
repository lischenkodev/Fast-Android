package ru.melodin.fast.api.model.attachment

import org.json.JSONArray
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import java.io.Serializable
import java.util.*

class VKPhotoSizes internal constructor(array: JSONArray) : VKModel(), Serializable {

    var sizes: ArrayList<PhotoSize>? = null
        private set

    init {
        sizes = ArrayList(array.length())
        for (i in 0 until array.length()) {
            sizes!!.add(PhotoSize(array.optJSONObject(i)))
        }
    }

    fun forType(type: String): PhotoSize? {
        for (size in sizes!!) {
            if (size.type == type) {
                return size
            }
        }

        return null
    }

    class PhotoSize internal constructor(source: JSONObject) : VKModel(),
        Serializable {

        val src: String = if (source.has("url")) source.optString("url") else source.optString("url")
        val width = source.optInt("width")
        val height = source.optInt("height")
        val type: String? = source.optString("type")

    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
