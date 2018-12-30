package ru.stwtforever.fast

import android.content.DialogInterface
import android.graphics.PorterDuff
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
import ru.stwtforever.fast.adapter.MessageAdapter
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

class MessagesActivity : AppCompatActivity(), TextWatcher {

    private var toolbar: Toolbar? = null
    var recycler: RecyclerView? = null
        private set
    private var send: ImageButton? = null
    private var smiles: ImageButton? = null
    private var unpin: ImageButton? = null
    private var message: EditText? = null
    private var progress: ProgressBar? = null
    private var chat_panel: LinearLayout? = null

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
    private var p_name: TextView? = null
    private var p_date: TextView? = null
    private var p_text: TextView? = null
    private var line: View? = null

    private val send_listener = View.OnClickListener {
        val s = message!!.text.toString()
        if (!s.trim { it <= ' ' }.isEmpty()) {
            text = s

            sendMessage()
            message!!.text = null
        }
    }

    private val record_listener = View.OnClickListener { }

    private//user
    //chat
    // group
    //channel
    val subtitleStatus: String
        get() {
            var type = 2
            if (peerId > 2000000000) type = 0
            if (peerId < -1) type = 1

            if (conversation != null) {
                if (conversation!!.isChannel) {
                    type = 3
                }
            }

            when (type) {
                0 -> return if (membersCount > 0)
                    getString(R.string.members_count, membersCount)
                else
                    ""
                1 -> return getString(R.string.group)
                2 -> {
                    currentUser = CacheStorage.getUser(peerId)
                    return getUserSubtitle(currentUser)
                }
                3 -> return getString(R.string.channel) + " • " + getString(R.string.members_count, membersCount)
                else -> return ""
            }
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
            if (chat_panel!!.background != null) {
                chat_panel!!.background.setColorFilter(ThemeManager.getBackground(), PorterDuff.Mode.MULTIPLY)
            }

        layoutManager = LinearLayoutManager(this)
        layoutManager!!.stackFromEnd = true
        layoutManager!!.orientation = RecyclerView.VERTICAL

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.title = title

        recycler!!.layoutManager = layoutManager

        message!!.addTextChangedListener(this)

        subtitle = subtitleStatus

        getCachedMessages()

        if (Utils.hasConnection()) {
            getMessages(0)
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
    }

    fun onItemClick(v: View, i: Int, item: VKMessage) {
        if (item.out) {
            showOutDialog(i)
        } else {
            showInDialog(i)
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
        p_name = pnd!!.findViewById(R.id.name)
        p_date = pnd!!.findViewById(R.id.date)
        p_text = pnd!!.findViewById(R.id.message)
        unpin = pnd!!.findViewById(R.id.unpin)
        line = findViewById(R.id.line)

        p_name!!.typeface = FontHelper.getFont(FontHelper.PS_BOLD)

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

        p_name!!.text = user!!.toString()
        p_date!!.text = Utils.dateFormatter.format(pinned.date * 1000)

        p_text!!.text = pinned.text

        unpin!!.visibility = if (conversation!!.can_change_pin) View.VISIBLE else View.GONE
        unpin!!.setOnClickListener { showConfirmUnpinDialog() }

        if ((pinned.attachments != null || !ArrayUtil.isEmpty(pinned.fwd_messages)) && TextUtils.isEmpty(pinned.text)) {

            val body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages)

            val r = "<b>$body</b>"
            val span = SpannableString(Html.fromHtml(r))
            span.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorAccent)), 0, body.length, 0)

            p_text!!.append(span)
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
        msg.id = adapter!!.getItem(adapter!!.itemCount - 1).id + 1
        msg.date = Calendar.getInstance().timeInMillis
        msg.out = true
        msg.status = VKMessage.STATUS_SENDING
        msg.isAdded = true
        msg.randomId = Random().nextLong()

