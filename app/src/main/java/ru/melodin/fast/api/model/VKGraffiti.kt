package ru.melodin.fast.api.model

import org.json.JSONObject

import java.io.Serializable

class VKGraffiti internal constructor(o: JSONObject) : VKModel(), Serializable {

    var url: String = o.optString("url")
    var width: Int = 0
    var height: Int = 0

    init {
        this.width = o.optInt("width")
        this.height = o.optInt("height")
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}
