package ru.melodin.fast.api


import android.text.TextUtils
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.database.MemoryCache

class UserConfig {

    constructor(accessToken: String?, email: String?, userId: Int, apiId: Int) {
        UserConfig.accessToken = accessToken
        UserConfig.email = email
        UserConfig.userId = userId
        UserConfig.apiId = apiId
    }

    constructor()



    companion object {

        private const val ACCESS_TOKEN = "access_token"
        private const val USER_ID = "user_id"
        private const val EMAIL = "email"
        private const val API_ID = "api_id"

        var userId: Int = -1


        var accessToken: String? = null

        private var email: String? = null
        private var apiId: Int = -1

        val isLoggedIn: Boolean
            get() = userId > 0 && accessToken != null && !TextUtils.isEmpty(accessToken!!.trim())



        fun restore(): UserConfig {
            val userId = AppGlobal.preferences.getInt(USER_ID, -1)
            val apiId = AppGlobal.preferences.getInt(API_ID, -1)
            val accessToken = AppGlobal.preferences.getString(ACCESS_TOKEN, null)
            val email = AppGlobal.preferences.getString(EMAIL, null)

            return UserConfig(accessToken, email, userId, apiId)
        }

        fun clear() {
            AppGlobal.preferences.edit()
                    .remove(ACCESS_TOKEN)
                    .remove(API_ID)
                    .remove(USER_ID)
                    .remove(EMAIL)
                    .apply()
        }

        fun save() {
            AppGlobal.preferences.edit()
                    .putInt(USER_ID, userId)
                    .putInt(API_ID, apiId)
                    .putString(ACCESS_TOKEN, accessToken)
                    .putString(EMAIL, email)
                    .apply()

        }

        fun getUser(): VKUser? {
            return MemoryCache.getUser(userId)
        }
    }
}
