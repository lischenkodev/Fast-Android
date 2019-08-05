package ru.melodin.fast.api.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*

open class VKUser : VKModel, Serializable {

    var id: Int = 0
    var invitedBy: Int = 0
    var name: String? = null
    var surname: String? = null
    var fullName: String? = null
    var screenName: String? = null
    var isOnline: Boolean = false
    var isOnlineMobile: Boolean = false
    var onlineApp: Int = 0
    var photo50: String? = null
    var photo100: String? = null
    var photo200: String? = null
    var status: String? = null
    var lastSeen: Long = 0
    var isVerified: Boolean = false
    var isDeactivated: Boolean = false
    var sex: Int = 0

    constructor() {
        this.name = "..."
        this.surname = ""
    }

    constructor(source: JSONObject) {

        this.id = source.optInt("id")
        this.invitedBy = source.optInt("invited_by", -1)
        this.name = source.optString("first_name", "...")
        this.surname = source.optString("last_name", "")

        fullName = "$name $surname"

        val deactivated = source.optString("deactivated")
        this.isDeactivated = deactivated == "deleted" || deactivated == "banned"

        this.photo50 = source.optString("photo_50")
        this.photo100 = source.optString("photo_100")
        this.photo200 = source.optString("photo_200")

        this.screenName = source.optString("screen_name")
        this.isOnline = source.optInt("online") == 1
        this.status = source.optString("status")
        this.isOnlineMobile = source.optInt("online_mobile") == 1
        this.isVerified = source.optInt("verified") == 1

        this.sex = source.optInt("sex")
        if (this.isOnlineMobile) {
            this.onlineApp = source.optInt("online_app")
        }

        val lastSeen = source.optJSONObject("last_seen")
        if (lastSeen != null) {
            this.lastSeen = lastSeen.optLong("time")
        }
    }

    override fun toString(): String {
        return "$name $surname"
    }

    object Sex {
        const val NONE = 0
        const val FEMALE = 1
        const val MALE = 2
    }

    companion object {

        const val FIELDS_DEFAULT =
            "photo_50,photo_100,photo_200,status,screen_name,online,online_mobile,last_seen,verified,sex"

        val EMPTY: VKUser = object : VKUser() {
            override fun toString(): String {
                return "..."
            }
        }

        private const val serialVersionUID = 1L

        var count: Int = 0

        fun parse(array: JSONArray): ArrayList<VKUser> {
            val users = ArrayList<VKUser>(array.length())
            for (i in 0 until array.length()) {
                users.add(VKUser(array.opt(i) as JSONObject))
            }

            return users
        }
    }

}
