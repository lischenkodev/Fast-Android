package ru.melodin.fast.api.model

import org.json.JSONObject
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKChat : VKModel, Serializable {

    var id: Int = 0
    var title: String? = null
    var adminId: Int = 0
    var users = ArrayList<VKUser>()
    var photo50: String? = null
    var photo100: String? = null
    var photo200: String? = null

    var state: VKConversation.State = VKConversation.State.IN
    var type: VKConversation.Type? = VKConversation.Type.USER

    constructor()

    constructor(o: JSONObject) {
        this.id = o.optInt("id", -1)
        this.title = o.optString("title")
        this.adminId = o.optInt("admin_id", -1)

        val users = o.optJSONArray("users")
        if (!ArrayUtil.isEmpty(users))
            this.users = VKUser.parse(users!!)

        this.photo50 = o.optString("photo_50")
        this.photo100 = o.optString("photo_100")
        this.photo200 = o.optString("photo_200")
        this.state = if (o.has("left")) VKConversation.State.LEFT else if (o.has("kicked")) VKConversation.State.KICKED else VKConversation.State.IN
        this.type = VKConversation.getType(o.optString("type"))
    }

    companion object {
        private const val serialVersionUID = 1L

        fun getIntState(state: VKConversation.State?): Int {
            if (state == null) return -1
            return when (state) {
                VKConversation.State.IN -> 0
                VKConversation.State.LEFT -> 1
                VKConversation.State.KICKED -> 2
            }
        }

        fun getState(state: Int): VKConversation.State {
            return when (state) {
                0 -> VKConversation.State.IN
                1 -> VKConversation.State.LEFT
                2 -> VKConversation.State.KICKED
                else -> VKConversation.State.IN
            }
        }
    }
}
