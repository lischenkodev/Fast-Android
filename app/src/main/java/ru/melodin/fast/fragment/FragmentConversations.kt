package ru.melodin.fast.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.CreateChatActivity
import ru.melodin.fast.MainActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.ConversationAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnCompleteListener
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKGroup
import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.FragmentSelector
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.FastToolbar
import java.util.*

class FragmentConversations : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
    RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private var adapter: ConversationAdapter? = null
    private var bundle: Bundle? = null

    private var chooseConversation = false

    var isLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.fragment_messages))

        chooseConversation = savedInstanceState?.getBoolean("choose_conversation")!!
    }

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bundle = savedInstanceState

        toolbar = tb
        recyclerList = list

        tb.setTitle(title)

        tb.inflateMenu(R.menu.fragment_dialogs_menu)
        tb.setItemVisible(0, false)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.create_chat) {
                    startActivity(Intent(activity, CreateChatActivity::class.java))
                }
            }
        })

        refresh.setColorSchemeColors(ThemeManager.accent)
        refresh.setOnRefreshListener(this)
        refresh.setProgressBackgroundColorSchemeColor(ThemeManager.primary)

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        getCachedConversations()

        if (savedInstanceState == null && !AppGlobal.preferences.getBoolean(
                FragmentSettings.KEY_OFFLINE,
                false
            )
        )
            getConversations(CONVERSATIONS_COUNT, 0)

        if (adapter != null && list?.adapter == null) {
            list?.adapter = adapter
        }
    }

    private fun createAdapter(conversations: ArrayList<VKConversation>?) {
        if (ArrayUtil.isEmpty(conversations)) return

        if (adapter == null) {
            adapter = ConversationAdapter(this, conversations!!)
            adapter!!.setOnItemClickListener(this)
            adapter!!.setOnItemLongClickListener(this)
            list.adapter = adapter
            list.scrollToPosition(0)
            return
        }

        adapter!!.changeItems(conversations!!)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }

    override fun onResume() {
        super.onResume()
        onHiddenChanged(false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden)
            (activity!! as MainActivity).showBottomView()
    }

    private fun getCachedConversations() {
        val conversations = CacheStorage.conversations

        if (!ArrayUtil.isEmpty(conversations)) {
            createAdapter(conversations)
        }
    }

    private fun getConversations(count: Int, offset: Int) {
        if (isLoading) return

        if (!Util.hasConnection()) {
            refresh!!.isRefreshing = false
            Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        if (bundle != null)
            refresh!!.isRefreshing = true

        TaskManager.execute {
            VKApi.messages().conversations
                .filter("all")
                .extended(true)
                .fields(VKUser.FIELDS_DEFAULT + ", " + VKGroup.FIELDS_DEFAULT)
                .count(count)
                .offset(offset)
                .execute(VKConversation::class.java, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val conversations = models as ArrayList<VKConversation>

                        CacheStorage.delete(DatabaseHelper.CONVERSATIONS_TABLE)
                        CacheStorage.insert(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversations as ArrayList<*>
                        )

                        val users = VKConversation.users
                        val groups = VKConversation.groups
                        val messages = ArrayList<VKMessage>()

                        for (conversation in conversations) {
                            conversation.last ?: continue
                            messages.add(conversation.last!!)
                        }

                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                        CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups)
                        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)

                        createAdapter(conversations)
                        refresh!!.isRefreshing = false

                        isLoading = false
                    }

                    override fun onError(e: Exception) {
                        isLoading = false
                        refresh!!.isRefreshing = false
                        Toast.makeText(
                            context,
                            getString(R.string.error) + ": " + e.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun openChat(position: Int) {
        val conversation = adapter!!.getItem(position)

        val peerId = conversation.peerId

        val args = Bundle().apply {
            putString("title", conversation.fullTitle)
            putString("photo", conversation.photo)
            putInt("peer_id", peerId)
            putBoolean("can_write", conversation.isCanWrite)
            putSerializable("conversation", conversation)
        }

        if (!conversation.isCanWrite) {
            args.putInt("reason", conversation.reason)
        }

        FragmentSelector.selectFragment(fragmentManager!!, FragmentMessages(), args, true)
    }

    private fun readConversation(peerId: Int) {
        if (!Util.hasConnection()) return
        TaskManager.execute {
            try {
                VKApi.messages().markAsRead().peerId(peerId).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onRefresh() {
        getConversations(CONVERSATIONS_COUNT, 0)
    }

    override fun onItemClick(position: Int) {

        openChat(position)

        val conversation = adapter!!.getItem(position)

        if (!conversation.isRead && !AppGlobal.preferences.getBoolean(
                FragmentSettings.KEY_NOT_READ_MESSAGES,
                false
            )
        ) {
            readConversation(conversation.peerId)
        }
    }

    override fun onItemLongClick(position: Int) {
        showAlert(position)
    }

    private fun showAlert(position: Int) {
        val adb = AlertDialog.Builder(activity!!)

        val conversation = adapter!!.getItem(position)

        val peerId = conversation.peerId

        adb.setTitle(conversation.fullTitle)

        val list = arrayListOf<String>(*resources.getStringArray(R.array.conversation_functions))
        val remove = ArrayList<String>()

        if (conversation.last == null || conversation.lastMessageId == -1 || conversation.last!!.isOut || conversation.isRead || conversation.last!!.isRead) {
            remove.add(getString(R.string.read))
        }

        list.removeAll(remove)

        val items = arrayOfNulls<String>(list.size)
        for (i in list.indices)
            items[i] = list[i]

        adb.setItems(items) { _, i ->
            val title = items[i]

            if (title == getString(R.string.clear_messages_history)) {
                showConfirmDeleteConversation(position)
            } else if (title == getString(R.string.read)) {
                readConversation(peerId)
            }
        }
        adb.show()
    }

    private fun showConfirmDeleteConversation(position: Int) {
        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.are_you_sure)
        adb.setPositiveButton(R.string.yes) { _, _ -> deleteConversation(position) }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun deleteConversation(position: Int) {
        val conversation = adapter!!.getItem(position)

        val peerId = conversation.peerId

        if (!Util.hasConnection() || peerId == -1 || peerId == 0) {
            refresh!!.isRefreshing = false
            return
        }

        refresh!!.isRefreshing = true

        TaskManager.execute {
            VKApi.messages()
                .deleteConversation()
                .peerId(peerId)
                .execute(null, object : OnCompleteListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        CacheStorage.delete(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            DatabaseHelper.PEER_ID,
                            peerId
                        )
                        adapter!!.remove(position)
                        adapter!!.notifyItemRemoved(position)
                        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                        refresh!!.isRefreshing = false
                    }

                    override fun onError(e: Exception) {
                        refresh!!.isRefreshing = false
                        Log.e("Error delete dialog", Log.getStackTraceString(e))
                        Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    companion object {
        private const val CONVERSATIONS_COUNT = 30
    }
}
