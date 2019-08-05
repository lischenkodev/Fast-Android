package ru.melodin.fast.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_messages.*
import kotlinx.android.synthetic.main.toolbar_action.*
import ru.melodin.fast.BuildConfig
import ru.melodin.fast.MainActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.MessageAdapter
import ru.melodin.fast.adapter.PopupAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnCompleteListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.VKUtil
import ru.melodin.fast.api.method.MethodSetter
import ru.melodin.fast.api.model.*
import ru.melodin.fast.common.*
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.model.ListItem
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.StringUtils
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*
import kotlin.collections.ArrayList

class FragmentMessages : BaseFragment(), RecyclerAdapter.OnItemClickListener,
    RecyclerAdapter.OnItemLongClickListener, TextWatcher, Toolbar.OnMenuItemClickListener {

    var isLoading: Boolean = false

    private var resumed: Boolean = false
    private var editing: Boolean = false
    private var canWrite: Boolean = false
    private var typing: Boolean = false
    private var notRead: VKMessage? = null

    private val random = Random()

    private var iconSend: Drawable? = null
    private var iconMic: Drawable? = null
    private var iconDone: Drawable? = null
    private var iconTrash: Drawable? = null

    private var adapter: MessageAdapter? = null

    private var cantWriteReason = -1
    private var peerId: Int = 0
    private var membersCount: Int = 0

    private var photo: String? = null

    private var reason: VKConversation.Reason? = null

    private var messageText: String? = null
    private var chatTitle: String? = null

    private var timer: Timer? = null
    private var selectTimer: Timer? = null

    private var pinned: VKMessage? = null
    private var edited: VKMessage? = null

    private var editingPosition: Int = 0

    private var conversation: VKConversation? = null

    private lateinit var layoutManager: LinearLayoutManager

    private var popupWindow: PopupWindow? = null
    private var popupAdapter: PopupAdapter? = null

    private val sendClick = View.OnClickListener {
        val s = message.text!!.toString()
        if (s.trim().isNotEmpty()) {
            messageText = s

            sendMessage()
            message.setText("")
        }
    }
    private val recordClick: View.OnClickListener? = null
    private val doneClick = View.OnClickListener {
        ViewUtil.hideKeyboard(message)
        editMessage(edited!!)
    }

    private val subtitle: String?
        get() {
            if (conversation == null) return null

            if (conversation!!.last != null && conversation!!.isChat && conversation!!.state != VKConversation.State.IN) {
                val kicked = conversation!!.state == VKConversation.State.KICKED

                return getString(if (kicked) R.string.kicked_out_text else R.string.leave_from_chat_text)
            } else
                return when (conversation!!.type) {
                    null -> null
                    VKConversation.Type.USER -> {
                        val currentUser = CacheStorage.getUser(peerId)
                        if (currentUser == null)
                            loadUser(peerId)
                        getUserSubtitle(currentUser)
                    }
                    VKConversation.Type.CHAT -> if (conversation!!.isGroupChannel) {
                        getString(R.string.channel) + " • " + getString(
                            R.string.members_count,
                            membersCount
                        )
                    } else {
                        if (membersCount > 0) resources.getQuantityString(
                            R.plurals.members,
                            membersCount,
                            membersCount
                        ) else ""
                    }
                    VKConversation.Type.GROUP -> if (BuildConfig.DEBUG) getString(R.string.group) else ""
                }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = tb
        recyclerList = recyclerView

        iconSend = ContextCompat.getDrawable(activity!!, R.drawable.md_send)
        iconMic = ContextCompat.getDrawable(activity!!, R.drawable.md_mic)
        iconDone = ContextCompat.getDrawable(activity!!, R.drawable.md_done)
        iconTrash = ContextCompat.getDrawable(activity!!, R.drawable.ic_trash)

        getIntentData()
        updateToolbar()
        showPinned(pinned)
        getConversation(peerId)
        checkCanWrite()

        tb.setTitle(chatTitle)
        tb.setBackVisible(true)

        actionTb.navigationIcon!!.setTint(ThemeManager.main)
        actionTb.setNavigationOnClickListener { onBackPressed() }
        actionTb.inflateMenu(R.menu.activity_chat_history)
        actionTb.setOnMenuItemClickListener(this)
        onPrepareMenu()
        for (i in 0 until actionTb.menu.size()) {
            val item = actionTb.menu[i]
            item.icon?.setTint(ThemeManager.main)
        }

        scrollToBottom.setOnClickListener {
            scrollToBottom.hide()
            if (adapter != null)
                recyclerView.smoothScrollToPosition(adapter!!.lastPosition + 1)
        }

        initListScrollListener()

        layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)
        layoutManager.stackFromEnd = true

        recyclerView.layoutManager = layoutManager

        if (VKConversation.isChatId(peerId))
            tb.setOnAvatarClickListener { openChatInfo() }

        initPopupWindow()
        loadAvatar()

        message.addTextChangedListener(this)

        smiles.setOnLongClickListener {
            val template = AppGlobal.preferences.getString(
                FragmentSettings.KEY_MESSAGE_TEMPLATE,
                FragmentSettings.DEFAULT_TEMPLATE_VALUE
            )
            if (StringUtils.isEmpty(message!!.text)) {
                message.setText(template)
            } else {
                message.append(template)
            }
            message.setSelection(message.text!!.length)
            true
        }

        getCachedHistory()
        if (savedInstanceState == null)
            getHistory(0, MESSAGES_COUNT)
    }

    @SuppressLint("InflateParams")
    private fun initPopupWindow() {
        val view = layoutInflater.inflate(R.layout.activity_messages_popup, null, false)

        val list = view.findViewById<RecyclerView>(R.id.list)

        list.layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)

        popupAdapter = PopupAdapter(activity!!, createItems())
        popupAdapter!!.setOnItemClickListener(object : RecyclerAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val item = popupAdapter!!.getItem(position)
                when (item.id) {
                    PopupAdapter.ID_CLEAR_DIALOG -> showConfirmDeleteConversation()
                    PopupAdapter.ID_NOTIFICATIONS -> toggleNotifications()
                    PopupAdapter.ID_LEAVE -> toggleChatState()
                    PopupAdapter.ID_CHAT_INFO -> openChatInfo()
                }

                popupWindow!!.dismiss()
            }
        })

        list.adapter = popupAdapter

        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow!!.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        popupWindow!!.width = resources.displayMetrics.widthPixels

        tb.setOnClickListener {
            popupWindow!!.showAsDropDown(tb)
        }
    }

    private fun createItems(): ArrayList<ListItem> {
        val chatInfo = ListItem(
            PopupAdapter.ID_CHAT_INFO,
            getString(R.string.chat_info),
            drawable(R.drawable.ic_info_black_24dp)
        )
        val disableNotifications = ListItem(
            PopupAdapter.ID_NOTIFICATIONS,
            getString(R.string.disable_notifications),
            drawable(R.drawable.ic_volume_off_black_24dp)
        )
        val clear = ListItem(
            PopupAdapter.ID_CLEAR_DIALOG,
            getString(R.string.clear_messages_history),
            drawable(R.drawable.ic_trash)
        )
        val left = ListItem(PopupAdapter.ID_LEAVE, "", ColorDrawable(Color.TRANSPARENT))

        val items = ArrayList(listOf(chatInfo, disableNotifications, clear, left))

        for (item in items)
            updatePopupItemById(item)

        val removeItems = ArrayList<ListItem>()
        if (conversation == null || conversation!!.type != VKConversation.Type.CHAT) {
            removeItems.add(left)
            removeItems.add(chatInfo)
        }

        if (conversation != null && conversation!!.isGroupChannel) {
            removeItems.add(chatInfo)
        }

        items.removeAll(removeItems)
        return items
    }

    private fun updatePopupItemById(item: ListItem) {
        when (item.id) {
            PopupAdapter.ID_NOTIFICATIONS -> if (conversation != null && conversation!!.isNotificationsDisabled) {
                item.title = getString(R.string.enable_notifications)
                item.icon = drawable(R.drawable.ic_volume_full_black_24dp)
            } else {
                item.title = getString(R.string.disable_notifications)
                item.icon = drawable(R.drawable.ic_volume_off_black_24dp)
            }
            PopupAdapter.ID_LEAVE -> {
                item.isVisible =
                    conversation != null && conversation!!.type == VKConversation.Type.CHAT

                if (item.isVisible) {
                    if (conversation!!.state == VKConversation.State.LEFT) {
                        item.title =
                            getString(if (conversation!!.isGroupChannel) R.string.subscribe_to_channel else R.string.return_to_chat)
                        item.icon =
                            drawable(if (conversation!!.isGroupChannel) R.drawable.md_add else R.drawable.ic_keyboard_return_black_24dp)
                    } else if (conversation!!.state == VKConversation.State.IN) {
                        item.title =
                            getString(if (conversation!!.isGroupChannel) R.string.unsubscribe_from_channel else R.string.leave_from_chat)
                        item.icon = drawable(R.drawable.md_clear)
                    }
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            android.R.id.home -> {
                adapter!!.clearSelected()
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                updateToolbar()
            }
            R.id.forward -> confirmForward(adapter!!.selectedMessages)
            R.id.delete -> showConfirmDeleteMessages(adapter!!.selectedMessages)
            R.id.reply -> confirmReply(adapter!!.selectedMessages)
        }

        return true
    }

    private fun loadAvatar() {
        tb.setAvatar(R.drawable.avatar_placeholder)

        if (photo != null && photo!!.trim().isNotEmpty() && Util.hasConnection()) {
            TaskManager.execute {
                activity!!.runOnUiThread {
                    Picasso.get()
                        .load(photo)
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(tb.avatar)
                }
            }
        } else {
            tb.setAvatar(R.drawable.avatar_placeholder)
        }
    }

    private fun toggleProgress() {
        if (progress.visibility == View.VISIBLE) {
            progress.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }

    private fun initListScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    scrollToBottom.hide()
                } else {
                    if (adapter != null && layoutManager.findLastVisibleItemPosition() < adapter!!.itemCount - 10 && !scrollToBottom.isShown)
                        scrollToBottom.show()
                }

                if (dy < 0) {
                    if (message.isFocused && AppGlobal.preferences.getBoolean(
                            FragmentSettings.KEY_HIDE_KEYBOARD_ON_SCROLL,
                            true
                        )
                    )
                        ViewUtil.hideKeyboard(message)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (adapter != null && newState != RecyclerView.SCROLL_STATE_SETTLING && layoutManager.findLastVisibleItemPosition() >= adapter!!.itemCount - 10) {
                    scrollToBottom.hide()
                }
            }
        })
    }

    private fun getConversation(peerId: Int) {
        if (!Util.hasConnection()) return
        TaskManager.loadConversation(peerId, true, object : OnCompleteListener {
            override fun onComplete(models: ArrayList<*>?) {
                if (ArrayUtil.isEmpty(models)) return
                models ?: return

                conversation = models[0] as VKConversation

                if (VKConversation.isChatId(peerId)) {
                    tb.setTitle(conversation!!.title)
                    updateToolbar()
                }

                popupAdapter!!.changeItems(createItems())
                getMessage(conversation!!.lastMessageId)
            }

            override fun onError(e: Exception) {}
        })
    }

    private fun getMessage(messageId: Int) {
        if (messageId == 0) return

        TaskManager.loadMessage(messageId, true, object : OnCompleteListener {
            override fun onComplete(models: ArrayList<*>?) {
                if (!ArrayUtil.isEmpty(models)) {
                    val message = models!![0] as VKMessage

                    conversation!!.last = message
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, message)
                    CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation!!)
                }
            }

            override fun onError(e: Exception) {}
        })
    }

    override fun onPause() {
        super.onPause()
        resumed = false
    }

    override fun onResume() {
        super.onResume()

        resumed = true
        if (notRead != null) {
            adapter!!.readNewMessage(notRead!!)
            notRead = null
        }

        (activity!! as MainActivity).hideBottomView()
    }

    fun setUserOnline(userId: Int) {
        if (peerId == userId)
            updateToolbar()
    }

    private fun getIntentData() {
        conversation = arguments!!.getSerializable("conversation") as VKConversation?
        chatTitle = arguments!!.getString("title")
        peerId = arguments!!.getInt("peer_id", -1)
        photo = arguments!!.getString("photo")
        cantWriteReason = arguments!!.getInt("reason", -1)
        canWrite = arguments!!.getBoolean("can_write", true)
        reason = VKConversation.getReason(cantWriteReason)

        if (conversation != null) {
            membersCount = conversation!!.membersCount
            pinned = conversation!!.pinned
        } else {
            membersCount = arguments!!.getInt("members_count", -1)
        }
    }

    private fun openChatInfo() {
        FragmentSelector.selectFragment(
            fragmentManager!!,
            FragmentChatInfo(),
            arguments!!.apply { putSerializable("conversation", conversation) },
            true
        )
    }

    private fun checkCanWrite() {
        send.isEnabled = true
        smiles.isEnabled = true
        message.isEnabled = true
        message.setText("")

        if (cantWriteReason <= 0) return
        if (!canWrite) {
            chatPanel.isEnabled = false
            send.isEnabled = false
            smiles.isEnabled = false
            message.isEnabled = false
            message.setText(VKUtil.getErrorReason(reason!!))
        }
    }

    fun chooseMessage(message: VKMessage?) {
        if (adapter == null) return
        if (adapter!!.contains(message!!.id)) {
            val position = adapter!!.searchPosition(message.id)

            recyclerView.smoothScrollToPosition(position)
            adapter!!.clearSelected()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
            adapter!!.selectItem(position)

            if (selectTimer != null) selectTimer!!.cancel()

            selectTimer = Timer()
            selectTimer?.schedule(object : TimerTask() {
                override fun run() {
                    activity?.runOnUiThread {
                        if (adapter?.isSelected(position)!!)
                            adapter!!.unSelectItem(position)

                        selectTimer = null
                    }
                }
            }, 3500)
        } else {
            showPinnedAlert(message)
        }
    }

    fun showPinned(pinned: VKMessage?) {
        if (pinned == null) {
            pinnedContainer.visibility = View.GONE
            return
        }

        pinnedContainer.visibility = View.VISIBLE

        pinnedContainer.setOnClickListener { chooseMessage(pinned) }

        var user = CacheStorage.getUser(pinned.fromId)
        if (user == null) user = VKUser.EMPTY

        pinnedName.text = user.toString().trim()
        pinnedDate.text = Util.dateFormatter.format(pinned.date * 1000L)

        pinnedText.text = pinned.text

        unpin.setOnClickListener { showConfirmUnpinMessage() }

        if (TextUtils.isEmpty(pinned.text) && !ArrayUtil.isEmpty(pinned.attachments)) {
            val body = VKUtil.getAttachmentBody(pinned.attachments, pinned.fwdMessages)

            val r = "<b>$body</b>"
            val span = SpannableString(HtmlCompat.fromHtml(r, HtmlCompat.FROM_HTML_MODE_LEGACY))
            span.setSpan(ForegroundColorSpan(color(R.color.accent)), 0, body.length, 0)

            pinnedText.append(span)
        }
    }

    private fun showPinnedAlert(pinned: VKMessage) {
        val adb = AlertDialog.Builder(activity!!)

        val v = AttachmentInflater.getInstance(null, activity!!)
            .message(null, null, pinned, isReply = false, withStyles = false)
        adb.setView(v)
        adb.setPositiveButton(android.R.string.ok, null)
        adb.show()
    }

    private fun showConfirmUnpinMessage() {
        if (!conversation!!.isCanChangePin) {
            pinnedContainer.visibility = View.GONE
            return
        }

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ -> unpinMessage() }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun unpinMessage() {
        TaskManager.execute {
            VKApi.messages().unpin().peerId(peerId).execute(null, object : OnCompleteListener {
                override fun onComplete(models: ArrayList<*>?) {
                    pinned = null
                    conversation!!.pinned = null
                    showPinned(null)

                    CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation!!)
                }

                override fun onError(e: Exception) {
                    Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun pinMessage(message: VKMessage) {
        TaskManager.execute {
            VKApi.messages().pin().messageId(message.id).peerId(peerId)
                .execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        pinned = message
                        showPinned(pinned)

                        conversation!!.pinned = pinned

                        CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation!!)
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setTyping() {
        if (typing || AppGlobal.preferences.getBoolean(FragmentSettings.KEY_HIDE_TYPING, false))
            return

        typing = true
        TaskManager.execute {
            VKApi.messages().setActivity().type(true).peerId(peerId)
                .execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        timer = Timer()
                        timer!!.schedule(object : TimerTask() {

                            override fun run() {
                                typing = false
                            }
                        }, 10000)
                    }

                    override fun onError(e: Exception) {

                    }
                })
        }
    }

    private fun sendMessage() {
        if (messageText!!.trim().isEmpty()) return

        val message = VKMessage()
        message.text = messageText!!.trim()
        message.fromId = UserConfig.userId
        message.peerId = peerId
        message.isAdded = true
        message.date = Calendar.getInstance().timeInMillis
        message.isOut = true
        message.randomId = random.nextInt()
        message.status = VKMessage.Status.SENDING

        adapter!!.addMessage(message)

        val position = adapter!!.lastPosition

        val msg = adapter!!.getItem(position)

        adapter!!.notifyDataSetChanged()
        recyclerView.scrollToPosition(position + 1)

        val size = adapter!!.itemCount

        TaskManager.execute {
            var id: Int

            if (!Util.hasConnection()) {
                try {
                    throw RuntimeException("No internet")
                } catch (ignored: Exception) {
                }
            }

            TaskManager.sendMessage(
                VKApi.messages().send().randomId(msg.randomId).message(
                    messageText!!.trim()
                ).peerId(peerId), object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return
                        id = models[0] as Int

                        if (typing) {
                            typing = false
                            timer?.cancel()
                        }

                        msg.id = id
                        msg.status = VKMessage.Status.SENT

                        val messagePosition = adapter!!.searchPosition(id)

                        adapter!!.notifyDataSetChanged()

                        if (adapter!!.itemCount > size) {
                            adapter!!.remove(messagePosition)
                            adapter!!.add(msg)
                            adapter!!.notifyItemMoved(messagePosition, adapter!!.lastPosition)
                            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                        }
                    }

                    override fun onError(e: Exception) {
                        Log.e("Error send message", Log.getStackTraceString(e))
                        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()

                        msg.status = VKMessage.Status.ERROR
                        adapter!!.notifyDataSetChanged()
                    }
                })
        }
    }

    private fun createAdapter(messages: ArrayList<VKMessage>, offset: Int) {
        if (ArrayUtil.isEmpty(messages)) {
            return
        }

        if (adapter == null) {
            adapter = MessageAdapter(this, messages, peerId)
            adapter!!.setOnItemClickListener(this)
            adapter!!.setOnItemLongClickListener(this)
            recyclerView.adapter = adapter

            recyclerView.scrollToPosition(adapter!!.itemCount + 1)
            return
        }

        if (offset > 0) {
            adapter!!.values!!.addAll(messages)
            adapter!!.notifyItemRangeInserted(
                adapter!!.values!!.size - messages.size - 1,
                messages.size
            )
            return
        }

        adapter!!.changeItems(messages)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)

        recyclerView.scrollToPosition(adapter!!.itemCount - 1)
    }

    private fun getCachedHistory() {
        val messages = CacheStorage.getMessages(peerId)
        createAdapter(messages, 0)

        if (!ArrayUtil.isEmpty(messages)) {
            toggleProgress()
        }
    }

    private fun getHistory(offset: Int, count: Int) {
        if (isLoading) return
        if (!Util.hasConnection()) {
            Toast.makeText(activity!!, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        if (adapter == null || adapter!!.isEmpty)
            tb.setSubtitle(R.string.loading)

        TaskManager.execute {
            var messages: ArrayList<VKMessage>

            VKApi.messages().history
                .peerId(peerId)
                .extended(true)
                .fields(VKUser.FIELDS_DEFAULT)
                .offset(offset)
                .count(count)
                .execute(VKMessage::class.java, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return

                        messages = models as ArrayList<VKMessage>
                        messages.reverse()

                        val users = VKMessage.users
                        val groups = VKMessage.groups

                        if (!ArrayUtil.isEmpty(users)) {
                            CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                        }

                        if (!ArrayUtil.isEmpty(groups)) {
                            CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups)
                        }

                        if (!ArrayUtil.isEmpty(messages)) {
                            CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)
                        }

                        isLoading = messages.isEmpty()

                        createAdapter(messages, offset)

                        if (progress.visibility == View.VISIBLE)
                            toggleProgress()

                        isLoading = false
                        updateToolbar()
                    }

                    override fun onError(e: Exception) {
                        isLoading = false
                        updateToolbar()
                        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                })

        }
    }

    fun updateChat(chat: VKChat) {
        chatTitle = chat.title
        photo = chat.photo200
        membersCount = chat.users.size

        conversation ?: return
        conversation!!.apply {
            title = chat.title
            photo50 = chat.photo50
            photo100 = chat.photo100
            photo200 = chat.photo200
        }

        tb.setTitle(chatTitle)
        updateToolbar()
        loadAvatar()
    }

    fun updateToolbar() {
        onPrepareMenu()

        tb.setSubtitle(if (isLoading) getString(R.string.loading) else subtitle)

        if (!editing && adapter != null && !adapter!!.isSelected && pinned != null && conversation!!.isCanChangePin)
            pinnedContainer.visibility = View.VISIBLE
    }

    private fun getUserSubtitle(user: VKUser?): String {
        if (user == null) return ""
        return when {
            user.isOnline -> getString(if (user.isOnlineMobile) R.string.online_mobile else R.string.online)
            else -> getString(
                if (user.sex == VKUser.Sex.MALE) R.string.last_seen_m else R.string.last_seen_w,
                Util.dateFormatter.format(user.lastSeen * 1000)
            )
        }
    }

    private fun loadUser(id: Int) {
        TaskManager.loadUser(id, object : OnCompleteListener {
            override fun onComplete(models: ArrayList<*>?) {
                updateToolbar()
            }

            override fun onError(e: Exception) {}
        })
    }

    private fun confirmReply(messages: ArrayList<VKMessage>) {
        if (messages.size == 1) {
            val replyId = messages[0].id

            val text = AppCompatEditText(activity!!)
            text.hint = getString(R.string.message)

            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(R.string.reply)
            builder.setView(text)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(R.string.send) { _, _ ->
                replyMessage(replyId, text.text.toString().trim())
            }
            builder.show()
        } else {
            val text = AppCompatEditText(activity!!)
            text.hint = getString(R.string.message)

            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(R.string.reply)
            builder.setView(text)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(R.string.send) { _, _ ->
                forwardMessages(peerId, text.text.toString().trim(), messages)
            }
            builder.show()
        }
    }

    private fun confirmForward(messages: ArrayList<VKMessage>) {
        FragmentSelector.addFragment(
            fragmentManager!!,
            FragmentConversations(true).apply {
                setTargetFragment(this@FragmentMessages, REQUEST_CHOOSE_MESSAGE)
            },
            Bundle().apply { putSerializable("fwd_messages", messages) },
            true
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHOOSE_MESSAGE && resultCode == Activity.RESULT_OK) {
            data ?: return

            val forwardingMessages = data.getSerializableExtra("fwd_messages") as ArrayList<VKMessage>? ?: return

            val conversation =
                data.getSerializableExtra("conversation") as VKConversation? ?: return

            val text = AppCompatEditText(activity!!)
            text.hint = getString(R.string.message)

            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(R.string.reply)
            builder.setView(text)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(R.string.send) { _, _ ->
                forwardMessages(
                    conversation.peerId,
                    text.text.toString().trim(),
                    forwardingMessages
                )
            }
            builder.show()
        }
    }

    private fun forwardMessages(peerId: Int, text: String, messages: ArrayList<VKMessage>) {
        if (ArrayUtil.isEmpty(messages)) return

        TaskManager.execute {
            VKApi.messages().send().randomId(random.nextInt()).forwardMessages(messages)
                .message(text).peerId(peerId).execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {

                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(
                            activity!!,
                            getString(R.string.error) + ": $e",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    private fun replyMessage(replyId: Int, text: String) {
        TaskManager.execute {
            VKApi.messages().send().randomId(random.nextInt()).replyTo(replyId).message(text)
                .peerId(peerId).execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(
                            activity!!,
                            getString(R.string.error) + ": $e",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    private fun toggleChatState() {
        conversation ?: return

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ ->
            val leave = conversation!!.state == VKConversation.State.IN
            val chatId = VKConversation.toChatId(conversation!!.last!!.peerId)
            setChatState(chatId, leave)
        }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun setChatState(chatId: Int, leave: Boolean) {
        TaskManager.execute {
            val setter: MethodSetter = if (leave) {
                VKApi.messages()
                    .removeChatUser()
                    .chatId(chatId)
                    .userId(UserConfig.userId)
            } else {
                VKApi.messages()
                    .addChatUser()
                    .chatId(chatId)
                    .userId(UserConfig.userId)
            }

            setter.execute(Int::class.java, object : OnCompleteListener {
                override fun onComplete(models: ArrayList<*>?) {
                    conversation!!.state =
                        if (leave) VKConversation.State.LEFT else VKConversation.State.IN
                    CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation!!)
                    updateToolbar()

                    popupAdapter!!.changeItems(createItems())
                }

                override fun onError(e: Exception) {
                    Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun toggleNotifications() {
        if (conversation == null) return
        if (conversation!!.isNotificationsDisabled) {
            conversation!!.isNoSound = false
            conversation!!.isDisabledForever = false
            conversation!!.disabledUntil = 0
        } else {
            conversation!!.isNoSound = true
            conversation!!.disabledUntil = -1
            conversation!!.isDisabledForever = true
        }

        setNotifications(!conversation!!.isNotificationsDisabled)
    }

    private fun setNotifications(on: Boolean) {
        TaskManager.execute {

            VKApi.account()
                .setSilenceMode()
                .peerId(peerId)
                .time(if (on) 0 else -1)
                .sound(on)
                .execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        conversation!!.isDisabledForever = !on
                        conversation!!.disabledUntil = if (on) 0 else -1
                        conversation!!.isNoSound = !on

                        popupAdapter!!.changeItems(createItems())

                        CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversation!!)
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    @SuppressLint("InflateParams")
    private fun showConfirmDeleteMessages(items: ArrayList<VKMessage>) {
        val mIds = IntArray(items.size)

        var self = true
        var can = true

        for (i in items.indices) {
            val message = items[i]
            mIds[i] = message.id

            if (message.date * 1000L < System.currentTimeMillis() - AlarmManager.INTERVAL_DAY)
                can = false

            if (message.fromId != UserConfig.userId)
                self = false
        }

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.are_you_sure)

        val v = LayoutInflater.from(activity!!)
            .inflate(R.layout.activity_messages_dialog_checkbox, null, false)

        val checkBox = v.findViewById<MaterialCheckBox>(R.id.checkBox)
        checkBox.setText(R.string.for_everyone)
        checkBox.isEnabled = peerId != UserConfig.userId && can
        checkBox.isChecked = true

        if (self)
            adb.setView(v)

        val forAll = if (self) if (checkBox.isEnabled) checkBox.isChecked else null else null

        adb.setPositiveButton(R.string.yes) { _, _ ->
            if (editing) {
                editing = false
                updateStyles()

                editingPosition = -1

                adapter!!.clearSelected()
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
            }

            deleteMessages(items, forAll, *mIds)
        }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun deleteMessages(messages: ArrayList<VKMessage>, forAll: Boolean?, vararg mIds: Int) {
        TaskManager.execute {

            try {
                if (messages[0].status != VKMessage.Status.ERROR)
                    VKApi.messages().delete().messageIds(*mIds).every(forAll).execute()
                activity!!.runOnUiThread {
                    adapter!!.values!!.removeAll(messages)
                    adapter!!.clearSelected()
                    adapter!!.notifyDataSetChanged()

                    updateToolbar()

                    for (message in messages) {
                        CacheStorage.delete(
                            DatabaseHelper.MESSAGES_TABLE,
                            DatabaseHelper.MESSAGE_ID,
                            message.id
                        )
                    }
                }
            } catch (e: Exception) {
                activity!!.runOnUiThread {
                    Toast.makeText(
                        activity!!,
                        R.string.error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()
    }

    private fun showAlert(position: Int) {
        conversation ?: return
        conversation!!.last ?: return
        val item = adapter!!.getItem(position)

        val list = arrayListOf(*resources.getStringArray(R.array.message_functions))
        val remove = arrayListOf<String>()

        if (!ArrayUtil.isEmpty(item.attachments)) {
            for (model in item.attachments) {
                if (model is VKLink) {
                    if (model.url.contains("poll")) {
                        remove.add(getString(R.string.edit))
                        break
                    }
                }
            }
        }

        if (TextUtils.isEmpty(conversation!!.last!!.text)) {
            remove.add(getString(R.string.copy))
        }

        if (item.status == VKMessage.Status.ERROR) {
            remove.add(getString(R.string.pin_message))
            remove.add(getString(R.string.edit))
            remove.add(getString(R.string.forward))
        } else {
            remove.add(getString(R.string.retry))
            if (!conversation!!.isCanChangePin) {
                remove.add(getString(R.string.pin_message))
            }

            if (conversation!!.last!!.date * 1000L < System.currentTimeMillis() - AlarmManager.INTERVAL_DAY || !item.isOut) {
                remove.add(getString(R.string.edit))
            }
        }

        if (pinned != null && pinned!!.id == item.id) {
            remove.add(getString(R.string.pin_message))
        }

        list.removeAll(remove)

        val items = arrayOfNulls<String>(list.size)
        for (i in list.indices)
            items[i] = list[i]

        val adb = AlertDialog.Builder(activity!!)

        adb.setItems(items) { _, i ->
            when (items[i]) {
                getString(R.string.forward) -> confirmForward(arrayListOf(item))
                getString(R.string.reply) -> confirmReply(arrayListOf(item))
                getString(R.string.copy) -> copyMessageText(item)
                getString(R.string.retry) -> TaskManager.resendMessage(item.randomId.toLong())
                getString(R.string.delete) -> showConfirmDeleteMessages(ArrayList(listOf(item)))
                getString(R.string.pin_message) -> showConfirmPinDialog(item)
                getString(R.string.edit) -> {
                    edited = item

                    if (editing)
                        message.setText("")

                    adapter!!.clearSelected()
                    adapter!!.setSelected(position, true)
                    adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)

                    editingPosition = position

                    showEdit()
                }
            }
        }
        adb.show()
    }

    private fun copyMessageText(item: VKMessage) {
        val clipService = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipService.setPrimaryClip(ClipData.newPlainText("msg id: ${item.id}", item.text))

        Toast.makeText(activity!!, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun showEdit() {
        editing = true

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        message.requestFocus()
        ViewUtil.showKeyboard(message)

        messageText = message.text!!.toString()
        updateStyles()
    }

    private fun updateStyles() {
        if (editing) {
            message.setText(edited!!.text)
            setDoneStyle()
        } else {
            adapter!!.clearSelected()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)

            if (messageText!!.trim().isEmpty()) {
                message.setText("")
                setMicStyle()
            } else {
                message.setText(messageText)
                setSendStyle()
            }

            messageText = null
        }

        updateToolbar()
        message.setSelection(message.text!!.length)
    }

    private fun editMessage(edited: VKMessage) {
        edited.text = message.text!!.toString().trim()

        if (edited.text!!.trim().isEmpty() && ArrayUtil.isEmpty(edited.attachments) && ArrayUtil.isEmpty(
                edited.fwdMessages
            )
        ) {
            showConfirmDeleteMessages(ArrayList(listOf(edited)))
        } else {
            val message = adapter!!.getItem(editingPosition)
            message.status = VKMessage.Status.SENDING
            message.isSelected = false
            adapter!!.notifyItemChanged(editingPosition, -1)

            editing = false
            this.edited = null

            updateStyles()

            TaskManager.execute {
                VKApi.messages().edit()
                    .message(edited.text!!)
                    .messageId(edited.id)
                    .attachment(edited.attachments)
                    .keepForwardMessages(true)
                    .keepSnippets(true)
                    .peerId(peerId)
                    .execute(Int::class.java, object : OnCompleteListener {
                        override fun onComplete(models: ArrayList<*>?) {
                            val editedMessage = adapter!!.getItem(editingPosition)
                            editedMessage.text = edited.text
                            editedMessage.status = VKMessage.Status.SENT
                            editedMessage.updateTime = System.currentTimeMillis()

                            adapter!!.notifyItemChanged(editingPosition, -1)
                            editingPosition = -1
                        }

                        override fun onError(e: Exception) {
                            activity!!.runOnUiThread {
                                adapter!!.getItem(editingPosition).status = VKMessage.Status.ERROR
                                adapter!!.notifyItemChanged(editingPosition, -1)
                                Toast.makeText(
                                    activity!!,
                                    R.string.error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            }
        }
    }

    private fun showConfirmPinDialog(message: VKMessage) {
        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ -> pinMessage(message) }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        setTyping()

        if (!editing) {
            if (s.toString().trim().isEmpty()) {
                setMicStyle()
            } else {
                setSendStyle()
            }
        } else {
            if (s.toString().trim().isEmpty() && ArrayUtil.isEmpty(edited!!.attachments)) {
                setTrashStyle()
                send.setOnClickListener { showConfirmDeleteMessages(ArrayList(listOf(edited!!))) }
            } else {
                setDoneStyle()
            }
        }
    }

    override fun afterTextChanged(s: Editable) {

    }

    private fun setSendStyle() {
        send.setImageDrawable(iconSend)
        send.setOnClickListener(sendClick)
    }

    private fun setMicStyle() {
        send.setImageDrawable(iconMic)
        send.setOnClickListener(recordClick)
    }

    private fun setDoneStyle() {
        send.setImageDrawable(iconDone)
        send.setOnClickListener(doneClick)
    }

    private fun setTrashStyle() {
        send.setImageDrawable(iconTrash)
    }

    private fun onBackPressed() {
        when {
            editing -> {
                editing = false
                updateStyles()
            }
            adapter?.isSelected!! -> {
                adapter!!.clearSelected()
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                updateToolbar()
            }
            else -> fragmentManager!!.popBackStack()
        }
    }

    override fun onItemClick(position: Int) {
        val item = adapter!!.getItem(position)

        if (item.action != null) return

        if (adapter!!.isSelected && !editing && selectTimer == null && adapter!!.selectedCount == 100) return

        if (adapter!!.isSelected && !editing && selectTimer == null) {
            adapter!!.toggleSelected(position)
            adapter!!.notifyItemChanged(position, -1)
            updateToolbar()
        } else
            showAlert(position)
    }

    override fun onItemLongClick(position: Int) {
        val item = adapter!!.getItem(position)
        if (item.action != null) return

        if (adapter!!.isSelected) {
            adapter!!.clearSelected()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
        } else {
            if (pinned != null)
                pinnedContainer.visibility = View.GONE
            adapter!!.setSelected(position, true)
        }

        updateToolbar()
    }

    private fun showConfirmDeleteConversation() {
        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ -> deleteConversation() }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun deleteConversation() {
        if (!Util.hasConnection()) {
            return
        }

        TaskManager.execute {
            VKApi.messages()
                .deleteConversation()
                .peerId(peerId)
                .offset(0)
                .execute(Int::class.java, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        CacheStorage.delete(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            DatabaseHelper.PEER_ID,
                            peerId
                        )
                        adapter!!.values!!.clear()
                        adapter!!.notifyDataSetChanged()
                    }

                    override fun onError(e: Exception) {
                        Log.e("Error delete dialog", Log.getStackTraceString(e))
                        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }

    private fun onPrepareMenu() {
        val menu = actionTb.menu

        val selecting = adapter?.isSelected

        selecting ?: return

        tb.visibility = if (selecting) View.GONE else View.VISIBLE
        actionTb.visibility = if (tb.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        actionTb.title = if (selecting) adapter!!.selectedCount.toString() else ""

        val delete = menu.findItem(R.id.delete)
        delete.isVisible = selecting
    }

    fun getRecyclerView(): RecyclerView {
        return recyclerView
    }

    fun isRunning(): Boolean {
        return resumed
    }

    fun setNotRead(last: VKMessage?) {
        notRead = last
    }

    companion object {
        private const val MESSAGES_COUNT = 30

        const val REQUEST_CHOOSE_MESSAGE = 1
    }
}