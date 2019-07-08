package ru.melodin.fast.api.model

import org.json.JSONObject
import java.io.Serializable
import java.util.*

class VKVoice(o: JSONObject) : VKModel(), Serializable {

    val duration: Int = o.optInt("duration")
    private val waveform: ArrayList<Int> = ArrayList()
    private val linkOgg: String = o.optString("link_ogg")
    val linkMp3: String = o.optString("link_mp3")

    init {

        val waveform = o.optJSONArray("waveform")
        for (i in 0 until waveform!!.length()) {
            this.waveform.add(waveform.opt(i) as Int)
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
