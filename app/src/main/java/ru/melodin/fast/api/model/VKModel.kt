package ru.melodin.fast.api.model

import org.json.JSONObject
import ru.melodin.fast.api.model.attachment.*
import ru.melodin.fast.model.Model
import java.io.Serializable

abstract class VKModel : Model, Serializable {

    constructor()

    constructor(source: JSONObject)

    internal fun initAttachmentType() {
        this.modelType = when (this) {
            is VKPhoto -> TYPE_PHOTO
            is VKVideo -> TYPE_VIDEO
            is VKAudio -> TYPE_AUDIO
            is VKDoc -> TYPE_DOC
            is VKLink -> TYPE_LINK
            else -> -1
        }
    }

    var modelType: Int = -1

    companion object {
        private const val serialVersionUID = 1L

        const val TYPE_PHOTO = 0
        const val TYPE_VIDEO = 1
        const val TYPE_AUDIO = 2
        const val TYPE_DOC = 3
        const val TYPE_LINK = 4
    }

}

