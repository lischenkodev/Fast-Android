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
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.R
import ru.melodin.fast.ShowCreateChatActivity
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKGroup
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.fragment.FragmentChatInfo
import ru.melodin.fast.fragment.FragmentFriends
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.CircleImageView
import java.util.*

class UserAdapter : RecyclerAdapter<VKUser, UserAdapter.ViewHolder> {

    private var fragment: BaseFragment? = null

    private var withKick: Boolean = false
    private var conversation: VKConversation? = null

    constructor(context: FragmentFriends, friends: ArrayList<VKUser>) : super(
        context.context!!,
        R.layout.item_user,
        friends
    ) {
        this.fragment = context
        EventBus.getDefault().register(this)
    }

    constructor(
        context: Context,
        users: ArrayList<VKUser>,
        withUpdates: Boolean = false,
        conversation: VKConversation? = null
    ) : super(context, R.layout.item_user, users) {
        if (conversation != null) {
            this.conversation = conversation
            this.withKick = conversation.isCanModerate
        }

        if (withUpdates)
            EventBus.getDefault().register(this)
    }

    constructor(context: Context, users: ArrayList<VKUser>, withKick: Boolean) : super(
        context,
        R.layout.item_user,
        users
    ) {
        this.withKick = withKick

        EventBus.getDefault().register(this)
    }

    constructor(fragment: BaseFragment, users: ArrayList<VKUser>, withKick: Boolean) : super(
        fragment.activity!!,
        R.layout.item_user,
        users
    ) {
        this.fragment = fragment
        this.withKick = withKick
    }

    fun destroy() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.USER_OFFLINE -> setUserOnline(
                online = false,
                mobile = false,
                userId = data[1] as Int,
                time = data[2] as Int
            )
            Keys.USER_ONLINE -> setUserOnline(
                true,
                data[3] as Boolean,
                data[1] as Int,
                data[2] as Int
            )
            Keys.CONNECTED -> {
                fragment ?: return
                if (fragment is FragmentFriends) {
                    val fr = fragment as FragmentFriends
                    if (fr.isLoading) {
                        fr.isLoading = false
                    }
                    fr.onRefresh()
                }
            }
        }
    }

    fun searchUser(userId: Int): Int {
        values!!.forEach {
            if (it.id == userId) return values!!.indexOf(it)
        }
        return -1
    }

    private fun setUserOnline(online: Boolean, mobile: Boolean, userId: Int, time: Int) {
        for (i in 0 until itemCount) {
            val user = getItem(i)
            if (user.id == userId) {
                user.isOnline = online
                user.isOnlineMobile = mobile
                user.lastSeen = time.toLong()
                notifyItemChanged(i, -1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: CircleImageView = v.findViewById(R.id.userAvatar)
        private var online: ImageView = v.findViewById(R.id.online)

        private var name: TextView = v.findViewById(R.id.name)
        private var lastSeen: TextView = v.findViewById(R.id.lastSeen)

        private var message: ImageButton = v.findViewById(R.id.message)
        private var functions: ImageButton = v.findViewById(R.id.functions)
        private var kick: ImageButton = v.findViewById(R.id.kick)

        private var contactContainer: LinearLayout = v.findViewById(R.id.contact_container)

        private var placeholder = getDrawable(R.drawable.avatar_placeholder)

        override fun bind(position: Int) {
            if (fragment != null && fragment is FragmentFriends) {
                contactContainer.visibility = View.VISIBLE
                message.setOnClickListener {
                    if (fragment is FragmentFriends) (fragment as FragmentFriends).openChat(
                        position
                    )
                }

                functions.setOnClickListener { v ->
                    if (fragment is FragmentFriends) (fragment as FragmentFriends).showDialog(
                        position,
                        v
                    )
                }
            } else {
                contactContainer.visibility = View.GONE
            }

            val user = getItem(position)

            if (withKick) {
                kick.visibility = View.VISIBLE
                kick.setOnClickListener {
                    if (context is ShowCreateChatActivity && fragment == null) {
                        (context as ShowCreateChatActivity).confirmKick(position)
                    } else if (fragment is FragmentChatInfo) {
                        (fragment as FragmentChatInfo).confirmKick(user.id)
                    }
                }
            } else {
                kick.visibility = View.GONE
            }

            name.text = user.toString()

            online.setImageDrawable(getOnlineIndicator(context, user))

            when {
                VKGroup.isGroupId(user.id) -> {
                    lastSeen.visibility = View.GONE
                    online.visibility = View.GONE
                }
                user.isOnline -> {
                    lastSeen.visibility = View.GONE
                    online.visibility = View.VISIBLE
                }
                else -> {
                    lastSeen.visibility = View.VISIBLE
                    online.visibility = View.GONE
                }
            }

            if (conversation != null) {
                var invitedUser = MemoryCache.getUser(user.invitedBy)
                if (invitedUser == null) {
                    invitedUser = VKUser.EMPTY
                    EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, user.invitedBy))
                }

                val text =
                    if (invitedUser.id == user.id) getString(R.string.chat_creator) else getString(
                        R.string.invited_by,
                        invitedUser.toString()
                    )
                lastSeen.text = text
                lastSeen.visibility = View.VISIBLE
            } else {
                if (lastSeen.visibility == View.VISIBLE) {
                    val seen = getString(
                        if (user.sex == VKUser.Sex.MALE)
                            R.string.last_seen_m
                        else
                            R.string.last_seen_w,
                        Util.dateFormatter.format(user.lastSeen * 1000)
                    )

                    lastSeen.text = seen
                } else {
                    lastSeen.text = ""
                }
            }

            if (TextUtils.isEmpty(user.photo200)) {
                avatar.setImageDrawable(
                    ColorDrawable(
                        ColorUtil.alphaColor(
                            ThemeManager.primary,
                            0.5f
                        )
                    )
                )
            } else {
                Picasso.get()
                    .load(user.photo200)
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(placeholder!!)
                    .into(avatar)
            }
        }
    }

    companion object {
        fun getOnlineIndicator(context: Context, user: VKUser): Drawable? {
            return if (!user.isOnline) null else ContextCompat.getDrawable(
                context,
                if (user.isOnlineMobile) R.drawable.ic_online_mobile else R.drawable.ic_online_pc
            )
        }
    }
}
