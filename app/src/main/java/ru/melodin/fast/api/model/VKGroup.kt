package ru.melodin.fast.api.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*

open class VKGroup : VKModel, Serializable {

    var id: Int = 0
    var name: String? = null
    var screenName: String? = null
    var isClosed: Boolean = false
    var isAdmin: Boolean = false
    var adminLevel: Int = 0
    var isMember: Boolean = false
    var type: Type? = null
    var isVerified: Boolean = false
    var photo50: String? = null
    var photo100: String? = null
    var photo200: String? = null
    var description: String? = null
    var membersCount: Long = 0
    var status: String? = null

    constructor()

    constructor(source: JSONObject) {
        this.id = source.optInt("id")

        this.name = source.optString("name")
        this.screenName = source.optString("screen_name")
        this.isClosed = source.optInt("is_closed") == 1
        this.isAdmin = source.optInt("is_admin") == 1
        this.isMember = source.optInt("is_member") == 1
        this.isVerified = source.optInt("verified") == 1
        this.adminLevel = source.optInt("admin_level")

        this.type = getType(source.optString("type", "group"))

        this.photo50 = source.optString("photo_50")
        this.photo100 = source.optString("photo_100")
        this.photo200 = source.optString("photo_200")

        this.description = source.optString("description")
        this.status = source.optString("status")
        this.membersCount = source.optLong("members_count")
    }

    override fun toString(): String {
        return name ?: ""
    }

    enum class Type {
        GROUP, PAGE, EVENT
    }

    companion object {

        const val FIELDS_DEFAULT =
            "city,country,place,description,wiki_page,market,members_count,counters,start_date,finish_date,can_post,can_see_all_posts,activity,status,contacts,links,fixed_post,verified,site,ban_info,cover"

        val EMPTY: VKGroup = object : VKGroup() {
            override fun toString(): String {
                return "Group"
            }
        }
        private const val serialVersionUID = 1L

        fun parse(array: JSONArray): ArrayList<VKGroup> {
            val groups = ArrayList<VKGroup>(array.length())
            for (i in 0 until array.length()) {
                groups.add(VKGroup(array.opt(i) as JSONObject))
            }

            return groups
        }

        fun getType(type: String?): Type? {
            return when (type) {
                null -> null
                "group" -> return Type.GROUP
                "page" -> return Type.PAGE
                "event" -> return Type.EVENT
                else -> null
            }
        }

        fun getType(type: Type?): String? {
            return when (type) {
                null -> null
                Type.GROUP -> "group"
                Type.PAGE -> "page"
                Type.EVENT -> "event"
            }
        }

        fun toGroupId(id: Int): Int {
            return if (id < 0) Math.abs(id) else 1000000000 - id
        }

        fun isGroupId(id: Int): Boolean {
            return id < 0
        }
    }
}
