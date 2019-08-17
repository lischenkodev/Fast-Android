package ru.melodin.fast.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_chat_info.*
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.eventbus.EventBus
import ru.melodin.fast.R
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKChat
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar


class FragmentChatInfo : BaseFragment() {

    private lateinit var conversation: VKConversation

    private var chat: VKChat? = null

    private var adapter: UserAdapter? = null

    private var items: ArrayList<VKUser>? = null

    override fun isBottomViewVisible(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        conversation = arguments?.getSerializable("conversation") as VKConversation

        recyclerView.layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)

        chatTitle.setText(conversation.title)
        chatTitle.setSelection(chatTitle.text!!.length)
        chatTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                prepareMenu()
            }
        })

        chatTitle.setOnEditorActionListener { _, actionId, _ ->
            chat ?: return@setOnEditorActionListener false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newTitle = chatTitle.text.toString().trim()
                if (!TextUtils.isEmpty(newTitle) && newTitle != chat!!.title) {
                    changeTitleName(newTitle)
                }
            }

            true
        }

        tb.inflateMenu(R.menu.fragment_chat_info)

        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                if (item.itemId == R.id.save_title) {
                    changeTitleName(chatTitle.text.toString().trim())
                    return true
                }
                return false
            }

        })

        tb.setTitle(R.string.chat)
        tb.setBackVisible(true)

        chatLeave.setOnClickListener {
            confirmKick(UserConfig.userId)
        }

        loadAvatar()
        chatAvatar.setOnClickListener {
            showPhotoAlert()
        }

        getCachedChat()
        if (Util.hasConnection()) loadChat()
    }


    private fun showPhotoAlert() {
        val list =
            arrayListOf(*resources.getStringArray(R.array.chat_photo_functions))
        val removeList = arrayListOf<String>()

        list.removeAll(removeList)

        val items = arrayOfNulls<String>(list.size)
        for (i in list.indices)
            items[i] = list[i]

        val builder = AlertDialog.Builder(activity!!)
        builder.setItems(items) { _, i ->
            when (items[i]) {
                getString(R.string.delete) -> showDeletePhotoAlert()
            }
        }

        builder.show()
    }

    private fun showDeletePhotoAlert() {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(R.string.confirmation)
        builder.setMessage(R.string.are_you_sure)
        builder.setNegativeButton(R.string.no, null)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            deletePhoto()
        }
        builder.show()
    }

    private fun deletePhoto() {
        TaskManager.execute {
            VKApi.messages().deleteChatPhoto().chatId(chat!!.id)
                .execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        chatAvatar.setImageResource(R.drawable.avatar_placeholder)

                        chat!!.apply {
                            photo50 = null
                            photo100 = null
                            photo200 = null
                        }

                        saveChat()

                        Toast.makeText(
                            activity!!,
                            R.string.success,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(
                            activity!!,
                            R.string.error,
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                })
        }
    }

    private fun saveChat() {
        CacheStorage.insert(DatabaseHelper.CHATS_TABLE, chat!!)
        EventBus.getDefault().postSticky(arrayOf<Any>(Keys.UPDATE_CHAT, chat!!))
    }

    private fun getCachedChat() {
        chat = CacheStorage.getChat(VKConversation.toChatId(conversation.peerId))
        chat ?: return

        initChat()
    }

    private fun loadChat() {
        TaskManager.loadChat(
            VKConversation.toChatId(conversation.peerId),
            VKUser.FIELDS_DEFAULT,
            object : OnResponseListener {

                override fun onComplete(models: ArrayList<*>?) {
                    if (ArrayUtil.isEmpty(models)) return

                    chat = models?.get(0) as VKChat

                    saveChat()
                    initChat()
                }

                override fun onError(e: Exception) {
                }

            })
    }

    private fun initChat() {
        createAdapter(chat!!.users)
        setMembersCount(chat!!.users.size)
        chatTitle.setText(chat!!.title)
        loadAvatar()
    }

    private fun createAdapter(items: ArrayList<VKUser>) {
        this.items = items

        if (adapter == null) {
            adapter = if (items.size > 30) {
                UserAdapter(this, ArrayList(items.subList(0, 29)), true)
            } else {
                UserAdapter(this, items, true)
            }

            recyclerView.adapter = adapter
            return
        }

        adapter!!.changeItems(items)
        adapter!!.notifyDataSetChanged()
    }

    private fun setMembersCount(count: Int) {
        chatMembers?.text = if (count > 0)
            resources.getQuantityString(R.plurals.members, count, count)
        else
            getString(R.string.no_members)
    }

    private fun changeTitleName(title: String) {
        TaskManager.addProcedure(
            VKApi.messages()
                .editChat()
                .chatId(VKConversation.toChatId(conversation.peerId))
                .title(title), Int::class.java, object : OnResponseListener {

                override fun onComplete(models: ArrayList<*>?) {
                    chat!!.title = title
                    prepareMenu()

                    ViewUtil.hideKeyboard(chatTitle)
                    chatTitle.clearFocus()

                    Toast.makeText(
                        activity!!,
                        R.string.title_changed,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onError(e: Exception) {}

            }, null
        )
    }

    private fun loadAvatar() {
        if (!TextUtils.isEmpty(if (chat == null) conversation.photo200 else chat!!.photo200)) {
            Picasso.get()
                .load(if (chat == null) conversation.photo200 else chat!!.photo200)
                .placeholder(R.drawable.avatar_placeholder)
                .into(chatAvatar)
        }
    }

    fun confirmKick(userId: Int) {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(R.string.confirmation)
        builder.setMessage(R.string.are_you_sure)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            kickUser(userId)
        }
        builder.setNegativeButton(R.string.no, null)
        builder.show()
    }

    private fun kickUser(userId: Int) {
        TaskManager.execute {
            VKApi.messages().removeChatUser().chatId(VKConversation.toChatId(conversation.peerId))
                .userId(userId).execute(null, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (userId == UserConfig.userId) {
                            chat!!.users = arrayListOf()
                            fragmentManager!!.popBackStack()
                        } else {
                            val position = adapter!!.searchUser(userId)
                            adapter!!.remove(position)
                            adapter!!.notifyDataSetChanged()
                            chat!!.users = adapter!!.values!!
                        }

                        CacheStorage.update(
                            DatabaseHelper.CHATS_TABLE,
                            chat!!,
                            DatabaseHelper.CHAT_ID,
                            chat!!.id
                        )
                        Toast.makeText(
                            activity!!,
                            R.string.success,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(
                            activity!!,
                            R.string.error,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                })
        }
    }

    private fun prepareMenu() {
        val menu = tb.menu
        val item = menu.findItem(R.id.save_title)

        item.isVisible =
            if (chat == null) false else chatTitle.text.toString().trim() != chat!!.title
    }
}
