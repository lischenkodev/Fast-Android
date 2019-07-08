package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.squareup.picasso.Picasso
import ru.melodin.fast.R
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.CircleImageView
import java.util.*

class CreateChatAdapter(context: Context, users: ArrayList<VKUser>) : RecyclerAdapter<VKUser, CreateChatAdapter.ViewHolder>(context, R.layout.activity_create_chat_list, users) {

    val selectedPositions: SparseArray<VKUser>
        get() {
            val selected = SparseArray<VKUser>()

            for (i in 0 until values!!.size) {
                val user = values!![i]
                if (user.isSelected) {
                    selected.put(i, user)
                }
            }

            return selected
        }

    override val selectedCount: Int
        get() {
            var count = 0

            for (u in values!!) {
                if (u.isSelected) count++
            }

            return count
        }

    override fun isSelected(position: Int): Boolean {
        return values!![position].isSelected
    }

    fun setSelected(position: Int) {
        values!![position].isSelected = true
    }

    override fun toggleSelected(position: Int) {
        val user = getItem(position)
        user.isSelected = !user.isSelected
    }

    @JvmOverloads
    fun clearSelect(position: Int = -1) {
        if (position != -1) {
            values!![position].isSelected = false
        } else
            for (i in 0 until values!!.size) {
                val u = values!![i]
                if (u.isSelected) {
                    u.isSelected = false
                }
            }
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: CircleImageView
        private var online: ImageView

        private var name: TextView
        private var lastSeen: TextView

        private var root: LinearLayout

        private var selected: AppCompatCheckBox = v.findViewById(R.id.selected)

        private var placeholder: Drawable? = null

        init {

            placeholder = getDrawable(R.drawable.avatar_placeholder)
            root = v.findViewById(R.id.root)

            avatar = v.findViewById(R.id.userAvatar)
            online = v.findViewById(R.id.online)

            name = v.findViewById(R.id.name)
            lastSeen = v.findViewById(R.id.lastSeen)
        }

        override fun bind(position: Int) {
            val user = getItem(position)

            name.text = user.toString()

            if (user.isOnline) {
                lastSeen.visibility = View.GONE
                online.visibility = View.VISIBLE
            } else {
                lastSeen.visibility = View.VISIBLE
                online.visibility = View.GONE
            }

            online.setImageDrawable(getOnlineIndicator(user))

            selected.isChecked = user.isSelected

            val seenText = getString(if (user.sex == VKUser.Sex.MALE) R.string.last_seen_m else R.string.last_seen_w)

            val seen = String.format(seenText, Util.dateFormatter.format(user.lastSeen * 1000))

            if (lastSeen.visibility == View.VISIBLE) {
                lastSeen.text = seen
            } else {
                lastSeen.text = ""
            }

            if (TextUtils.isEmpty(user.photo200)) {
                avatar.setImageDrawable(placeholder)
            } else {
                Picasso.get()
                        .load(user.photo200)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder!!)
                        .into(avatar)
            }
        }

        private fun getOnlineIndicator(user: VKUser): Drawable? {
            return if (!user.isOnline) null else getDrawable(if (user.isOnlineMobile) R.drawable.ic_online_mobile else R.drawable.ic_online)
        }
    }
}
