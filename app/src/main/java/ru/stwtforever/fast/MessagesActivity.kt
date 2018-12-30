package ru.stwtforever.fast

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import ru.stwtforever.fast.adapter.MessageAdapter
import ru.stwtforever.fast.adapter.RecyclerAdapter
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.VKUtils
import ru.stwtforever.fast.api.model.VKConversation
import ru.stwtforever.fast.api.model.VKGroup
import ru.stwtforever.fast.api.model.VKMessage
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.LowThread
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.database.CacheStorage
import ru.stwtforever.fast.database.DatabaseHelper
import ru.stwtforever.fast.database.MemoryCache
import ru.stwtforever.fast.fragment.FragmentSettings
import ru.stwtforever.fast.helper.DialogHelper
import ru.stwtforever.fast.helper.FontHelper
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils
import ru.stwtforever.fast.util.ViewUtils
import java.util.*

class MessagesActivity : AppCompatActivity(), TextWatcher, RecyclerAdapter.OnItemClickListener {

    private var toolbar: Toolbar? = null
    private var recycler: RecyclerView? = null

    private var send: ImageButton? = null
    private var smiles: ImageButton? = null
    private var unpin: ImageButton? = null
    private var message: EditText? = null
    private var progress: ProgressBar? = null
    private var chatPanel: LinearLayout? = null

    private var layoutManager: LinearLayoutManager? = null
    private var adapter: MessageAdapter? = null

    private var loading: Boolean = false
    private var busy: Boolean = false
    private var canWrite: Boolean = false
    private var editing: Boolean = false
    private var typing: Boolean = false

    private var reason = -1
    private var peerId: Int = 0
    private var membersCount: Int = 0

    private var text: String? = null
    private var reasonText: String? = null
    private var title: String? = null
    private var subtitle: String? = null
    private var photo: String? = null

    private var noMessages: TextView? = null

    private var timer: Timer? = null

    private var pinned: VKMessage? = null
    private var last: VKMessage? = null
    private var conversation: VKConversation? = null

    private var currentUser: VKUser? = null

    private var pnd: LinearLayout? = null
    private var pName: TextView? = null
    private var pDate: TextView? = null
    private var pText: TextView? = null
    private var line: View? = null

    private val sendListener = View.OnClickListener {
        val s = message!!.text.toString()
        if (!s.trim().isEmpty()) {
            text = s

            sendMessage()
            message!!.text = null
        }
    }

    private val recordListener = View.OnClickListener { }

    private val subtitleStatus: String
        get() {
            var type = 2
            if (peerId > 2000000000) type = 0
            if (peerId < -1) type = 1

            if (conversation != null) {
                if (conversation!!.isChannel) {
                    type = 3
                }
            }

            return when (type) {
                0 -> if (membersCount > 0)
                    getString(R.string.members_count, membersCount)
                else
                    ""
                1 -> getString(R.string.group)
                2 -> {
                    currentUser = CacheStorage.getUser(peerId)
                    getUserSubtitle(currentUser)
                }
                3 -> getString(R.string.channel) + " • " + getString(R.string.members_count, membersCount)
                else -> ""
            } ?: ""
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.applyWindowStyles(this)
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        initViews()
        getIntentData()
        showPinned(pinned)
        checkCanWrite()

        if (ThemeManager.isDark())
            if (chatPanel!!.background != null) {
                chatPanel!!.background.setColorFilter(ThemeManager.getBackground(), PorterDuff.Mode.MULTIPLY)
            }

        layoutManager = LinearLayoutManager(this)
        layoutManager!!.stackFromEnd = true
        layoutManager!!.orientation = RecyclerView.VERTICAL

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = title

        recycler!!.layoutManager = layoutManager

        message!!.addTextChangedListener(this)

        subtitle = subtitleStatus

        getCachedMessages()

        if (Utils.hasConnection()) {
            getHistory(0)
        }

        smiles!!.setOnLongClickListener {
            val template = Utils.getPrefs().getString(FragmentSettings.KEY_MESSAGE_TEMPLATE, FragmentSettings.DEFAULT_TEMPLATE_VALUE)
            if (message!!.text.isEmpty()) {
                message!!.setText(template)
            } else {
                message!!.append(template)
            }
            message!!.setSelection(message!!.text.length)
            true
        }

        val gd = GradientDrawable()
        gd.setColor(ThemeManager.getAccent())
        gd.cornerRadius = 100f

        send!!.background = gd
        send!!.setImageResource(R.drawable.md_mic)

        noMessages!!.visibility = View.GONE

        loadImage()
    }

