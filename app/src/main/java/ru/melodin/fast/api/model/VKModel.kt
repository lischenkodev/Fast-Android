package ru.melodin.fast.api.model

import org.json.JSONObject
import ru.melodin.fast.model.Model
import java.io.Serializable

abstract class VKModel : Model, Serializable {

    constructor()

    constructor(source: JSONObject)

    companion object {
        private const val serialVersionUID = 1L
    }
}