        adapter!!.values.add(msg)
        adapter!!.notifyItemInserted(adapter!!.itemCount - 1)
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
                send!!.setOnClickListener(record_listener)
        } else {
            send!!.setImageResource(R.drawable.md_send)
            if (!editing)
                send!!.setOnClickListener(send_listener)
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
                getMessages(0)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        chat_panel = findViewById(R.id.chat_panel)
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

    private fun createAdapter(messages: ArrayList<VKMessage>) {
        if (adapter != null) {
            adapter!!.changeItems(messages)
            adapter!!.notifyDataSetChanged()
        } else {
            adapter = MessageAdapter(this, messages, peerId)
            recycler!!.adapter = adapter
            recycler!!.smoothScrollToPosition(adapter!!.itemCount + 1)
        }
    }

    private fun insertMessages(messages: ArrayList<VKMessage>) {
        if (adapter != null) {
            adapter!!.insert(messages)
            adapter!!.notifyItemRangeChanged(0, messages.size)
        }
    }

    private fun getCachedMessages() {
        val messages = CacheStorage.getMessages(peerId)
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages)
        }
    }

    private fun getUserSubtitle(user: VKUser?): String {
        return if (user != null) {
            if (user.online) {
                getString(R.string.online)
            } else {
                String.format(getString(if (user.sex == VKUser.Sex.MALE) R.string.last_seen_m else R.string.last_seen_w), Utils.dateFormatter.format(user.last_seen * 1000))
            }
        } else ""

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
                users = VKApi.users().get().userIds(ids).fields("photo_100,photo_50,photo_200,online,last_seen").execute(VKUser::class.java)
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

    private fun getMessages(offset: Int) {
        loading = true
        supportActionBar!!.setSubtitle(getString(R.string.loading))
        ThreadExecutor.execute(object : AsyncCallback(this) {

            var messages: ArrayList<VKMessage>? = null
            var users: ArrayList<VKUser>? = null

            @Throws(Exception::class)
            override fun ready() {
                messages = VKApi.messages().history.peerId(peerId.toLong()).extended(true).offset(offset).count(MESSAGES_COUNT).execute(VKMessage::class.java)
            }

            override fun done() {
                messages!!.reverse()
                if (offset == 0) {
                    CacheStorage.deleteMessages(peerId)
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)
                    createAdapter(messages!!)
                    recycler!!.smoothScrollToPosition(adapter!!.itemCount)
                } else {
                    insertMessages(messages!!)
                }

                checkMessagesCount()

                loading = messages!!.isEmpty()
                if (!messages!!.isEmpty()) {
                    getUsers(messages!!)
                }

                supportActionBar!!.setSubtitle(subtitleStatus)
            }

            override fun error(e: Exception) {
                supportActionBar!!.setSubtitle(subtitleStatus)
                e.printStackTrace()
            }

        })

        if (offset != 0) {
            return
        }

        if (peerId > 2000000000 || VKGroup.isGroupId(peerId)) {
            return
        }

        ThreadExecutor.execute(object : AsyncCallback(this) {

            var users: ArrayList<VKUser>? = null

            @Throws(Exception::class)
            override fun ready() {
                users = ArrayList()

                val user = VKApi.users().get().userId(peerId).fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java)[0]
                users!!.add(user)
            }

            override fun done() {
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                supportActionBar!!.subtitle = subtitleStatus
            }

            override fun error(e: Exception) {
                Toast.makeText(this@MessagesActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
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
        val adb = AlertDialog.Builder(this)

        val items = arrayOf(getString(R.string.pin_message), getString(R.string.edit), getString(R.string.delete))

        val listener = DialogInterface.OnClickListener { d, i ->
            when (i) {
                0 -> showConfirmPinDialog(position)
                1 -> edit(position)
                2 -> showDeleteOutDialog(position)
            }
        }

        if (!editing)
            DialogHelper.create(adb, items, listener)
    }

    fun showDeleteInDialog(position: Int) {

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

    fun showDeleteOutDialog(position: Int) {
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
        adb.setPositiveButton(R.string.yes) { p1, p2 -> deleteMessage(position, entries[0], null) }

        DialogHelper.create(adb, every, values, click)
    }

    companion object {

        private val MESSAGES_COUNT = 60
    }

}
