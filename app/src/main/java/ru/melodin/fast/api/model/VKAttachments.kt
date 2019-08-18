package ru.melodin.fast.api.model

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import org.json.JSONArray
import ru.melodin.fast.R
import ru.melodin.fast.api.model.attachment.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.util.ArrayUtil
import java.io.Serializable
import java.util.*

class VKAttachments : VKModel(), Serializable {
    companion object {

        private const val TYPE_PHOTO = "photo"
        private const val TYPE_VIDEO = "video"
        private const val TYPE_AUDIO = "audio"
        private const val TYPE_DOC = "doc"
        private const val TYPE_WALL = "wall"
        private const val TYPE_POSTED_PHOTO = "posted_photo"
        private const val TYPE_LINK = "link"
        private const val TYPE_NOTE = "note"
        private const val TYPE_APP = "app"
        private const val TYPE_POLL = "poll"
        private const val TYPE_WIKI_PAGE = "page"
        private const val TYPE_ALBUM = "album"
        private const val TYPE_STICKER = "sticker"
        private const val TYPE_STORY = "story"
        private const val TYPE_GIFT = "gift"
        private const val TYPE_AUDIO_MESSAGE = "audio_message"
        private const val TYPE_GRAFFITI = "graffiti"

        fun parse(array: JSONArray): ArrayList<VKModel> {
            val attachments = ArrayList<VKModel>(array.length())

            for (i in 0 until array.length()) {
                var attachment = array.optJSONObject(i)!!

                if (attachment.has("attachment")) {
                    attachment = attachment.optJSONObject("attachment")!!
                }

                val type = attachment.optString("type")
                val source = attachment.optJSONObject(type) ?: return attachments

                when (type) {
                    TYPE_STORY -> attachments.add(VKStory(source))
                    TYPE_PHOTO -> attachments.add(VKPhoto(source))
                    TYPE_AUDIO -> attachments.add(VKAudio(source))
                    TYPE_VIDEO -> attachments.add(VKVideo(source))
                    TYPE_DOC -> attachments.add(VKDoc(source))
                    TYPE_STICKER -> attachments.add(VKSticker(source))
                    TYPE_LINK -> attachments.add(VKLink(source))
                    TYPE_GIFT -> attachments.add(VKGift(source))
                    TYPE_AUDIO_MESSAGE -> attachments.add(VKVoice(source))
                    TYPE_GRAFFITI -> attachments.add(VKGraffiti(source))
                    TYPE_WALL -> attachments.add(VKWall(source))
                }
            }

            return attachments
        }

        @SuppressLint("DefaultLocale")
        fun getAttachmentString(attachments: ArrayList<VKModel>): String {
            if (ArrayUtil.isEmpty(attachments)) return ""
            val b = StringBuilder()

            if (attachments.size > 1) {
                return attachments.size.toString() + " " + getString(R.string.attachments_lot).toLowerCase()
            }

            for (attach in attachments) {
                when (attach) {
                    is VKAudio -> b.append(getString(R.string.audio))
                    is VKPhoto -> b.append(getString(R.string.photo))
                    is VKSticker -> b.append(getString(R.string.sticker))
                    is VKDoc -> b.append(getString(R.string.doc))
                    is VKLink -> b.append(getString(R.string.link))
                    is VKVideo -> b.append(getString(R.string.video))
                    is VKVoice -> b.append(getString(R.string.voice_message))
                    is VKGraffiti -> b.append(getString(R.string.graffiti))
                    is VKGift -> b.append(getString(R.string.gift))
                    is VKWall -> b.append(getString(R.string.wall_post))
                    else -> b.append(getString(R.string.attachment))
                }
            }

            return b.toString()
        }

        private fun isOneType(type: Class<*>?, attachments: ArrayList<VKModel>): Boolean {
            if (ArrayUtil.isEmpty(attachments) || type == null) return false

            for (a in attachments) {
                if (a.javaClass != type) {
                    return false
                }
            }

            return true
        }

        private fun getString(@StringRes resId: Int): String {
            return AppGlobal.res.getString(resId)
        }
    }
}
