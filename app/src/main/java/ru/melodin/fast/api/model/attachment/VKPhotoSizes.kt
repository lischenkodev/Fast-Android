package ru.melodin.fast.api.model.attachment

import org.json.JSONArray
import org.json.JSONObject
import ru.melodin.fast.api.model.VKModel
import java.io.Serializable
import java.util.*

class VKPhotoSizes : VKModel, Serializable {

    var sizes: ArrayList<PhotoSize>? = null
        private set

    internal constructor(array: JSONArray) {
        sizes = ArrayList(array.length())
        for (i in 0 until array.length()) {
            sizes!!.add(PhotoSize(array.optJSONObject(i)))
        }
    }

    internal constructor(array: JSONArray, doc: Boolean) {
        sizes = ArrayList(array.length())

        for (i in 0 until array.length()) {
            sizes!!.add(PhotoSize(array.optJSONObject(i), doc))
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

    class PhotoSize internal constructor(source: JSONObject, doc: Boolean) : VKModel(),
        Serializable {

        val src: String
        val width: Int
        val height: Int
        val type: String

        internal constructor(o: JSONObject) : this(o, false)

        init {
            this.src = if (doc) source.optString("url") else source.optString("url")
            this.width = source.optInt("width")
            this.height = source.optInt("height")
            this.type = source.optString("type")
        }
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}
