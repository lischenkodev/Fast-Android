package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.MessagesActivity
import ru.melodin.fast.R
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.*
import ru.melodin.fast.api.model.attachment.VKAudio
import ru.melodin.fast.api.model.attachment.VKDoc
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.api.model.attachment.VKVideo
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.AttachmentInflater
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.BoundedLinearLayout
import ru.melodin.fast.view.CircleImageView
import java.io.IOException
import java.util.*

class MessageAdapter(context: Context, messages: ArrayList<VKMessage>, private val peerId: Int) : RecyclerAdapter<VKMessage, MessageAdapter.ViewHolder>(context, R.layout.item_message, messages) {

    private var mediaPlayer: MediaPlayer? = null
    private var playingId = -1

    private val attachmentInflater: AttachmentInflater = AttachmentInflater(this, context)
    private val metrics: DisplayMetrics = context.resources.displayMetrics
    private val activity: MessagesActivity = context as MessagesActivity
    private val layoutManager: LinearLayoutManager = activity.getRecyclerView().layoutManager as LinearLayoutManager

    init {
        EventBus.getDefault().register(this)
    }

    private fun releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    fun destroy() {
        releaseMediaPlayer()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReceive(data: Array<Any>) {
        when (data[0] as String) {
            AttachmentInflater.KEY_PLAY_AUDIO -> {
                val messageId = data[1] as Int

                if (mediaPlayer == null) {
                    releaseMediaPlayer()
                    initMediaPlayer(data[1] as Int, data[2] as String)
                } else {
                    if (mediaPlayer!!.isPlaying) {
                        releaseMediaPlayer()
                        setPlaying(playingId, false)
                        playingId = -1
                        initMediaPlayer(data[1] as Int, data[2] as String)
                    } else {
                        mediaPlayer!!.start()
                        setPlaying(messageId, true)
                    }
                }
            }
            AttachmentInflater.KEY_PAUSE_AUDIO -> if (mediaPlayer != null) {
                mediaPlayer!!.pause()

                val mId = data[1] as Int
                setPlaying(mId, false)
            }
            Keys.KEY_USER_OFFLINE, Keys.KEY_USER_ONLINE -> activity.setUserOnline(data[1] as Int)
            Keys.KEY_MESSAGE_CLEAR_FLAGS -> handleClearFlags(data)
            Keys.KEY_MESSAGE_SET_FLAGS -> handleSetFlags(data)
            Keys.KEY_MESSAGE_NEW -> {
                val conversation = data[1] as VKConversation
                conversation.last ?: return

                if (peerId != conversation.last?.peerId) return

                val last = conversation.last

                if (last!!.action == VKMessage.Action.PIN_MESSAGE) {
                    val position = findPosition(last.actionId)
                    if (position != -1)
                        activity.showPinned(getItem(position))
                } else if (last.action == VKMessage.Action.UNPIN_MESSAGE) {
                    activity.showPinned(null)
                }

                addMessage(last)
                notifyDataSetChanged()

                val lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()

                if (lastVisibleItem >= itemCount - 4) {
                    activity.getRecyclerView().scrollToPosition(itemCount - 1)
                }

                if (!last.isOut && last.peerId == peerId && !AppGlobal.preferences.getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
                    if (!activity.isRunning()) {
                        activity.setNotRead(last)
                    } else {
                        readNewMessage(last)
                    }
                }
            }
            Keys.KEY_MESSAGE_EDIT -> editMessage(data[1] as VKMessage)
            Keys.UPDATE_MESSAGE -> updateMessage(data[1] as Int)
            Keys.CONNECTED -> {
            }
            Keys.UPDATE_GROUP -> updateGroup(data[1] as Int)
            Keys.UPDATE_USER -> updateUser(data[1] as Int)
        }
    }

    private fun initMediaPlayer(messageId: Int, url: String) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setDataSource(url)
            mediaPlayer!!.setOnCompletionListener {
                releaseMediaPlayer()
                EventBus.getDefault().postSticky(arrayOf<Any>(AttachmentInflater.KEY_STOP_AUDIO))
                setPlaying(messageId, false)
            }
            mediaPlayer!!.setOnErrorListener { _, _, _ ->
                playingId = -1
                releaseMediaPlayer()
                setPlaying(messageId, false)
                false
            }
            mediaPlayer!!.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.start()
                playingId = messageId
                setPlaying(messageId, true)
            }
            mediaPlayer!!.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun setPlaying(messageId: Int, playing: Boolean) {
        val position = findPosition(messageId)
        if (position == -1) return

        val message = getItem(position)
        message.isPlaying = playing
        notifyItemChanged(position, -1)
    }

