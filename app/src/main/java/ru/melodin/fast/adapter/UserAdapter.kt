package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.ChatInfoActivity
import ru.melodin.fast.R
import ru.melodin.fast.ShowCreateChatActivity
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.fragment.FragmentFriends
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.CircleImageView
import java.util.*

class UserAdapter : RecyclerAdapter<VKUser, UserAdapter.ViewHolder> {

    private var fragment: FragmentFriends? = null

    private var withKick: Boolean = false
    private var conversation: VKConversation? = null

    constructor(context: FragmentFriends, friends: ArrayList<VKUser>) : super(context.context!!, R.layout.item_user, friends) {
        this.fragment = context
        EventBus.getDefault().register(this)
    }

    constructor(context: Context, users: ArrayList<VKUser>, withUpdates: Boolean = false, conversation: VKConversation? = null) : super(context, R.layout.item_user, users) {
        if (conversation != null) {
            this.conversation = conversation
            this.withKick = conversation.isCanModerate
        }

        if (withUpdates)
            EventBus.getDefault().register(this)
    }

    constructor(context: Context, users: ArrayList<VKUser>, withKick: Boolean) : super(context, R.layout.item_user, users) {
        this.withKick = withKick

        EventBus.getDefault().register(this)
    }

    fun destroy() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.KEY_USER_OFFLINE -> setUserOnline(false, data[1] as Int, data[2] as Long)
            Keys.KEY_USER_ONLINE -> setUserOnline(true, data[1] as Int, data[2] as Long)
            Keys.CONNECTED -> {
                fragment ?: return
                if (fragment!!.isLoading)
                    fragment!!.isLoading = false
                fragment!!.onRefresh()
            }
        }
    }

    private fun setUserOnline(online: Boolean, userId: Int, time: Long) {
        for (i in 0 until itemCount) {
            val user = getItem(i)
            if (user.id == userId) {
                user.isOnline = online
                user.lastSeen = time
                notifyItemChanged(i, -1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: CircleImageView
        private var online: ImageView

        private var name: TextView
        private var lastSeen: TextView

        private var message: ImageButton
        private var functions: ImageButton
        private var kick: ImageButton

        private var contactContainer: LinearLayout

        private var placeholder: Drawable? = null

        init {
            placeholder = getDrawable(R.drawable.avatar_placeholder)

            avatar = v.findViewById(R.id.userAvatar)
            online = v.findViewById(R.id.online)

            lastSeen = v.findViewById(R.id.lastSeen)
            name = v.findViewById(R.id.name)

            message = v.findViewById(R.id.message)
            functions = v.findViewById(R.id.functions)
            kick = v.findViewById(R.id.kick)

            contactContainer = v.findViewById(R.id.contact_container)
        }

        override fun bind(position: Int) {
            if (fragment != null) {
                contactContainer.visibility = View.VISIBLE
                message.setOnClickListener { fragment!!.openChat(position) }

                functions.setOnClickListener { v -> fragment!!.showDialog(position, v) }
            } else {
                contactContainer.visibility = View.GONE
            }

            if (withKick) {
                kick.visibility = View.VISIBLE
                kick.setOnClickListener {
                    if (context is ChatInfoActivity) {
                        (context as ChatInfoActivity).confirmKick(position)
                    } else if (context is ShowCreateChatActivity) {
                        (context as ShowCreateChatActivity).confirmKick(position)
                    }
                }
            } else {
                kick.visibility = View.GONE
            }

            val user = getItem(position)

            name.text = user.toString()

            online.setImageDrawable(getOnlineIndicator(user))

            if (user.isOnline) {
                lastSeen.visibility = View.GONE
                online.visibility = View.VISIBLE
            } else {
                lastSeen.visibility = View.VISIBLE
                online.visibility = View.GONE
            }

            if (conversation != null) {
                var invitedUser = MemoryCache.getUser(user.invitedBy)
                if (invitedUser == null) {
                    invitedUser = VKUser.EMPTY
                    EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, user.invitedBy))
                }

                val text = if (invitedUser.id == user.id) getString(R.string.chat_creator) else getString(R.string.invited_by, invitedUser.toString())
                lastSeen.text = text
                lastSeen.visibility = View.VISIBLE
            } else {
                if (lastSeen.visibility == View.VISIBLE) {
                    val seen = getString(
                            if (user.sex == VKUser.Sex.MALE)
                                R.string.last_seen_m
                            else
                                R.string.last_seen_w,
                            Util.dateFormatter.format(user.lastSeen * 1000))

                    lastSeen.text = seen
                } else {
                    lastSeen.text = ""
                }
            }

            if (TextUtils.isEmpty(user.photo200)) {
                avatar.setImageDrawable(ColorDrawable(ColorUtil.alphaColor(ThemeManager.primary, 0.5f)))
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
