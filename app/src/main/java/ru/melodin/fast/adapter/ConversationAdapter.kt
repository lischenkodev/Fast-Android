package ru.melodin.fast.adapter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.annotations.Contract
import ru.melodin.fast.R
import ru.melodin.fast.api.OnCompleteListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKUtil
import ru.melodin.fast.api.model.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.fragment.FragmentConversations
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import java.util.*

class ConversationAdapter(private val fragment: FragmentConversations, values: ArrayList<VKConversation>) :
        RecyclerAdapter<VKConversation, ConversationAdapter.ViewHolder>(fragment.context!!, R.layout.item_conversation, values) {

    private val manager: LinearLayoutManager = (fragment.recyclerView!!.layoutManager as LinearLayoutManager?)!!

    private var lastUpdateId: Int = 0

    private val loadingIds = ArrayList<Int>()

    init {
        UserConfig.getUser()
        EventBus.getDefault().register(this)
    }

    fun destroy() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.USER_OFFLINE -> setUserOnline(online = false, mobile = false, userId = data[1] as Int, time = data[2] as Int)
            Keys.USER_ONLINE -> setUserOnline(true, data[3] as Boolean, data[1] as Int, data[2] as Int)
            Keys.MESSAGE_CLEAR_FLAGS -> handleClearFlags(data)
            Keys.MESSAGE_NEW -> {
                addMessage(data[1] as VKConversation)
            }
            Keys.MESSAGE_EDIT -> {
                val message = data[1] as VKMessage
                editMessage(message)
            }
            Keys.UPDATE_MESSAGE -> updateMessage(data[1] as Int)
            FragmentSettings.KEY_MESSAGES_CLEAR_CACHE -> {
                clear()
                notifyDataSetChanged()
                fragment.onRefresh()
            }
            Keys.NOTIFICATIONS_CHANGE -> changeNotifications(data[1] as Int, data[2] as Boolean, data[3] as Int)
            Keys.UPDATE_USER -> {
                val userId = data[1] as Int

                if (loadingIds.indexOf(userId) != -1)
                    loadingIds.remove(userId)

                updateUser(userId)
            }
            Keys.UPDATE_GROUP -> {
                val groupId = data[1] as Int

                if (loadingIds.indexOf(groupId) != -1)
                    loadingIds.remove(groupId)

                updateGroup(groupId)
            }
            Keys.CONNECTED -> {
                if (fragment.isLoading)
                    fragment.isLoading = false

                if (!AppGlobal.preferences.getBoolean(FragmentSettings.KEY_OFFLINE, false))
                    fragment.onRefresh()
            }
            Keys.UPDATE_CHAT -> {
                val chat = data[1] as VKChat
                updateChat(chat)
            }
        }
    }

    private fun updateChat(chat: VKChat) {
        values!!.forEach {
            if (it.peerId > 2_000_000_000 && VKConversation.toChatId(it.peerId) == chat.id) {
                it.apply {
                    photo50 = chat.photo50
                    photo100 = chat.photo100
                    photo200 = chat.photo200
                    title = chat.title
                    membersCount = chat.users.size
                    state = chat.state
                }

                val position = values!!.indexOf(it)
                notifyItemChanged(position, -1)
                return
            }
        }
    }

    private fun changeNotifications(peerId: Int, noSound: Boolean, disabledUntil: Int) {
        val position = findConversationPosition(peerId)
        if (position == -1) return

        val conversation = getItem(position).apply {
            this.isNoSound = noSound
            this.disabledUntil = disabledUntil
            this.isDisabledForever = disabledUntil == -1
        }

        notifyItemChanged(position, -1)

        CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation)
    }

    private fun handleClearFlags(data: Array<Any>) {
        val mId = data[1] as Int
        val flags = data[2] as Int

        if (VKMessage.isUnread(flags))
            readMessage(mId)
    }

    private fun updateMessage(messageId: Int) {
        if (messageId == lastUpdateId) return
        lastUpdateId = messageId

        for (i in 0 until itemCount) {
            val conversation = getItem(i)
            if (conversation.last!!.id == messageId) {
                conversation.last = CacheStorage.getMessage(messageId)
                notifyItemChanged(i, -1)
                break
            }
        }
    }

    private fun updateUser(userId: Int) {
        for (i in 0 until itemCount) {
            val conversation = getItem(i)
            conversation.last ?: continue
            if (conversation.type == VKConversation.Type.USER) {
                if (conversation.peerId == userId) {
                    notifyItemChanged(i, -1)
                    break
                }
            } else if (conversation.isFromUser) {
                if (conversation.last!!.fromId == userId) {
                    notifyItemChanged(i, -1)
                    break
                }
            }
        }
    }

    private fun updateGroup(id: Int) {
        var groupId = id
        if (groupId > 0)
            groupId *= -1
        for (i in 0 until itemCount) {
            val conversation = getItem(i)
            conversation.last ?: continue
            if (conversation.type == VKConversation.Type.GROUP) {
                if (conversation.peerId == groupId) {
                    notifyItemChanged(i, -1)
                    break
                }
            } else if (conversation.isFromGroup) {
                if (conversation.last!!.fromId == groupId) {
                    notifyItemChanged(i, -1)
                    break
                }
            }
        }
    }

    private fun addMessage(conversation: VKConversation) {
        val firstVisiblePosition = manager.findFirstVisibleItemPosition()

        val index = findConversationPosition(conversation.peerId)

        if (index >= 0) {
            val current = getItem(index)

            conversation.apply {
                this.peerId = current.peerId
                this.photo50 = current.photo50
                this.photo100 = current.photo100
                this.photo200 = current.photo200
                this.pinned = current.pinned
                this.title = current.title
                this.isCanWrite = current.isCanWrite
                this.type = current.type
                this.unread = current.unread + 1
                this.isDisabledForever = current.isDisabledForever
                this.disabledUntil = current.disabledUntil
                this.isNoSound = current.isNoSound
                this.isGroupChannel = current.isGroupChannel
                this.isCanChangeInfo = current.isCanChangeInfo
                this.isCanChangeInviteLink = current.isCanChangeInviteLink
                this.isCanChangePin = current.isCanChangePin
                this.isCanInvite = current.isCanInvite
                this.isCanPromoteUsers = current.isCanPromoteUsers
                this.isCanSeeInviteLink = current.isCanSeeInviteLink
                this.membersCount = current.membersCount

                if (last!!.isOut) {
                    unread = 0
                    isRead = false
                }
            }

            if (index > 0) {
                remove(index)
                add(0, conversation)
                notifyItemMoved(index, 0)
                notifyItemRangeChanged(0, itemCount, -1)

                if (firstVisiblePosition <= 1)
                    manager.scrollToPosition(0)
            } else {
                remove(0)
                add(0, conversation)
                notifyItemChanged(0, -1)
            }

            CacheStorage.update(DatabaseHelper.CONVERSATIONS_TABLE, conversation, DatabaseHelper.PEER_ID, conversation.peerId)

            conversation.last ?: return

            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, conversation.last!!, DatabaseHelper.MESSAGE_ID, conversation.last!!.id)
        } else {
            if (!conversation.last!!.isOut)
                conversation.unread = conversation.unread + 1

            add(0, conversation)
            notifyItemInserted(0)
            notifyItemRangeChanged(0, itemCount, -1)

            if (firstVisiblePosition <= 1)
                manager.scrollToPosition(0)

            if (conversation.last!!.action == VKMessage.Action.CREATE) {
                TaskManager.loadConversation(conversation.peerId, true, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val dialog = models[0] as VKConversation
                        addMessage(dialog)

                        CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, dialog)

                        dialog.last ?: return

                        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, dialog.last!!)
                    }

                    override fun onError(e: Exception) {}
                })
            } else {
                CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation)
                conversation.last ?: return
                CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, conversation.last!!)
            }
        }
    }

    private fun readMessage(id: Int) {
        val position = searchMessagePosition(id)

        if (position == -1) return

        val current = getItem(position)
        current.isRead = true
        current.unread = 0

        notifyItemChanged(position, -1)
    }

    private fun editMessage(edited: VKMessage) {
        val position = searchMessagePosition(edited.id)
        if (position == -1) return

        val message = getItem(position).last ?: return
        message.apply {
            this.flags = edited.flags
            this.text = edited.text
            this.updateTime = edited.updateTime
            this.attachments = edited.attachments
        }

        notifyItemChanged(position, -1)
    }

    private fun setUserOnline(online: Boolean, mobile: Boolean, userId: Int, time: Int) {
        for (i in 0 until itemCount) {
            val conversation = getItem(i)
            if (conversation.type != VKConversation.Type.USER) continue

            conversation.last ?: return

            val user = MemoryCache.getUser(conversation.last!!.peerId) ?: return
            if (user.id == userId) {
                user.apply {
                    isOnline = online
                    isOnlineMobile = mobile
                    lastSeen = time.toLong()
                }

                notifyItemChanged(i, -1)
            }
        }
    }

    private fun findConversationPosition(peerId: Int): Int {
        for (i in 0 until itemCount) {
            if (getItem(i).peerId == peerId) return i
        }

        return -1
    }

    private fun searchUser(userId: Int): VKUser {
        val user = MemoryCache.getUser(userId)
        if (user == null) {
            if (!loadingIds.contains(userId)) {
                loadingIds.add(userId)
                EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, userId, javaClass.simpleName))
            }
            return VKUser.EMPTY
        }
        return user
    }

    private fun searchGroup(groupId: Int?): VKGroup {
        val group = MemoryCache.getGroup(groupId!!)
        if (group == null) {
            if (loadingIds.indexOf(groupId) == -1) {
                loadingIds.add(groupId)
                EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, if (groupId < 0) groupId else groupId * -1, javaClass.simpleName))
            }
            return VKGroup.EMPTY
        }
        return group
    }

    private fun searchMessagePosition(mId: Int): Int {
        values!!.forEach {
            if (it.last != null && it.last!!.id == mId) return values!!.indexOf(it)
        }

        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return ViewHolder(getView(parent)!!)
    }

    inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: ImageView
        private var avatarSmall: ImageView
        private var online: ImageView
        private var out: ImageView
        private var muted: ImageView

        private var title: TextView
        private var body: TextView
        private var time: TextView
        private var counter: TextView

        private var container: LinearLayout
        private var counterContainer: FrameLayout

        private var placeholder: Drawable = getDrawable(R.drawable.avatar_placeholder)!!

        @ColorInt
        private var pushesEnabled: Int = 0
        @ColorInt
        private var pushesDisabled: Int = 0
        @ColorInt
        private var accentColor: Int = 0

        init {
            accentColor = ThemeManager.accent
            pushesEnabled = accentColor
            pushesDisabled = if (ThemeManager.isDark) ColorUtil.lightenColor(ThemeManager.primary, 2f) else Color.GRAY

            avatar = v.findViewById(R.id.userAvatar)
            avatarSmall = v.findViewById(R.id.avatar_small)
            online = v.findViewById(R.id.online)
            out = v.findViewById(R.id.icon_out_message)
            muted = v.findViewById(R.id.muted)

            title = v.findViewById(R.id.title)
            body = v.findViewById(R.id.body)
            time = v.findViewById(R.id.date)
            counter = v.findViewById(R.id.counter)

            container = v.findViewById(R.id.container)
            counterContainer = v.findViewById(R.id.counter_container)

            val background = GradientDrawable()
            background.setColor(ThemeManager.accent)
            background.cornerRadius = 200f

            counter.background = background
        }

        override fun bind(position: Int) {
            val item = getItem(position)

            val last = item.last

            muted.visibility = if (item.isNotificationsDisabled) View.VISIBLE else View.GONE

            if (last == null) return

            var fromGroup: VKGroup? = null

            var fromUser: VKUser? = null

            if (item.isFromGroup) {
                fromGroup = searchGroup(VKGroup.toGroupId(last.fromId))
            } else if (item.isFromUser) {
                fromUser = searchUser(last.fromId)
            }

            val peerAvatar = item.photo
            val fromAvatar = VKUtil.getPhoto100(item, fromUser, fromGroup)

            body.text = last.text
            title.text = item.fullTitle

            counter.text = if (item.unread > 0) item.unread.toString() else ""
            time.text = Util.dateFormatter.format(last.date * 1000)

            counter.background.setTint(if (item.isNotificationsDisabled) pushesDisabled else pushesEnabled)
            if (item.isNotificationsDisabled) {
                counter.setTextColor(Color.WHITE)
            } else {
                counter.setTextColor(if (ThemeManager.isDark) Color.DKGRAY else Color.WHITE)
            }

            if (item.isChat || last.isOut) {
                avatarSmall.visibility = View.VISIBLE
            } else {
                avatarSmall.visibility = View.GONE
            }

            (avatarSmall.parent as LinearLayout).gravity = if (avatar.visibility == View.VISIBLE) Gravity.CENTER_VERTICAL else Gravity.START or Gravity.TOP

            if (!TextUtils.isEmpty(peerAvatar)) {
                Picasso.get()
                        .load(peerAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder)
                        .into(avatar)
            } else {
                avatar.setImageDrawable(placeholder)
            }

            if (avatarSmall.visibility == View.VISIBLE)
                if (!TextUtils.isEmpty(fromAvatar)) {
                    Picasso.get()
                            .load(fromAvatar)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(placeholder)
                            .into(avatarSmall)
                } else {
                    avatarSmall.setImageDrawable(placeholder)
                }

            if (last.action == null) {
                if (TextUtils.isEmpty(last.text)) {
                    val body = VKUtil.getAttachmentBody(item.last!!.attachments, item.last!!.fwdMessages)

                    val span = SpannableString(body)
                    span.setSpan(ForegroundColorSpan(accentColor), 0, body.length, 0)

                    this.body.append(span)
                }
            } else {
                val body = VKUtil.getActionBody(last, true)

                val span = SpannableString(body)
                span.setSpan(ForegroundColorSpan(accentColor), 0, body!!.length, 0)

                this.body.text = span
            }

            counter.visibility = if (TextUtils.isEmpty(counter.text.toString())) View.GONE else View.VISIBLE
            out.visibility = if (last.isOut && !item.isRead) View.VISIBLE else View.GONE
            counterContainer.visibility = if (item.isRead) View.GONE else View.VISIBLE

            if (item.type == VKConversation.Type.USER) {
                var user = CacheStorage.getUser(item.peerId)
                if (user == null) user = VKUser.EMPTY

                online.visibility = if (user.isOnline) View.VISIBLE else View.GONE
                online.setImageDrawable(UserAdapter.getOnlineIndicator(context, user))
            } else {
                online.visibility = View.GONE
                online.setImageDrawable(null)
            }
        }
    }
}
