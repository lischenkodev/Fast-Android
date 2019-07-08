package ru.melodin.fast

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_chat_info.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKChat
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.database.CacheStorage.getChat
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*

class ChatInfoActivity : BaseActivity() {

    private var title: String? = null
    private var photo: String? = null
    private var peerId: Int = -1

    private var chat: VKChat? = null

    private var adapter: UserAdapter? = null

    private lateinit var conversation: VKConversation

    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_info)
        getIntentData()

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        chatTitle.setText(title)
        chatTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTitle = chatTitle.text.toString().trim()
                if (!TextUtils.isEmpty(newTitle)) {
                    changeTitleName(newTitle)
                }

                true
            } else
                false
        }

        tb.setTitle(R.string.chat)
        tb.setBackVisible(true)

        loadAvatar()
        setMembersCount(conversation.membersCount)

        getCachedChat()
        if (Util.hasConnection()) getChat()
    }

    private fun getCachedChat() {
        chat = getChat(VKConversation.toChatId(peerId))

        chat ?: return

        chatTitle.setText(chat!!.title)
        loadAvatar()
        createAdapter(chat!!.users)
    }

    private fun getChat() {
        TaskManager.loadChat(VKConversation.toChatId(peerId), VKUser.FIELDS_DEFAULT, object : TaskManager.OnCompleteListener {

            override fun onComplete(models: ArrayList<*>?) {
                if (ArrayUtil.isEmpty(models)) return

                chat = models?.get(0) as VKChat

                chatTitle.setText(chat!!.title)
                loadAvatar()
                createAdapter(chat!!.users)
            }

            override fun onError(e: Exception) {
            }

        })
    }

    private fun createAdapter(items: ArrayList<VKUser>) {
        if (adapter == null) {
            adapter = UserAdapter(this, items, true, conversation)
            recyclerView.adapter = adapter
            return
        }

        adapter!!.changeItems(items)
        adapter!!.notifyDataSetChanged()
    }

    private fun setMembersCount(count: Int) {
        chatMembers.text = if (count > 0)
            resources.getQuantityString(R.plurals.members, count, count)
        else
            string(R.string.no_members)
    }

    private fun changeTitleName(title: String) {
        if (timer != null) return

        val setter = VKApi.messages().editChat().chatId(VKConversation.toChatId(peerId)).title(title)

        TaskManager.addProcedure(setter, Int::class.java, object : TaskManager.OnCompleteListener {

            override fun onComplete(models: ArrayList<*>?) {
                ViewUtil.hideKeyboard(chatTitle)
                chatTitle.clearFocus()
                Toast.makeText(this@ChatInfoActivity, R.string.title_changed, Toast.LENGTH_SHORT).show()

                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        timer = null
                    }
                }, 3000)
            }

            override fun onError(e: Exception) {}

        }, null)
    }

    private fun loadAvatar() {
        if (!TextUtils.isEmpty(title)) {
            TaskManager.execute {
                runOnUiThread {
                    Picasso.get()
                            .load(if (chat == null) photo else chat!!.photo200)
                            .placeholder(R.drawable.avatar_placeholder)
                            .into(chatAvatar)
                }
            }
        }
    }

    private fun getIntentData() {
        conversation = intent.getSerializableExtra("conversation") as VKConversation
        title = intent.getStringExtra("title")
        photo = intent.getStringExtra("photo")
        peerId = intent.getIntExtra("peer_id", -1)
    }

    fun confirmKick(position: Int) {

    }
}
