package ru.melodin.fast.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.checkbox.MaterialCheckBox
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_messages.*
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar_action.*
import ru.melodin.fast.BuildConfig
import ru.melodin.fast.MainActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.MessageAdapter
import ru.melodin.fast.adapter.PopupAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.VKUtil
import ru.melodin.fast.api.model.*
import ru.melodin.fast.api.model.attachment.VKLink
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.AttachmentInflater
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.model.DividerItem
import ru.melodin.fast.model.ListItem
import ru.melodin.fast.mvp.contract.MessagesContract
import ru.melodin.fast.mvp.presenter.MessagesPresenter
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.StringUtils
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*
import kotlin.collections.ArrayList

class FragmentMessages : BaseFragment(), RecyclerAdapter.OnItemClickListener,
    RecyclerAdapter.OnItemLongClickListener, TextWatcher, Toolbar.OnMenuItemClickListener,
    MessagesContract.View {

    var isLoading: Boolean = false

    private var resumed: Boolean = false
    private var editing: Boolean = false
    private var canWrite: Boolean = false
    private var typing: Boolean = false

    var notRead: VKMessage? = null

    private val random = Random()

    private var iconSend: Drawable? = null
    private var iconMic: Drawable? = null
    private var iconDone: Drawable? = null
    private var iconTrash: Drawable? = null

    private var adapter: MessageAdapter? = null

    private var cantWriteReason = -1
    private var peerId = -1
    private var membersCount = 0

    private var photo: String? = null

    private var messageText: String? = null
    private var chatTitle: String? = ""

    private var timer: Timer? = null
    private var selectTimer: Timer? = null

    private var edited: VKMessage? = null

    private var editingPosition = 0

    private var conversation: VKConversation? = null

    private lateinit var layoutManager: LinearLayoutManager

    private var popupWindow: PopupWindow? = null
    private var popupAdapter: PopupAdapter? = null

    private val sendClick = View.OnClickListener {
        if (adapter != null) {
            val s = message.text!!.toString()
            if (s.trim().isNotEmpty()) {
                messageText = s

                sendCurrentMessage()
                message.setText("")
            }
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

            if (conversation!!.lastMessage != null && conversation!!.isChat && conversation!!.state != VKConversation.State.IN) {
                val kicked = conversation!!.state == VKConversation.State.KICKED

                return getString(if (kicked) R.string.kicked_out_text else R.string.leave_from_chat_text)
            } else
                return when (conversation!!.type) {
                    null -> null
                    VKConversation.Type.USER -> {
                        val currentUser = CacheStorage.getUser(peerId)
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

    private val presenter = MessagesPresenter()

    companion object {

        private const val MESSAGES_COUNT = 30

        const val REQUEST_CHOOSE_MESSAGE = 1
    }

    override fun isBottomViewVisible(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getIntentData()

        (activity!! as MainActivity).goBack = false
        presenter.attachView(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshLayout.isEnabled = false

        toolbar = tb
        recyclerList = list

        (tb.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0

        iconSend = ContextCompat.getDrawable(activity!!, R.drawable.md_send)
        iconMic = ContextCompat.getDrawable(activity!!, R.drawable.md_mic)
        iconDone = ContextCompat.getDrawable(activity!!, R.drawable.md_done)
        iconTrash = ContextCompat.getDrawable(activity!!, R.drawable.ic_trash)

        actionTb.visibility = View.GONE

        updateToolbar()
        checkPinnedExists(conversation?.pinned)
        loadConversation(peerId)
        checkCanWrite()

        if (arguments != null && arguments!!.containsKey("text")) {
            messageText = arguments!!.getString("text")

            message.setText(messageText)
            message.setSelection(messageText!!.length)
            setSendStyle()

            ViewUtil.showKeyboard(message)
        }

        tb.setAvatar(R.drawable.avatar_placeholder)
        tb.setTitle(chatTitle)
        tb.setBackVisible(true)

        actionTb.navigationIcon!!.setTint(ThemeManager.MAIN)
        actionTb.setNavigationOnClickListener { onBackPressed() }
        actionTb.inflateMenu(R.menu.activity_chat_history)
        actionTb.setOnMenuItemClickListener(this)
        onPrepareMenu()
        for (i in 0 until actionTb.menu.size()) {
            val item = actionTb.menu[i]
            item.icon?.setTint(ThemeManager.MAIN)
        }

        scrollToBottom.setOnClickListener {
            scrollToBottom.hide()
            if (adapter != null)
                list.smoothScrollToPosition(adapter!!.lastPosition)
        }

        initListScrollListener()

        layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)
        layoutManager.stackFromEnd = true

        list.layoutManager = layoutManager

        if (VKConversation.isChatId(peerId))
            tb.setOnAvatarClickListener { openChatInfo() }

        initPopupWindow()

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

        getCachedMessages(30, 0)
        if (savedInstanceState == null)
            loadMessages(MESSAGES_COUNT, 0)
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
                    PopupAdapter.ID_CLEAR_HISTORY -> showConfirmDeleteConversation()
                    PopupAdapter.ID_NOTIFICATIONS -> toggleNotifications()
                    PopupAdapter.ID_LEAVE -> toggleChatState()
                    PopupAdapter.ID_CHAT_INFO -> openChatInfo()
                    PopupAdapter.ID_ATTACHMENTS -> openChatAttachments()
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
        context ?: return arrayListOf()

        val chatInfo = ListItem(
            PopupAdapter.ID_CHAT_INFO,
            getString(R.string.chat_info),
            drawable(R.drawable.ic_info_black_24dp)
        )

        val chatDivider = DividerItem()

        val attachments = ListItem(
            PopupAdapter.ID_ATTACHMENTS,
            getString(R.string.show_attachments),
            drawable(R.drawable.ic_image_multiple)
        )

        val disableNotifications = ListItem(
            PopupAdapter.ID_NOTIFICATIONS,
            getString(R.string.disable_notifications),
            drawable(R.drawable.ic_volume_off)
        )

        val dangerDivider = DividerItem()

        val clear = ListItem(
            PopupAdapter.ID_CLEAR_HISTORY,
            getString(R.string.clear_messages_history),
            drawable(R.drawable.ic_trash)
        )
        val left = ListItem(PopupAdapter.ID_LEAVE, "", ColorDrawable(Color.TRANSPARENT))

        val items = ArrayList(listOf(chatInfo, chatDivider, attachments, disableNotifications, dangerDivider, clear, left))

        for (item in items)
            updatePopupItemById(item)

        val removeItems = ArrayList<ListItem>()
        if (conversation == null || conversation!!.type != VKConversation.Type.CHAT) {
            removeItems.add(left)
            removeItems.add(chatInfo)
            removeItems.add(chatDivider)
        }

        if (conversation != null && conversation!!.isGroupChannel) {
            removeItems.add(chatInfo)
            removeItems.add(chatDivider)
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
                item.icon = drawable(R.drawable.ic_volume_off)
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
            R.id.forward -> confirmForwardMessages(adapter!!.selectedMessages)
            R.id.delete -> showConfirmDeleteMessages(adapter!!.selectedMessages)
            R.id.reply -> confirmReplyMessages(adapter!!.selectedMessages)
        }

        return true
    }

    private fun loadAvatar() {
        if (photo != null && photo!!.trim().isNotEmpty() && Util.hasConnection()) {
            TaskManager.execute {
                activity!!.runOnUiThread {
                    Picasso.get()
                        .load(photo)
                        .into(tb.avatar)
                }
            }
        } else {
            tb.setAvatar(R.drawable.avatar_placeholder)
        }
    }

    private fun initListScrollListener() {
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (adapter != null && layoutManager.findLastVisibleItemPosition() < adapter!!.itemCount - 10)
                    scrollToBottom.show()
                else
                    scrollToBottom.hide()

                if (dy < 0) {
                    if (message.isFocused && AppGlobal.preferences.getBoolean(
                            FragmentSettings.KEY_HIDE_KEYBOARD_ON_SCROLL,
                            true
                        )
                    ) {
                        ViewUtil.hideKeyboard(message)
                    }
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

    override fun loadConversation(peerId: Int) {
        if (!Util.hasConnection()) {
            if (conversation == null)
                showNoInternetToast()
            return
        }
        TaskManager.loadConversation(peerId, true, object : OnResponseListener {
            override fun onComplete(models: ArrayList<*>?) {
                if (ArrayUtil.isEmpty(models)) return
                models ?: return

                val conversation = models[0] as VKConversation
                this@FragmentMessages.conversation = conversation

                CacheStorage.update(
                    DatabaseHelper.CONVERSATIONS_TABLE,
                    conversation,
                    DatabaseHelper.PEER_ID,
                    conversation.peerId
                )

                when (conversation.type) {
                    VKConversation.Type.CHAT -> {
                        chatTitle = conversation.title
                        photo = conversation.photo200

                        checkPinnedExists(conversation.pinned)

                        updateToolbar()
                    }
                    VKConversation.Type.USER -> TaskManager.loadUser(
                        peerId,
                        object : OnResponseListener {
                            override fun onComplete(models: ArrayList<*>?) {
                                if (ArrayUtil.isEmpty(models)) return
                                models ?: return

                                val user = models[0] as VKUser

                                chatTitle = user.toString()
                                photo = user.photo200

                                updateToolbar()
                            }

                            override fun onError(e: Exception) {}
                        })
                    VKConversation.Type.GROUP -> TaskManager.loadGroup(
                        peerId,
                        object : OnResponseListener {
                            override fun onComplete(models: ArrayList<*>?) {
                                if (ArrayUtil.isEmpty(models)) return
                                models ?: return

                                val group = models[0] as VKGroup

                                chatTitle = group.name
                                photo = group.photo200

                                updateToolbar()
                            }

                            override fun onError(e: Exception) {}
                        })
                }

                popupAdapter!!.changeItems(createItems())

                if (conversation.lastMessage == null && conversation.lastMessageId > 0)
                    loadMessage(conversation.lastMessageId)
            }

            override fun onError(e: Exception) {}
        })
    }

    override fun loadMessage(messageId: Int) {
        TaskManager.loadMessage(messageId, true, object : OnResponseListener {
            override fun onComplete(models: ArrayList<*>?) {
                if (!ArrayUtil.isEmpty(models)) {
                    val message = models!![0] as VKMessage

                    conversation!!.lastMessage = message
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, message)
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
    }

    fun setUserOnline(userId: Int) {
        if (peerId == userId)
            updateToolbar()
    }

    private fun getIntentData() {
        conversation = arguments!!.getSerializable("conversation") as VKConversation?
        chatTitle = arguments!!.getString("title", "")
        peerId = arguments!!.getInt("peer_id", -1)
        photo = arguments!!.getString("photo")
        cantWriteReason = arguments!!.getInt("reason", -1)
        canWrite = arguments!!.getBoolean("can_write", true)

        membersCount = if (conversation != null) {
            conversation!!.membersCount
        } else {
            arguments!!.getInt("members_count", -1)
        }
    }

    private fun openChatInfo() {
        parent?.replaceFragment(
            0,
            FragmentChatInfo(),
            arguments!!.apply { putSerializable("conversation", conversation) },
            true
        )
    }

    private fun openChatAttachments() {
        parent?.replaceFragment(
            0,
            ParentFragmentMessagesAttachments(),
            arguments,
            true
        )
    }

    override fun showCantWrite(reason: Int) {
        chatPanel.isEnabled = false
        send.isEnabled = false
        smiles.isEnabled = false
        message.isEnabled = false
        message.setText(VKUtil.getErrorReason(VKConversation.getReason(reason)))
    }

    override fun hideCantWrite() {
        send.isEnabled = true
        smiles.isEnabled = true
        message.isEnabled = true
        message.setText("")
    }

    private fun checkCanWrite() {
        if (cantWriteReason <= 0) hideCantWrite()
        if (!canWrite) showCantWrite(cantWriteReason)
    }

    override fun selectMessage(message: VKMessage) {
        adapter ?: return

        val index = adapter!!.searchPosition(message.id)

        if (index == -1) {
            showMessageAlert(message)
        } else {
            list.smoothScrollToPosition(index)
            adapter!!.clearSelected()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
            adapter!!.selectItem(index)

            if (selectTimer != null) selectTimer!!.cancel()

            selectTimer = Timer()
            selectTimer?.schedule(object : TimerTask() {
                override fun run() {
                    activity?.runOnUiThread {
                        if (adapter?.isSelected(index)!!)
                            adapter!!.unSelectItem(index)

                        selectTimer = null
                    }
                }
            }, 3500)
        }
    }

    override fun showPinnedMessage(message: VKMessage) {
        pinnedContainer.visibility = View.VISIBLE
        pinnedContainer.setOnClickListener { selectMessage(message) }

        val user = CacheStorage.getUser(message.fromId) ?: VKUser.EMPTY

        pinnedName.text = user.toString().trim()
        pinnedDate.text = Util.formatShortTimestamp(message.date * 1000L)

        pinnedText.text = message.text

        unpin.setOnClickListener { showConfirmUnpinMessage() }

        if (TextUtils.isEmpty(message.text) && !ArrayUtil.isEmpty(message.attachments)) {
            val body = VKUtil.getAttachmentBody(message.attachments, message.fwdMessages)

            val r = "<b>$body</b>"
            val span = SpannableString(HtmlCompat.fromHtml(r, HtmlCompat.FROM_HTML_MODE_LEGACY))
            span.setSpan(ForegroundColorSpan(color(R.color.accent)), 0, body.length, 0)

            pinnedText.append(span)
        }
    }

    override fun hidePinnedMessage() {
        pinnedContainer.visibility = View.GONE
    }

    private fun checkPinnedExists(pinned: VKMessage?) {
        if (pinned == null) {
            hidePinnedMessage()
        } else {
            showPinnedMessage(pinned)
        }
    }

    private fun showMessageAlert(pinned: VKMessage) {
        val adb = AlertDialog.Builder(activity!!)

        val v = AttachmentInflater.getInstance(null, activity!!)
            .message(null, null, pinned, isReply = false, withStyles = false)
        adb.setView(v)
        adb.setPositiveButton(android.R.string.ok, null)
        adb.show()
    }

    private fun showConfirmUnpinMessage() {
        if (conversation != null && !conversation!!.isCanChangePin) {
            hidePinnedMessage()
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
        conversation ?: return
        TaskManager.execute {
            VKApi.messages().unpin().peerId(peerId).execute(null, object : OnResponseListener {
                override fun onComplete(models: ArrayList<*>?) {
                    conversation!!.pinned = null
                    hidePinnedMessage()

                    CacheStorage.update(
                        DatabaseHelper.CONVERSATIONS_TABLE,
                        conversation!!,
                        DatabaseHelper.PEER_ID,
                        conversation!!.peerId
                    )
                }

                override fun onError(e: Exception) {
                    Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun pinMessage(message: VKMessage) {
        conversation ?: return
        TaskManager.execute {
            VKApi.messages().pin().messageId(message.id).peerId(peerId)
                .execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {

                        conversation!!.pinned = message
                        showPinnedMessage(message)

                        CacheStorage.update(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversation!!,
                            DatabaseHelper.PEER_ID,
                            conversation!!.peerId
                        )
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
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
                .execute(null, object : OnResponseListener {
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

    override fun sendCurrentMessage() {
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


        val empty = adapter!!.isEmpty

        adapter!!.addMessage(message)

        val position = adapter!!.lastPosition

        val msg = adapter!!.getItem(position)

        if (empty) {
            setNoItemsViewVisible(false)
        }

        adapter!!.notifyDataSetChanged()
        list.scrollToPosition(position)

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
                ).peerId(peerId), object : OnResponseListener {
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
                        showErrorToast()

                        msg.status = VKMessage.Status.ERROR
                        adapter!!.notifyDataSetChanged()
                    }
                })
        }
    }

    override fun createAdapter(items: ArrayList<VKMessage>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) {
            return
        }

        if (adapter == null) {
            adapter = MessageAdapter(this, items!!, peerId)
            adapter!!.setOnItemClickListener(this)
            adapter!!.setOnItemLongClickListener(this)
            list.adapter = adapter

            list.scrollToPosition(adapter!!.lastPosition)
            return
        }

        if (offset > 0) {
            adapter!!.values!!.addAll(items!!)
            adapter!!.notifyItemRangeInserted(
                adapter!!.values!!.size - items.size - 1,
                items.size
            )
            return
        }

        adapter!!.changeItems(items!!)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)

        list.scrollToPosition(adapter!!.lastPosition)
    }

    override fun getCachedMessages(count: Int, offset: Int) {
        val messages = CacheStorage.getMessages(peerId)


        if (!ArrayUtil.isEmpty(messages)) {
            setProgressBarVisible(false)
            setNoItemsViewVisible(false)
            createAdapter(messages, 0)
        } else {
            setProgressBarVisible(true)
        }
    }

    override fun loadMessages(count: Int, offset: Int) {
        if (!Util.hasConnection()) {
            if (adapter == null || adapter!!.isEmpty)
                setNoItemsViewVisible(true)

            showNoInternetToast()
            return
        }

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
                .execute(VKMessage::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) {
                            setNoItemsViewVisible(true)
                            return
                        }

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

                        presenter.onFilledList()
                        updateToolbar()
                    }

                    override fun onError(e: Exception) {
                        if (adapter == null || adapter!!.isEmpty) {
                            setNoItemsViewVisible(true)
                        }
                        updateToolbar()
                        showErrorToast()
                    }
                })

        }
    }

    override fun setProgressBarVisible(visible: Boolean) {
        when (visible) {
            true -> {
                progressBar.visibility = View.VISIBLE
                setRefreshing(false)
                setNoItemsViewVisible(false)
            }
            else -> progressBar.visibility = View.GONE
        }
    }

    override fun setRefreshing(value: Boolean) {}

    override fun setNoItemsViewVisible(visible: Boolean) {
        when (visible) {
            true -> {
                emptyView.visibility = View.VISIBLE
                setProgressBarVisible(false)
            }
            else -> emptyView.visibility = View.GONE
        }
    }

    override fun clearList() {
        adapter ?: return
        adapter!!.clear()
        adapter!!.notifyDataSetChanged()
        setNoItemsViewVisible(true)
    }

    override fun showNoInternetToast() {
        Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorToast() {
        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_SHORT).show()
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
    }

    fun updateToolbar() {
        parent!!.runOnUiThread {
            onPrepareMenu()
            loadAvatar()

            tb.setTitle(chatTitle)
            tb.setSubtitle(if (isLoading) getString(R.string.loading) else subtitle)

            if (!editing && adapter != null && !adapter!!.isSelected && conversation != null && conversation!!.pinned != null)
                pinnedContainer.visibility = View.VISIBLE
        }
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


    override fun confirmReplyMessages(messages: ArrayList<VKMessage>) {
        if (messages.size == 1) {
            val replyId = messages[0].id

            val text = AppCompatEditText(activity!!)
            text.hint = getString(R.string.message)
            text.setText(message.text.toString())

            val builder = AlertDialog.Builder(activity!!)
            builder.setTitle(R.string.reply)
            builder.setView(text)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(R.string.send) { _, _ ->
                message.setText("")
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

    override fun confirmForwardMessages(messages: ArrayList<VKMessage>) {
        parent!!.replaceFragment(
            0,
            FragmentChooseConversation().apply {
                setTargetFragment(
                    this@FragmentMessages,
                    REQUEST_CHOOSE_MESSAGE
                )
            },
            Bundle().apply { putSerializable("fwd_messages", messages) },
            true
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHOOSE_MESSAGE && resultCode == Activity.RESULT_OK) {
            data ?: return

            val forwardingMessages =
                data.getSerializableExtra("fwd_messages") as ArrayList<VKMessage>? ?: return

            val conversation =
                data.getSerializableExtra("conversation") as VKConversation? ?: return

            val text = AppCompatEditText(activity!!)
            text.hint = getString(R.string.message)

            val builder = AlertDialog.Builder(activity!!).apply {
                setTitle(R.string.reply)
                setView(text)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(R.string.send) { _, _ ->
                    forwardMessages(
                        conversation.peerId,
                        text.text.toString().trim(),
                        forwardingMessages
                    )
                }
            }
            builder.show()
        }
    }

    override fun forwardMessages(peerId: Int, text: String, messages: ArrayList<VKMessage>) {
        adapter?.clearSelected()
        adapter?.notifyDataSetChanged()

        if (ArrayUtil.isEmpty(messages)) return

        val ids = arrayListOf<Int>()
        for (message in messages) {
            ids.add(message.id)
        }

        TaskManager.execute {
            VKApi.messages().send().randomId(random.nextInt()).forwardMessages(ids)
                .message(text).peerId(peerId).execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {

                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
        }
    }

    override fun replyMessage(replyId: Int, text: String) {
        adapter?.clearSelected()
        adapter?.notifyDataSetChanged()
        TaskManager.execute {
            VKApi.messages().send().randomId(random.nextInt()).replyTo(replyId).message(text)
                .peerId(peerId).execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
        }
    }

    override fun leaveFromChat() {
        conversation ?: return

        TaskManager.execute {
            VKApi.messages()
                .removeChatUser()
                .chatId(VKConversation.toChatId(conversation!!.peerId))
                .userId(UserConfig.userId)
                .execute(object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        conversation!!.state = VKConversation.State.LEFT
                        CacheStorage.update(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversation!!,
                            DatabaseHelper.PEER_ID,
                            conversation!!.peerId
                        )

                        updateToolbar()

                        popupAdapter!!.changeItems(createItems())
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
        }
    }

    override fun returnToChat() {
        conversation ?: return

        TaskManager.execute {
            VKApi.messages()
                .addChatUser()
                .chatId(VKConversation.toChatId(conversation!!.peerId))
                .userId(UserConfig.userId)
                .execute(object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        conversation!!.state = VKConversation.State.IN
                        CacheStorage.update(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversation!!,
                            DatabaseHelper.PEER_ID,
                            conversation!!.peerId
                        )

                        updateToolbar()

                        popupAdapter!!.changeItems(createItems())
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
        }
    }

    private fun toggleChatState() {
        conversation ?: return

        if (conversation!!.state != VKConversation.State.IN) {
            returnToChat()
            return
        }

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ ->
            leaveFromChat()
        }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
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

        if (conversation!!.isNotificationsDisabled) {
            enableNotifications()
        } else {
            disableNotifications()
        }
    }

    override fun enableNotifications() {
        conversation ?: return
        TaskManager.execute {
            VKApi.account()
                .setSilenceMode()
                .peerId(peerId)
                .time(0)
                .sound(true)
                .execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        conversation!!.isDisabledForever = false
                        conversation!!.disabledUntil = 0
                        conversation!!.isNoSound = false

                        popupAdapter!!.changeItems(createItems())

                        CacheStorage.update(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversation!!,
                            DatabaseHelper.PEER_ID,
                            conversation!!.peerId
                        )
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
        }
    }

    override fun disableNotifications() {
        conversation ?: return
        TaskManager.execute {
            VKApi.account()
                .setSilenceMode()
                .peerId(peerId)
                .time(-1)
                .sound(false)
                .execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        conversation!!.isDisabledForever = true
                        conversation!!.disabledUntil = -1
                        conversation!!.isNoSound = true

                        popupAdapter!!.changeItems(createItems())

                        CacheStorage.update(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversation!!,
                            DatabaseHelper.PEER_ID,
                            conversation!!.peerId
                        )
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
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
                adapter!!.notifyDataSetChanged()
            }

            deleteMessages(items, forAll)
        }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    override fun removeMessages(messages: ArrayList<VKMessage>) {
        parent!!.runOnUiThread {
            adapter!!.values!!.removeAll(messages)
            adapter!!.notifyDataSetChanged()
        }
    }

    override fun deleteMessages(messages: ArrayList<VKMessage>, forAll: Boolean?) {
        TaskManager.execute {
            var hasError = false
            for (message in messages) {
                if (message.status == VKMessage.Status.ERROR) {
                    hasError = true
                    break
                }
            }

            adapter!!.clearSelected()
            removeMessages(messages)
            updateToolbar()

            if (!hasError) {
                VKApi.messages().delete().messageIds(messages).every(forAll).execute(object :
                    OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        for (message in messages) {
                            CacheStorage.delete(
                                DatabaseHelper.MESSAGES_TABLE,
                                DatabaseHelper.MESSAGE_ID,
                                message.id
                            )
                        }
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()
    }

    private fun showAlert(position: Int) {
        conversation ?: return
        conversation!!.lastMessage ?: return

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

        if (TextUtils.isEmpty(conversation!!.lastMessage!!.text)) {
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

            if (conversation!!.lastMessage!!.date * 1000L < System.currentTimeMillis() - AlarmManager.INTERVAL_DAY || !item.isOut) {
                remove.add(getString(R.string.edit))
            }
        }

        if (conversation!!.pinned != null && conversation!!.pinned!!.id == item.id) {
            remove.add(getString(R.string.pin_message))
        }

        list.removeAll(remove)

        val items = arrayOfNulls<String>(list.size)
        for (i in list.indices)
            items[i] = list[i]

        val adb = AlertDialog.Builder(activity!!)

        adb.setItems(items) { _, i ->
            when (items[i]) {
                getString(R.string.forward) -> confirmForwardMessages(arrayListOf(item))
                getString(R.string.reply) -> confirmReplyMessages(arrayListOf(item))
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
        item.text ?: return
        Util.copyText(item.text!!)

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

    override fun editMessage(message: VKMessage) {
        message.text = message.text!!.toString().trim()

        if (message.text!!.trim().isEmpty() && ArrayUtil.isEmpty(message.attachments) && ArrayUtil.isEmpty(
                message.fwdMessages
            )
        ) {
            showConfirmDeleteMessages(ArrayList(listOf(message)))
        } else {
            val current = adapter!!.getItem(editingPosition)
            current.status = VKMessage.Status.SENDING
            current.isSelected = false
            adapter!!.notifyItemChanged(editingPosition, -1)

            editing = false
            this.edited = null

            updateStyles()

            TaskManager.execute {
                VKApi.messages().edit()
                    .message(current.text!!)
                    .messageId(current.id)
                    .attachment(current.attachments)
                    .keepForwardMessages(true)
                    .keepSnippets(true)
                    .peerId(peerId)
                    .execute(Int::class.java, object : OnResponseListener {
                        override fun onComplete(models: ArrayList<*>?) {
                            val editedMessage = adapter!!.getItem(editingPosition)
                            editedMessage.text = current.text
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

    override fun onBackPressed() {
        when {
            editing -> {
                editing = false
                updateStyles()
            }
            adapter != null && adapter!!.isSelected -> {
                adapter!!.clearSelected()
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                updateToolbar()
            }
            else -> {
                parent!!.goBack = true
                parent!!.onBackPressed()
            }
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
            if (conversation!!.pinned != null)
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
                .execute(Int::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        CacheStorage.delete(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            DatabaseHelper.PEER_ID,
                            peerId
                        )

                        CacheStorage.delete(
                            DatabaseHelper.MESSAGES_TABLE,
                            DatabaseHelper.PEER_ID,
                            peerId
                        )

                        clearList()
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

        menu.findItem(R.id.delete).apply { isVisible = selecting }
    }

    fun isRunning(): Boolean {
        return resumed
    }
}