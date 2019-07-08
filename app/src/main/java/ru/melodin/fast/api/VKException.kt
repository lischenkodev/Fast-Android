package ru.melodin.fast.api

import java.io.IOException

class VKException(val url: String, override val message: String, val code: Int) : IOException(message) {

    var captchaSid: String? = null
    var captchaImg: String? = null
    var redirectUri: String? = null

    override fun toString(): String {
        return message
    }
}