    fun getRecycler(): RecyclerView? {
        return recycler
    }

    private fun loadImage() {
        try {
            val icon = Picasso.get().load(photo).placeholder(ColorDrawable(Color.RED)).get()
            runOnUiThread { supportActionBar!!.setIcon(BitmapDrawable(icon)) }
        } catch (e: Exception) {
            runOnUiThread { supportActionBar!!.setIcon(null) }
        }
    }

    override fun onItemClick(v: View?, position: Int) {
        val item = adapter!!.getItem(position)

        if (!TextUtils.isEmpty(item.actionType)) return

        when (item.out) {
            true -> showOutDialog(position)
            false -> showInDialog(position)
        }
    }

    private fun checkCanWrite() {
        send!!.isEnabled = true
        smiles!!.isEnabled = true
        message!!.isEnabled = true
        message!!.setHint(R.string.tap_to_type)
        message!!.setText("")

        if (reason <= 0) return
        if (!canWrite) {
            send!!.isEnabled = false
            smiles!!.isEnabled = false
            message!!.isEnabled = false
            message!!.hint = VKUtils.getErrorReason(reason)
            message!!.setText("")
        }
    }

    private fun showPinned(pinned: VKMessage?) {
        pnd = findViewById(R.id.pinned_msg_container)
        pName = pnd!!.findViewById(R.id.name)
        pDate = pnd!!.findViewById(R.id.date)
        pText = pnd!!.findViewById(R.id.message)
        unpin = pnd!!.findViewById(R.id.unpin)
        line = findViewById(R.id.line)

        pName!!.typeface = FontHelper.getFont(FontHelper.PS_BOLD)

        if (pinned == null) {
            line!!.visibility = View.GONE
            pnd!!.visibility = View.GONE
            checkPinnedExists()
            return
        }

        checkPinnedExists()

        pnd!!.visibility = View.VISIBLE
        line!!.visibility = View.VISIBLE

        pnd!!.setOnClickListener {
            if (adapter!!.contains(pinned.id)) {
                recycler!!.scrollToPosition(adapter!!.findPosition(pinned.id))
            } else
                showAlertPinned(pinned)
        }

        var user = CacheStorage.getUser(pinned.fromId)
        if (user == null) user = VKUser.EMPTY

        pName!!.text = user!!.toString()
        pDate!!.text = Utils.dateFormatter.format(pinned.date * 1000)

        pText!!.text = pinned.text

        unpin!!.visibility = if (conversation!!.can_change_pin) View.VISIBLE else View.GONE
        unpin!!.setOnClickListener { showConfirmUnpinDialog() }

        if ((pinned.attachments != null || !ArrayUtil.isEmpty(pinned.fwd_messages)) && TextUtils.isEmpty(pinned.text)) {

            val body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages)

            val r = "<b>$body</b>"
            val span = SpannableString(Html.fromHtml(r))
            span.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorAccent)), 0, body.length, 0)

            pText!!.append(span)
        }
    }

    private fun showAlertPinned(pinned: VKMessage) {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.pinned_message)

        val v = LayoutInflater.from(this).inflate(R.layout.alert_pinned, null, false)

        val pnd = v.findViewById<LinearLayout>(R.id.pinned_msg_container)
        val name = pnd.findViewById<TextView>(R.id.name)
        val date = pnd.findViewById<TextView>(R.id.date)
        val text = pnd.findViewById<TextView>(R.id.message)

        var user = CacheStorage.getUser(pinned.fromId)
        if (user == null) user = VKUser.EMPTY

        name.typeface = FontHelper.getFont(FontHelper.PS_BOLD)

        name.text = user!!.toString()
        date.text = Utils.dateFormatter.format(pinned.date * 1000)

        text.text = pinned.text

        if ((pinned.attachments != null || !ArrayUtil.isEmpty(pinned.fwd_messages)) && TextUtils.isEmpty(pinned.text)) {

            val body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages)

            val r = "<b>$body</b>"
            val span = SpannableString(Html.fromHtml(r))
            span.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorAccent)), 0, body.length, 0)

            text.append(span)
        }

        adb.setView(v)
        adb.setPositiveButton(android.R.string.ok, null)

        DialogHelper.create(adb).show()
    }

    private fun pinMessage(position: Int) {
        if (pinned != null && pinned!!.id == adapter!!.getItem(position).id) return
        ThreadExecutor.execute(object : AsyncCallback(this) {

            var msg: VKMessage? = null

            @Throws(Exception::class)
            override fun ready() {
                msg = adapter!!.getItem(position)
                VKApi.messages().pin().peerId(peerId.toLong()).messageId(msg!!.id).execute<Any>(null)
            }

            override fun done() {
                conversation!!.pinned = msg
                pinned = msg
                showPinned(msg)
            }

            override fun error(e: Exception) {
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun unpinMessage() {
        ThreadExecutor.execute(object : AsyncCallback(this) {

            @Throws(Exception::class)
            override fun ready() {
                VKApi.messages().unpin().peerId(peerId.toLong()).execute<Any>(null)
            }

            override fun done() {
                conversation!!.pinned = null
                pinned = null
                showPinned(null)
            }

            override fun error(e: Exception) {
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }

        })
    }

    private fun sendMessage() {
        if (busy || TextUtils.isEmpty(text)) return
        busy = true

        val msg = VKMessage()
        msg.text = text
        msg.fromId = UserConfig.userId
        msg.peerId = peerId
        msg.date = Calendar.getInstance().timeInMillis
        msg.out = true
        msg.status = VKMessage.STATUS_SENDING
        msg.isAdded = true
        msg.randomId = Random().nextLong()

        adapter!!.values.add(msg)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, null)
        recycler!!.smoothScrollToPosition(adapter!!.itemCount + 1)

        val size = adapter!!.itemCount

        ThreadExecutor.execute(object : AsyncCallback(this) {

            @Throws(Exception::class)
            override fun ready() {
                msg.id = VKApi.messages().send().peerId(peerId.toLong()).randomId(msg.randomId).text(text!!.trim { it <= ' ' }).execute(Int::class.java)[0]
            }

            override fun done() {
                if (typing) {
                    typing = false
                    if (timer != null)
                        timer!!.cancel()
                }

                checkMessagesCount()
                busy = false
                msg.status = VKMessage.STATUS_SENT

                if (adapter!!.itemCount > size) {
                    val i = adapter!!.values.indexOf(msg)
                    adapter!!.remove(i)
                    adapter!!.add(msg)
                    adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, msg)
                }
            }

            override fun error(e: Exception) {
                e.printStackTrace()
                busy = false
                msg.status = VKMessage.STATUS_ERROR
                adapter!!.notifyItemChanged(adapter!!.itemCount - 1, msg)
            }
        })

    }

    fun edit(position: Int) {
        val m = adapter!!.values[position]
        val oldText = m.text

        adapter!!.showHover(position, true)

        applyStyles(true)
        editing = true

        message!!.setText(oldText)
        message!!.setSelection(oldText.length)

        send!!.setOnClickListener {
            val newText = message!!.text.toString()

            if (!TextUtils.isEmpty(newText) && ArrayUtil.isEmpty(m.attachments) && ArrayUtil.isEmpty(m.fwd_messages) || !ArrayUtil.isEmpty(m.attachments) || !ArrayUtil.isEmpty(m.fwd_messages)) {
                editMessage(position, m.id, oldText, newText)
            } else {
                if (ArrayUtil.isEmpty(m.attachments) && ArrayUtil.isEmpty(m.fwd_messages))
                    showDeleteOutDialog(position)
            }
        }
    }


    override fun onBackPressed() {
        if (editing) {
            editing = false
            applyStyles(editing)
            message!!.setText("")
            checkHovered()
        } else
            super.onBackPressed()
    }

    private fun checkHovered() {
        var position = -1

        for (i in 0 until adapter!!.values.size) {
            val m = adapter!!.values[i]
            if (m.isSelected) position = i
        }

        if (position == -1) return

        adapter!!.values[position].isSelected = false
        adapter!!.notifyItemChanged(position)
    }

    private fun deleteMessage(pos: Int, every: Boolean?, spam: Boolean?) {
        if (busy) return
        busy = true
        ThreadExecutor.execute(object : AsyncCallback(this) {

            @Throws(Exception::class)
            override fun ready() {
                val id = adapter!!.values[pos].id
                VKApi.messages()
                        .delete()
                        .messageIds(id)
                        .every(every)
                        .spam(spam)
                        .execute(Int::class.java)
            }

            override fun done() {
                busy = false
                if (adapter!!.isHover(pos)) {
                    adapter!!.showHover(pos, false)
                }

                adapter!!.values.removeAt(pos)
                adapter!!.notifyDataSetChanged()
                checkMessagesCount()
            }

            override fun error(e: Exception) {
                busy = false
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun editMessage(position: Int, messageId: Int, oldText: String, newText: String) {
        if (busy) return
        busy = true

        val msg = adapter!!.values[position]
        msg.status = VKMessage.STATUS_SENDING
        adapter!!.notifyItemChanged(position, msg)
        ThreadExecutor.execute(object : AsyncCallback(this) {

            internal var res: Int = 0

            @Throws(Exception::class)
            override fun ready() {
                res = VKApi.messages()
                        .edit()
                        .peerId(peerId.toLong())
                        .text(newText)
                        .messageId(messageId)
                        .keepForwardMessages(true)
                        .attachment(msg.attachments)
                        .keepSnippets(true)
                        .dontParseLinks(false)
                        .execute(Int::class.java)[0]
            }

            override fun done() {
                busy = false
                adapter!!.values[position].text = newText
                adapter!!.values[position].status = VKMessage.STATUS_SENT
                adapter!!.values[position].isSelected = false
                adapter!!.notifyItemChanged(position, msg)

                editing = false

                applyStyles(false)
                message!!.setText("")
            }

            override fun error(e: Exception) {
                busy = false
                adapter!!.values[position].status = VKMessage.STATUS_ERROR
                adapter!!.notifyItemChanged(position, msg)
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_SHORT).show()
            }
        })


    }

    private fun applyStyles(isEdit: Boolean) {
        if (isEdit) {
            send!!.setImageResource(R.drawable.md_done)
        } else {
            val s = message!!.text.toString()

            applyBtnStyle(TextUtils.isEmpty(s.trim { it <= ' ' }))
        }
    }

    override fun onTextChanged(cs: CharSequence, p2: Int, p3: Int, p4: Int) {
        if (!typing) {
            setTyping()
        }
        if (!editing) {
            applyBtnStyle(cs.toString().trim { it <= ' ' }.isEmpty())
        }
    }

    override fun beforeTextChanged(cs: CharSequence, p2: Int, p3: Int, p4: Int) {}

    override fun afterTextChanged(p1: Editable) {}

    private fun setTyping() {
        if (Utils.getPrefs().getBoolean(FragmentSettings.KEY_HIDE_TYPING, false)) return
        typing = true
        LowThread(Runnable {
            try {
                VKApi.messages().setActivity().peerId(peerId.toLong()).execute(Int::class.java)
                runOnUiThread {
                    timer = Timer()
                    timer!!.schedule(object : TimerTask() {

                        override fun run() {
                            typing = false
                        }


                    }, 10000)
                }
            } catch (ignored: Exception) {
            }
        }).start()
    }

    private fun applyBtnStyle(isTextEmpty: Boolean) {
        if (isTextEmpty) {
            send!!.setImageResource(R.drawable.md_mic)
            if (!editing)
                send!!.setOnClickListener(recordListener)
        } else {
            send!!.setImageResource(R.drawable.md_send)
            if (!editing)
                send!!.setOnClickListener(sendListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (adapter != null) {
            adapter!!.destroy()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_chat_history, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menuUpdate -> if (!loading && !editing) {
                adapter!!.values.clear()
                getHistory(0)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        chatPanel = findViewById(R.id.chat_panel)
        smiles = findViewById(R.id.smiles)
        toolbar = findViewById(R.id.tb)
        recycler = findViewById(R.id.list)
        send = findViewById(R.id.send)
        message = findViewById(R.id.message)
        progress = findViewById(R.id.progress)
        noMessages = findViewById(R.id.text_no_messages)
    }

    private fun getIntentData() {
        val intent = intent
        this.conversation = intent.getSerializableExtra("conversation") as VKConversation
        this.title = intent.getStringExtra("title")
        this.photo = intent.getStringExtra("photo")
        this.peerId = intent.getIntExtra("peer_id", -1)
        this.reason = intent.getIntExtra("reason", -1)
        this.canWrite = intent.getBooleanExtra("can_write", false)
        this.reasonText = if (canWrite) "" else VKUtils.getErrorReason(reason)

        if (conversation != null) {
            this.last = conversation!!.last
            this.membersCount = conversation!!.membersCount
            this.pinned = conversation!!.pinned
        }

        checkPinnedExists()
    }

    private fun checkPinnedExists() {
        val space = findViewById<Space>(R.id.space)
        space.visibility = if (pinned == null) View.GONE else View.VISIBLE
    }

    private fun createAdapter(messages: ArrayList<VKMessage>, offset: Int) {
        checkMessagesCount()

        if (ArrayUtil.isEmpty(messages)) {
            return
        }

        checkMessagesCount()

        val isEmpty: Boolean = if (adapter == null) false else adapter!!.itemCount == 0

        if (offset != 0) {
            adapter!!.changeItems(messages)
            if (isEmpty)
                adapter!!.notifyItemRangeInserted(0, adapter!!.itemCount)
            else
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, null)
            return
        }

        checkMessagesCount()

        if (adapter != null) {
            adapter!!.changeItems(messages)
            if (isEmpty)
                adapter!!.notifyItemRangeInserted(0, adapter!!.itemCount)
            else
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, null)
            return
        }

        adapter = MessageAdapter(this, messages, peerId)
        recycler!!.adapter = adapter
        adapter!!.setOnItemClickListener(this)
        recycler!!.scrollToPosition(adapter!!.itemCount)

        checkMessagesCount()
    }

    private fun getCachedMessages() {
        val messages = CacheStorage.getMessages(peerId)
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages, 0)
        }
    }

    private fun getUserSubtitle(user: VKUser?): String {
        return when {
            user!!.online -> getString(R.string.online)
            else -> getString(when (user.sex == VKUser.Sex.MALE) {
                true -> R.string.last_seen_m
                false -> R.string.last_seen_w
            }, Utils.dateFormatter.format(user.lastSeen * 1000))

        } ?: ""
    }

    fun checkMessagesCount() {
        if (adapter == null) return

        val count = adapter!!.values.size

        if (count > 0) {
            noMessages!!.visibility = View.GONE
        } else {
            noMessages!!.visibility = View.VISIBLE
        }
    }

    private fun getUserIds(ids: HashSet<Int>, messages: ArrayList<VKMessage>) {
        for (m in messages) {
            if (!VKGroup.isGroupId(m.fromId)) {
                ids.add(m.fromId)
            }
            if (m.peerId < 2000000000) {
                ids.add(m.peerId)
            }
        }
        for (msg in messages) {
            if (!ArrayUtil.isEmpty(msg.fwd_messages)) {
                getUserIds(ids, msg.fwd_messages)
            }
        }
    }

    private fun getUsers(messages: ArrayList<VKMessage>) {
        val ids = HashSet<Int>()
        getUserIds(ids, messages)

        ThreadExecutor.execute(object : AsyncCallback(this) {

            var users: ArrayList<VKUser>? = null

            @Throws(Exception::class)
            override fun ready() {
                users = VKApi.users().get().userIds(ids).fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java)
            }

            override fun done() {
                if (ArrayUtil.isEmpty(users)) {
                    return
                }

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                MemoryCache.update(users)
            }

            override fun error(e: Exception) {
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getHistory(offset: Int) {
        loading = true
        supportActionBar!!.subtitle = getString(R.string.loading)

        ThreadExecutor.execute(object : AsyncCallback(this) {

            var messages: ArrayList<VKMessage>? = null

            @Throws(Exception::class)
            override fun ready() {
                messages = VKApi.messages().history.peerId(peerId.toLong()).extended(true).fields(VKUser.FIELDS_DEFAULT).offset(offset).count(MESSAGES_COUNT).execute(VKMessage::class.java)

                messages!!.reverse()

                val users = messages!![0].history_users
                val groups = messages!![0].history_groups

                if (!ArrayUtil.isEmpty(users)) {
                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                }

                if (!ArrayUtil.isEmpty(groups)) {
                    CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups)
                }

                if (!ArrayUtil.isEmpty(messages)) {
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)
                }

                loading = messages!!.isEmpty()
            }

            override fun done() {
                createAdapter(messages!!, 0)

                supportActionBar!!.subtitle = subtitleStatus
            }

            override fun error(e: Exception) {
                supportActionBar!!.subtitle = subtitleStatus
            }
        })
    }

/*
 *
 *      DIALOGS
 *
 */

    private fun showConfirmPinDialog(position: Int) {
        if (conversation != null && !conversation!!.can_change_pin) return

        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.confirm_pin)
        adb.setNegativeButton(R.string.no, null)
        adb.setPositiveButton(R.string.yes) { di, i -> pinMessage(position) }

        DialogHelper.create(adb).show()
    }

    private fun showConfirmUnpinDialog() {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.confirm_unpin)
        adb.setNegativeButton(R.string.no, null)
        adb.setPositiveButton(R.string.yes) { p1, p2 -> unpinMessage() }

        DialogHelper.create(adb).show()
    }

    private fun showInDialog(position: Int) {
        val adb = AlertDialog.Builder(this)

        var items = arrayOf(getString(R.string.pin_message), getString(R.string.delete))

        val msg = adapter!!.values[position]
        val isError = msg.status == VKMessage.STATUS_ERROR

        if (isError) {
            items = arrayOf(getString(R.string.retry), getString(R.string.delete))
        }

        val listener = DialogInterface.OnClickListener { d, i ->
            when (i) {
                0 -> showConfirmPinDialog(position)
                1 -> showDeleteInDialog(position)
            }
        }

        DialogHelper.create(adb, items, listener)
    }

    private fun showOutDialog(position: Int) {
        if (editing) return

        val adb = AlertDialog.Builder(this)

        val items = arrayOf(getString(R.string.pin_message), getString(R.string.edit), getString(R.string.delete))
        adb.setItems(items) { _, which ->
            when (which) {
                0 -> showConfirmPinDialog(position)
                1 -> edit(position)
                2 -> showDeleteOutDialog(position)
            }
        }

        adb.create().show()
    }

    private fun showDeleteInDialog(position: Int) {
        val m = adapter!!.getItem(position)
        if (m.status == VKMessage.STATUS_ERROR) {
            adapter!!.values.removeAt(position)
            adapter!!.notifyDataSetChanged()
            return
        }

        val adb = AlertDialog.Builder(this)
        adb.setTitle(getString(R.string.delete) + "?")

        val items = arrayOf(getString(R.string.spam))

        val values = booleanArrayOf(false)
        val entries = BooleanArray(1)

        val click = DialogInterface.OnMultiChoiceClickListener { d, i, value ->
            when (i) {
                0 -> entries[0] = value
            }
        }

        adb.setNegativeButton(R.string.no, null)
        adb.setPositiveButton(R.string.yes) { p1, p2 -> deleteMessage(position, null, entries[0]) }

        DialogHelper.create(adb, items, values, click)
    }

    private fun showDeleteOutDialog(position: Int) {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(getString(R.string.delete) + "?")

        val every = arrayOf(getString(R.string.delete_for_everyone))

        val values = booleanArrayOf(true)
        val entries = booleanArrayOf(true)

        val click = DialogInterface.OnMultiChoiceClickListener { d, i, value ->
            when (i) {
                0 -> entries[0] = value
            }
        }

        adb.setNegativeButton(R.string.no, null)
        adb.setPositiveButton(R.string.yes) { _, _ -> deleteMessage(position, entries[0], null) }

        DialogHelper.create(adb, every, values, click)
    }

    companion object {

        const val MESSAGES_COUNT = 60
    }

}
