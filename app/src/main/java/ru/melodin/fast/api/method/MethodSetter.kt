package ru.melodin.fast.api.method

import androidx.collection.ArrayMap
import ru.melodin.fast.api.OnCompleteListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.util.ArrayUtil
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

open class MethodSetter(private val name: String) {
    private val params: ArrayMap<String, String>?

    private val signedUrl: String
        get() = getSignedUrl(false)

    init {
        this.params = ArrayMap()
    }

    fun put(key: String, value: Any?): MethodSetter {
        this.params!![key] = value.toString()
        return this
    }

    fun put(key: String, value: Int): MethodSetter {
        this.params!![key] = value.toString()
        return this
    }

    fun put(key: String, value: Long): MethodSetter {
        this.params!![key] = value.toString()
        return this
    }

    fun put(key: String, value: Boolean): MethodSetter {
        this.params!![key] = if (value) "1" else "0"
        return this
    }

    private fun getSignedUrl(isPost: Boolean): String {
        if (!params!!.containsKey("access_token")) {
            params["access_token"] = UserConfig.accessToken
        }
        if (!params.containsKey("v")) {
            params["v"] = VKApi.API_VERSION
        }
        if (!params.containsKey("lang")) {
            params["lang"] = VKApi.lang
        }

        return VKApi.BASE_URL + name + "?" + if (isPost) "" else getParams()
    }

    fun getParams(): String {
        if (ArrayUtil.isEmpty(params)) return ""

        params ?: return ""

        val buffer = StringBuilder()
        try {
            for (i in 0 until params.size) {
                val key = params.keyAt(i)
                val value = params.valueAt(i)

                if (buffer.isNotEmpty()) {
                    buffer.append("&")
                }

                buffer.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, "UTF-8"))
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return buffer.toString()
    }

    @Throws(Exception::class)
    fun execute() {
        VKApi.execute<Any>(signedUrl, null)
    }

    @Throws(Exception::class)
    fun execute(cls: Class<*>?): ArrayList<*>? {
        return VKApi.execute(signedUrl, cls)
    }

    fun execute(cls: Class<*>?, listener: OnCompleteListener) {
        VKApi.execute(signedUrl, cls, listener)
    }

    fun userId(value: Int): MethodSetter {
        return put("user_id", value)
    }

    fun userIds(vararg ids: Int): MethodSetter {
        return put("user_ids", ArrayUtil.toString(*ids))
    }

    fun userIds(ids: Collection<Int>): MethodSetter {
        return put("user_ids", ArrayUtil.toString(ids))
    }

    fun ownerId(value: Int): MethodSetter {
        return put("owner_id", value)
    }

    fun peerId(value: Int): MethodSetter {
        return put("peer_id", value)
    }

    fun groupId(value: Int): MethodSetter {
        return put("group_id", value)
    }

    fun groupIds(vararg ids: Int): MethodSetter {
        return put("group_ids", ArrayUtil.toString(*ids))
    }

    fun fields(values: String): MethodSetter {
        return put("fields", values)
    }

    fun count(value: Int): MethodSetter {
        return put("count", value)
    }

    fun sound(value: Boolean): MethodSetter {
        return put("sound", value)
    }

    fun sort(value: Int): MethodSetter {
        return put("sort", value)
    }

    fun time(value: Int): MethodSetter {
        return put("time", value)
    }

    fun order(value: String): MethodSetter {
        return put("order", value)
    }

    fun offset(value: Int): MethodSetter {
        return put("offset", value)
    }

    fun nameCase(value: String): MethodSetter {
        return put("name_case", value)
    }

    fun captchaSid(value: String): MethodSetter {
        return put("captcha_sid", value)
    }

    fun captchaKey(value: String): MethodSetter {
        return put("captcha_key", value)
    }

    fun withConfig(config: UserConfig): MethodSetter {
        return put("access_token", UserConfig.accessToken)
    }

    open fun extended(value: Boolean): MethodSetter {
        return put("extended", value)
    }
}
