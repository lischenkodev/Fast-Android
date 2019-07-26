package ru.melodin.fast.api

import android.app.Activity
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import ru.melodin.fast.BuildConfig
import ru.melodin.fast.LoginActivity
import ru.melodin.fast.api.method.AppMethodSetter
import ru.melodin.fast.api.method.MessageMethodSetter
import ru.melodin.fast.api.method.MethodSetter
import ru.melodin.fast.api.method.UserMethodSetter
import ru.melodin.fast.api.model.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.net.HttpRequest
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import java.util.*

object VKApi {
    private const val TAG = "Fast.VKApi"

    const val BASE_URL = "https://api.vk.com/method/"
    const val API_VERSION = "5.103"

    var config: UserConfig? = null
    var lang: String? = AppGlobal.locale.language

    @Throws(Exception::class)
    fun <T> execute(url: String, cls: Class<T>?): ArrayList<T>? {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "url: $url")
        }

        val buffer = HttpRequest[url].asString()

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "json: $buffer")
        }

        val json = JSONObject(buffer)
        try {
            checkError(json, url)
        } catch (ex: VKException) {
            return if (ex.code == ErrorCodes.TOO_MANY_REQUESTS) {
                execute(url, cls)
            } else
                throw ex
        }

        if (cls == null) {
            return null
        }

        if (BuildConfig.DEBUG)
            Log.i("Class is", cls.simpleName)

        if (cls == VKLongPollServer::class.java) {
            val server = VKLongPollServer(json.optJSONObject("response")!!)
            return ArrayUtil.singletonList(server) as ArrayList<T>
        }

        if (cls == Boolean::class.java) {
            val value = json.optInt("response") == 1
            return ArrayUtil.singletonList(value) as ArrayList<T>
        }

        if (cls == Long::class.java) {
            val value = json.optLong("response")
            return ArrayUtil.singletonList(value) as ArrayList<T>
        }

        if (cls == Int::class.java || cls == Int::class.javaPrimitiveType) {
            val value = json.optInt("response")
            return ArrayUtil.singletonList(value) as ArrayList<T>
        }

        val array = optItems(json)

        val models: ArrayList<T>

        if (cls == VKChat::class.java && !url.contains("chat_ids"))
            models = ArrayList(1)
        else
            models = ArrayList(array!!.length())

        if (cls == VKUser::class.java) {
            if (url.contains("friends.get")) {
                VKUser.count = json.optJSONObject("response")!!.optInt("count")
            }

            for (i in 0 until array!!.length()) {
                models.add(VKUser(array.optJSONObject(i)) as T)
            }
        } else if (cls == VKMessage::class.java) {
            if (url.contains("messages.getHistory")) {
                VKMessage.lastHistoryCount = json.optJSONObject("response")!!.optInt("count")

                val groups = json.optJSONObject("response")!!.optJSONArray("groups")
                if (groups != null && groups.length() > 0) {
                    VKMessage.groups = VKGroup.parse(groups)
                }

                val profiles = json.optJSONObject("response")!!.optJSONArray("profiles")
                if (profiles != null && profiles.length() > 0) {
                    VKMessage.users = VKUser.parse(profiles)
                }
            }

            for (i in 0 until array!!.length()) {
                var source: JSONObject? = array.optJSONObject(i)
                val unread = source!!.optInt("unread")
                if (source.has("message")) {
                    source = source.optJSONObject("message")
                }
                val message = VKMessage(source!!)
                message.unread = unread
                models.add(message as T)
            }
        } else if (cls == VKGroup::class.java) {
            for (i in 0 until array!!.length()) {
                models.add(VKGroup(array.optJSONObject(i)) as T)
            }
        } else if (cls == VKModel::class.java && url.contains("messages.getHistoryAttachments")) {
            return VKAttachments.parse(array!!) as ArrayList<T>
        } else if (cls == VKConversation::class.java) {
            if (url.contains("messages.getConversations?")) {
                val response = json.optJSONObject("response")
                VKConversation.count = response!!.optInt("count")

                val groups = response.optJSONArray("groups")
                if (groups != null && groups.length() > 0) {
                    VKConversation.groups = VKGroup.parse(groups)
                }

                val profiles = response.optJSONArray("profiles")
                if (profiles != null && profiles.length() > 0) {
                    VKConversation.users = VKUser.parse(profiles)
                }
            }

            for (i in 0 until array!!.length()) {
                val source = array.optJSONObject(i)
                val conversation: VKConversation
                conversation = if (url.contains("getConversations?")) {
                    val jConversation = source.optJSONObject("conversation")
                    val jLastMessage = source.optJSONObject("last_message")

                    VKConversation(jConversation, jLastMessage)
                } else {
                    VKConversation(source, null)
                }
                models.add(conversation as T)
            }
        } else if (cls == VKChat::class.java) {
            if (!url.contains("chat_ids")) {
                models.add(VKChat(json.optJSONObject("response")!!) as T)
            }
        }

        return models
    }

    fun execute(url: String, cls: Class<*>?,
                listener: OnCompleteListener?) {
        TaskManager.execute {
            try {
                val models = execute(url, cls)
                if (listener != null) {
                    AppGlobal.handler.post { listener.onComplete(models as ArrayList<*>?) }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                if (listener != null) {
                    AppGlobal.handler.post { listener.onError(e) }
                }
            }


        }
    }

    private fun optItems(source: JSONObject): JSONArray? {
        val response = source.opt("response")
        if (response is JSONArray) {
            return response
        }

        return if (response is JSONObject) {
            if (!response.has("items")) null else response.optJSONArray("items")
        } else null

    }

    fun checkError(activity: Activity, e: Exception) {
        if (e !is VKException) return
        if (e.code == ErrorCodes.USER_AUTHORIZATION_FAILED) {
            activity.finishAffinity()
            activity.startActivity(Intent(activity, LoginActivity::class.java))
            activity.stopService(Intent(activity, LongPollService::class.java))
            UserConfig.clear()
            DatabaseHelper.getInstance().dropTables(AppGlobal.database)
            DatabaseHelper.getInstance().onCreate(AppGlobal.database)
        }
    }

    @Throws(VKException::class)
    private fun checkError(json: JSONObject, url: String) {
        if (json.has("error")) {
            val error = json.optJSONObject("error")

            val code = error!!.optInt("error_code")
            val message = error.optString("error_msg")

            val e = VKException(url, message, code)
            if (code == ErrorCodes.CAPTCHA_NEEDED) {
                e.captchaImg = error.optString("captcha_img")
                e.captchaSid = error.optString("captcha_sid")
            }
            if (code == ErrorCodes.VALIDATION_REQUIRED) {
                e.redirectUri = error.optString("redirect_uri")
            }
            throw e
        }
    }

    fun users(): VKUsers {
        return VKUsers()
    }

    fun friends(): VKFriends {
        return VKFriends()
    }

    fun messages(): VKMessages {
        return VKMessages()
    }

    fun groups(): VKGroups {
        return VKGroups()
    }

    fun apps(): VKApps {
        return VKApps()
    }

    fun account(): VKAccounts {
        return VKAccounts()
    }

    fun stats(): VKStats {
        return VKStats()
    }

    class VKStats internal constructor() {

        fun trackVisitor(): MethodSetter {
            return MethodSetter("stats.trackVisitor")
        }
    }

    class VKFriends {

        fun get(): MethodSetter {
            return MethodSetter("friends.get")
        }

        fun delete(): MethodSetter {
            return MethodSetter("friends.delete")
        }
    }

    class VKUsers {

        fun get(): UserMethodSetter {
            return UserMethodSetter("users.get")
        }
    }

    class VKMessages {

        val conversations: MessageMethodSetter
            get() = MessageMethodSetter("getConversations")

        val conversationsById: MessageMethodSetter
            get() = MessageMethodSetter("getConversationsById")

        val byId: MessageMethodSetter
            get() = MessageMethodSetter("getById")

        val history: MessageMethodSetter
            get() = MessageMethodSetter("getHistory")

        val historyAttachments: MessageMethodSetter
            get() = MessageMethodSetter("getHistoryAttachments")

        val longPollServer: MessageMethodSetter
            get() = MessageMethodSetter("getLongPollServer")

        val longPollHistory: MessageMethodSetter
            get() = MessageMethodSetter("getLongPollHistory")

        val chat: MessageMethodSetter
            get() = MessageMethodSetter("getChat")

        val chatUsers: MessageMethodSetter
            get() = MessageMethodSetter("getChatUsers")

        fun edit(): MessageMethodSetter {
            return MessageMethodSetter("edit")
        }

        fun unpin(): MessageMethodSetter {
            return MessageMethodSetter("unpin")
        }

        fun pin(): MessageMethodSetter {
            return MessageMethodSetter("pin")
        }

        fun search(): MessageMethodSetter {
            return MessageMethodSetter("search")
        }

        fun send(): MessageMethodSetter {
            return MessageMethodSetter("send")
        }

        fun sendSticker(): MessageMethodSetter {
            return MessageMethodSetter("sendSticker")
        }

        fun delete(): MessageMethodSetter {
            return MessageMethodSetter("delete")
        }

        fun deleteConversation(): MessageMethodSetter {
            return MessageMethodSetter("deleteConversation")
        }

        fun restore(): MessageMethodSetter {
            return MessageMethodSetter("restore")
        }

        fun markAsRead(): MessageMethodSetter {
            return MessageMethodSetter("markAsRead")
        }

        fun markAsImportant(): MessageMethodSetter {
            return MessageMethodSetter("markAsImportant")
        }

        fun createChat(): MessageMethodSetter {
            return MessageMethodSetter("createChat")
        }

        fun editChat(): MessageMethodSetter {
            return MessageMethodSetter("editChat")
        }

        fun setActivity(): MessageMethodSetter {
            return MessageMethodSetter("setActivity").type(true)
        }

        fun addChatUser(): MessageMethodSetter {
            return MessageMethodSetter("addChatUser")
        }

        fun removeChatUser(): MessageMethodSetter {
            return MessageMethodSetter("removeChatUser")
        }

        fun deleteChatPhoto(): MessageMethodSetter {
            return MessageMethodSetter("deleteChatPhoto")
        }
    }

    class VKGroups {
        val byId: MethodSetter
            get() = MethodSetter("groups.getById")

        fun join(): MethodSetter {
            return MethodSetter("groups.join")
        }
    }

    class VKApps {
        fun get(): AppMethodSetter {
            return AppMethodSetter("apps.get")
        }
    }

    class VKAccounts {
        fun setOffline(): MethodSetter {
            return MethodSetter("account.setOffline")
        }

        fun setOnline(): MethodSetter {
            return MethodSetter("account.setOnline")
        }

        fun setSilenceMode(): MethodSetter {
            return MethodSetter("account.setSilenceMode")
        }
    }
}