    private fun findPosition(mId: Int): Int {
        for (i in 0 until values!!.size) {
            val message = getItem(i)
            if (message.id == mId)
                return i
        }

        return -1
    }

    fun readNewMessage(message: VKMessage) {
        if (message.isOut || layoutManager.findLastCompletelyVisibleItemPosition() < searchPosition(message.id))
            return
        TaskManager.execute {
            VKApi.messages()
                    .markAsRead()
                    .messageIds(message.id)
                    .execute(Int::class.java, object : VKApi.OnResponseListener {
                        override fun onSuccess(models: ArrayList<*>?) {
                            readMessage(message.id)
                        }

                        override fun onError(e: Exception) {
                        }
                    })
        }
    }

    private fun handleClearFlags(data: Array<Any>) {
        val mId = data[1] as Int
        val flags = data[2] as Int
        val peerId = data[3] as Int

        if (peerId != this.peerId) return

        if (VKMessage.isImportant(flags))
            importantMessage(false, mId)

        if (VKMessage.isUnread(flags))
            readMessage(mId)
    }

    private fun handleSetFlags(data: Array<Any>) {
        val mId = data[1] as Int
        val flags = data[2] as Int
        val peerId = data[3] as Int

        if (peerId != this.peerId) return

        if (VKMessage.isImportant(flags))
            importantMessage(true, mId)

        if (VKMessage.isUnread(flags))
            readMessage(mId)
    }

    operator fun contains(id: Int): Boolean {
        return searchPosition(id) != -1
    }

    private fun importantMessage(important: Boolean, mId: Int) {
        val position = searchPosition(mId)
        if (position == -1) return

        val message = getItem(position)

        message.isImportant = important
        notifyItemChanged(position, -1)
    }

    private fun updateMessage(messageId: Int) {
        val position = searchPosition(messageId)
        if (position == -1) return

        remove(position)
        add(position, CacheStorage.getMessage(messageId)!!)
        notifyDataSetChanged()
    }

    private fun editMessage(edited: VKMessage) {
        val position = searchPosition(edited.id)
        if (position == -1) return

        val message = getItem(position)
        message.text = edited.text
        message.flags = edited.flags
        message.attachments = edited.attachments
        message.updateTime = edited.updateTime

        notifyItemChanged(position, -1)
    }

    fun addMessage(msg: VKMessage) {
        if (msg.randomId != 0 && containsRandom(msg.randomId.toLong()) || contains(msg.id))
            return

        add(msg)
    }

    private fun containsRandom(randomId: Long): Boolean {
        for (message in values!!)
            if (message.randomId.toLong() == randomId)
                return true
        return false
    }

    private fun readMessage(mId: Int) {
        for (i in 0 until itemCount) {
            val message = getItem(i)
            if (message.id == mId) {
                notifyItemChanged(i, -1)
                break
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (values!!.size == position) {
            TYPE_FOOTER
        } else {
            TYPE_NORMAL
        }
    }

    override fun getItem(position: Int): VKMessage {
        return if (getItemViewType(position) == TYPE_FOOTER) {
            super.getItem(position - 1)
        } else super.getItem(position)
    }

    fun searchPosition(mId: Int): Int {
        for (i in 0 until itemCount)
            if (getItem(i).id == mId) return i

        return -1
    }

    private fun searchUser(id: Int): VKUser {
        val user = CacheStorage.getUser(id)

        if (user == null) {
            EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, id, javaClass.simpleName))
            return VKUser.EMPTY
        }
        return user
    }

    private fun searchGroup(id: Int): VKGroup {
        val group = CacheStorage.getGroup(if (id < 0) id * -1 else id)

        if (group == null) {
            EventBus.getDefault().postSticky(arrayOf(Keys.NEED_LOAD_ID, if (id < 0) id else id * -1, javaClass.simpleName))
            return VKGroup.EMPTY
        }

        return group
    }

    private fun updateUser(userId: Int) {
        for (i in 0 until itemCount) {
            val message = getItem(i)
            if (message.fromId == userId) {
                notifyItemChanged(i, -1)
                break
            }
        }
    }

