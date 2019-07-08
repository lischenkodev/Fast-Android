package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import ru.melodin.fast.R
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.view.CircleImageView
import java.util.*

class ShowCreateAdapter(context: Context, users: ArrayList<VKUser>) : RecyclerAdapter<VKUser, ShowCreateAdapter.ViewHolder>(context, R.layout.activity_create_show_list, users) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        var avatar: CircleImageView
        var online: ImageView

        var remove: ImageButton

        var name: TextView
        var invitedBy: TextView

        var placeholder: Drawable? = null

        init {

            placeholder = getDrawable(R.drawable.avatar_placeholder)

            remove = v.findViewById(R.id.remove)

            avatar = v.findViewById(R.id.userAvatar)
            online = v.findViewById(R.id.online)

            name = v.findViewById(R.id.name)
            invitedBy = v.findViewById(R.id.lastSeen)
        }

        override fun bind(position: Int) {
            val user = getItem(position)

            name.text = user.toString()

            online.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            online.setImageDrawable(getOnlineIndicator(user))

            val text = if (user.id == UserConfig.userId) getString(R.string.chat_creator) else getString(R.string.invited_by, UserConfig.getUser()!!.toString())
            invitedBy.text = text

            if (TextUtils.isEmpty(user.photo200)) {
                avatar.setImageDrawable(placeholder)
            } else {
                Picasso.get()
                        .load(user.photo200)
                        .placeholder(placeholder!!)
                        .into(avatar)
            }

            remove.visibility = if (user.id == UserConfig.userId) View.GONE else View.VISIBLE

            remove.setOnClickListener {
                if (values!!.size >= 2) {
                    remove(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(0, itemCount, getItem(itemCount - 1))
                }
            }
        }

        private fun getOnlineIndicator(user: VKUser): Drawable? {
            return if (!user.isOnline) null else getDrawable(if (user.isOnlineMobile) R.drawable.ic_online_mobile else R.drawable.ic_online)
        }
    }
}
