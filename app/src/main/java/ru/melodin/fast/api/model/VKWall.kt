package ru.melodin.fast.api.model

import org.json.JSONObject
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKWall internal constructor(o: JSONObject) : VKModel(), Serializable {

    val id: Int = o.optInt("id", -1)
    val ownerId: Int = o.optInt("owner_id", -1)
    val fromId: Int = o.optInt("from_id", -1)
    val createdBy: Int = o.optInt("created_by", -1)
    val date: Int = o.optInt("date", -1)
    val text: String = o.optString("text", "")
    private val replyOwnerId: Int = o.optInt("reply_owner_id", -1)
    private val replyPostId: Int = o.optInt("reply_post_id", -1)
    private val isFriendsOnly: Boolean = o.optInt("friends_only", -1) == 1
    var attachments: ArrayList<VKModel>? = null
        private set

    init {
        val attachments = o.optJSONArray("attachments")
        if (!ArrayUtil.isEmpty(attachments))
            this.attachments = VKAttachments.parse(o.optJSONArray("attachments")!!)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