    private fun updateGroup(groupId: Int) {
        for (i in 0 until itemCount) {
            val message = getItem(i)
            if (message.fromId == groupId) {
                notifyItemChanged(i, -1)
                break
            }
        }
    }


    private fun onAvatarLongClick(position: Int) {
        val user = CacheStorage.getUser(getItem(position).fromId) ?: return

        Toast.makeText(context, user.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun createFooter(): View {
        val footer = View(context)
        footer.visibility = View.VISIBLE
        footer.setBackgroundColor(Color.TRANSPARENT)
        footer.visibility = View.INVISIBLE
        footer.isEnabled = false
        footer.isClickable = false
        footer.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px(66f).toInt())

        return footer
    }

    private fun inflateAttachments(item: VKMessage, parent: ViewGroup, bubble: BoundedLinearLayout) {
        val background = bubble.background
        val attachments = item.attachments
        for (i in attachments.indices) {
            bubble.background = background
            bubble.visibility = View.VISIBLE
            parent.visibility = View.VISIBLE

            when (val attachment = attachments[i]) {
                is VKAudio -> attachmentInflater.audio(item, parent, attachment)
                is VKPhoto -> {
                    if (TextUtils.isEmpty(item.text))
                        bubble.setBackgroundColor(Color.TRANSPARENT)

                    attachmentInflater.photo(item, parent, attachment, -1)
                }
                is VKSticker -> {
                    bubble.setBackgroundColor(Color.TRANSPARENT)
                    attachmentInflater.sticker(item, parent, attachment)
                }
                is VKDoc -> attachmentInflater.doc(item, parent, attachment)
                is VKLink -> attachmentInflater.link(item, parent, attachment)
                is VKVideo -> {
                    if (TextUtils.isEmpty(item.text))
                        bubble.setBackgroundColor(Color.TRANSPARENT)

                    attachmentInflater.video(item, parent, attachment, -1)
                }
                is VKGraffiti -> {
                    bubble.setBackgroundColor(Color.TRANSPARENT)
                    attachmentInflater.graffiti(item, parent, attachment)
                }
                is VKVoice -> attachmentInflater.voice(item, parent, attachment)
                is VKWall -> attachmentInflater.wall(item, parent, attachment)
                else -> attachmentInflater.empty(parent, context.getString(R.string.unknown))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return when (viewType) {
            TYPE_FOOTER -> FooterViewHolder(createFooter())
            TYPE_NORMAL -> ViewHolder(getView(parent)!!)
            else -> FooterViewHolder(createFooter())
        }
    }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {
        if (holder is FooterViewHolder) return
        holder.bind(position)

        super.onBindViewHolder(holder, position)
    }

    internal inner class FooterViewHolder(v: View) : RecyclerHolder(v) {

        override fun bind(position: Int) {}
    }

    open inner class ViewHolder(v: View) : RecyclerHolder(v) {

        private var avatar: CircleImageView
        private var indicator: ImageView
        private var important: ImageView

        private var text: TextView

        private var mainContainer: LinearLayout

        private var bubble: BoundedLinearLayout
        private var attachments: LinearLayout
        private var serviceContainer: LinearLayout
        private var messageContainer: LinearLayout
        private var timeContainer: LinearLayout
        private var bubbleContainer: LinearLayout
        private var replyContainer: LinearLayout

        private var sending: Drawable? = null
        private var error: Drawable? = null
        private var placeholder: Drawable? = null
        private var defaultBg: Drawable? = null
        private var circle: GradientDrawable? = null

        @ColorInt
        var alphaAccentColor: Int = 0

        val isFooter = false

        init {
            alphaAccentColor = ColorUtil.alphaColor(ThemeManager.accent, 0.3f)

            text = v.findViewById(R.id.message)

            placeholder = getDrawable(R.drawable.avatar_placeholder)

            avatar = itemView.findViewById(R.id.userAvatar)
            important = itemView.findViewById(R.id.important)

            circle = null
            defaultBg = getDrawable(R.drawable.msg_bg)
            error = getDrawable(R.drawable.ic_error_black_24dp)
            sending = getDrawable(R.drawable.ic_access_time_black_24dp)

            sending!!.setTint(ThemeManager.accent)
            error!!.setTint(Color.RED)

            bubbleContainer = itemView.findViewById(R.id.bubble_container)
            messageContainer = itemView.findViewById(R.id.message_container)
            serviceContainer = itemView.findViewById(R.id.service_container)
            mainContainer = itemView.findViewById(R.id.root)
            replyContainer = itemView.findViewById(R.id.reply_container)
            bubble = itemView.findViewById(R.id.bubble)
            attachments = itemView.findViewById(R.id.attachments)
            timeContainer = itemView.findViewById(R.id.time_container)

            indicator = itemView.findViewById(R.id.message_state)

            bubble.maxWidth = metrics.widthPixels - metrics.widthPixels / 3
        }

        override fun bind(position: Int) {
            val item = getItem(position)

            val isUser = item.fromId > 0

            var user: VKUser? = null
            var group: VKGroup? = null

            val fromId = item.fromId

            if (isUser) {
                user = searchUser(fromId)
            } else {
                group = searchGroup(fromId)
            }

            itemView.setBackgroundColor(if (item.isSelected) alphaAccentColor else 0)

            //String time_ = item.getUpdateTime() > 0 ? getString(R.string.edited) + ", " : "" + Util.dateFormatter.format(item.isAdded() ? item.getDate() : item.getDate() * 1000L);
            //.setTimeText(time_);

            val gravity = if (item.isOut) Gravity.END else Gravity.START

            timeContainer.gravity = gravity
            bubbleContainer.gravity = gravity

            important.visibility = if (item.isImportant) View.VISIBLE else View.GONE
            indicator.visibility = if (!item.isOut) View.GONE else View.VISIBLE

            if (indicator.visibility == View.VISIBLE) {
                if (item.status == VKMessage.Status.SENT && !item.isRead) {
                    indicator.setImageDrawable(circle)
                } else if (item.status == VKMessage.Status.ERROR) {
                    indicator.setImageDrawable(error)
                } else if (item.status == VKMessage.Status.SENDING) {
                    indicator.setImageDrawable(sending)
                } else {
                    indicator.setImageDrawable(null)
                }
            }

            val avatarSrc = if (!isUser) group!!.photo100 else user!!.photo100

            avatar.visibility = if (item.isOut || !item.isChat) View.GONE else View.VISIBLE

            if (avatar.visibility == View.VISIBLE) {
                if (TextUtils.isEmpty(avatarSrc)) {
                    avatar.setImageDrawable(placeholder)
                } else {
                    Picasso.get()
                            .load(avatarSrc)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(placeholder!!)
                            .into(avatar)
                }
            } else {
                avatar.setImageDrawable(null)
            }

            avatar.setOnLongClickListener {
                if (item.isChat) {
                    onAvatarLongClick(position)
                    true
                } else {
                    false
                }
            }

            messageContainer.gravity = if (item.isOut) Gravity.END else Gravity.START

            if (TextUtils.isEmpty(item.text)) {
                text.text = ""
                text.visibility = View.GONE
            } else {
                text.visibility = View.VISIBLE
                val text = item.text!!.trim()
                this.text.text = text
            }

            val textColor: Int = if (ThemeManager.isDark) {
                -0x111112
            } else {
                -0xe2e2e3
            }
            val linkColor: Int = ThemeManager.accent

            text.setTextColor(textColor)
            text.setLinkTextColor(linkColor)

            if (item.action != null) {
                messageContainer.visibility = View.GONE
                serviceContainer.visibility = View.VISIBLE

                serviceContainer.removeAllViews()
                attachmentInflater.service(item, serviceContainer)
                bubble.background = null
            } else {
                messageContainer.visibility = View.VISIBLE
                serviceContainer.visibility = View.GONE
                bubble.background = defaultBg
            }

            if (!ArrayUtil.isEmpty(item.fwdMessages) || !ArrayUtil.isEmpty(item.attachments)) {
                attachments.visibility = View.VISIBLE
                attachments.removeAllViews()
            } else {
                attachments.visibility = View.GONE
            }

            if (!ArrayUtil.isEmpty(item.attachments)) {
                inflateAttachments(item, attachments, bubble)
            }

            if (!ArrayUtil.isEmpty(item.fwdMessages)) {
                attachmentInflater.showForwardedMessages(item, attachments, true)
            }

            if (item.reply != null) {
                replyContainer.visibility = View.VISIBLE
                replyContainer.removeAllViews()
                attachmentInflater.reply(item, replyContainer, true)
            } else {
                replyContainer.visibility = View.GONE
            }
        }
    }

    fun getActivity(): MessagesActivity {
        return context as MessagesActivity
    }

    companion object {

        private const val TYPE_NORMAL = 0
        private const val TYPE_FOOTER = 10

    }
}
