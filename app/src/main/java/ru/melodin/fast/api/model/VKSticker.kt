package ru.melodin.fast.api.model

import android.text.TextUtils
import org.json.JSONObject
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKSticker(source: JSONObject) : VKModel(), Serializable {

    val id: Int = source.optInt("sticker_id")
    private val productId: Int = source.optInt("product_id")
    private val images = ArrayList<Size>()
    private val backgroundImages = ArrayList<Size>()

    var maxWidth: Int = 0
        private set
    var maxHeight: Int = 0
        private set
    val maxSize: String?
    val maxBackgroundSize: String?

    init {

        val images = source.optJSONArray("images")
        for (i in 0 until images!!.length()) {
            val size = images.optJSONObject(i)
            this.images.add(Size(size))
        }

        val backgroundImages = source.optJSONArray("images_with_background")
        for (i in 0 until backgroundImages!!.length()) {
            val size = backgroundImages.optJSONObject(i)
            this.backgroundImages.add(Size(size))
        }

        maxSize = findMaxSize()
        maxBackgroundSize = findMaxBackgroundSize()
    }

    fun size(width: Int): String? {
        if (ArrayUtil.isEmpty(images)) return null
        for (size in images) {
            if (size.width == width)
                return size.url
        }

        return null
    }

    fun backgroundSize(width: Int): String? {
        if (ArrayUtil.isEmpty(backgroundImages)) return null
        for (size in backgroundImages) {
            if (size.width == width)
                return size.url
        }

        return null

    }

    private fun findMaxSize(): String? {
        if (ArrayUtil.isEmpty(images)) return null
        for (i in images.indices.reversed()) {
            val size = images[i]
            val image = size.url
            if (!TextUtils.isEmpty(image)) {
                maxWidth = size.width
                maxHeight = size.height
                return image
            }
        }

        return null
    }

    private fun findMaxBackgroundSize(): String? {
        if (ArrayUtil.isEmpty(backgroundImages)) return null
        for (i in backgroundImages.indices.reversed()) {
            val image = backgroundImages[i].url
            if (!TextUtils.isEmpty(image)) {
                return image
            }
        }

        return null
    }

    inner class Size(o: JSONObject) : Serializable {

        val width: Int = o.optInt("width")
        val height: Int = o.optInt("height")
        val url: String = o.optString("url")

    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
