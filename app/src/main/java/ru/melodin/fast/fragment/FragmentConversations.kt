package ru.melodin.fast.fragment

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
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.ConversationAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKGroup
import ru.melodin.fast.api.model.VKMessage
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.mvp.contract.ConversationsContract
import ru.melodin.fast.mvp.presenter.ConversationsPresenter
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar
import java.util.*

class FragmentConversations : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
    RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener,
    ConversationsContract.View, FastToolbar.OnMenuItemClickListener {

    private var adapter: ConversationAdapter? = null
    private var bundle: Bundle? = null

    private val presenter = ConversationsPresenter()

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()

        presenter.detachView()
        presenter.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)

        title = getString(R.string.fragment_messages)
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
        tb.setOnMenuItemClickListener(this)

        ViewUtil.applyToolbarMenuItemsColor(tb)

        refreshLayout.setColorSchemeColors(ThemeManager.ACCENT)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.PRIMARY)

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        presenter.viewIsReady()

        getCachedConversations(30, 0)

        if (savedInstanceState == null && !AppGlobal.preferences.getBoolean(
                FragmentSettings.KEY_OFFLINE,
                false
            ) && Util.hasConnection()
        ) {
            loadConversations(CONVERSATIONS_COUNT, 0)
        } else {
            setProgressBarVisible(false)
            setRefreshing(false)
            if (adapter != null && adapter!!.isEmpty)
                setNoItemsViewVisible(true)
        }

        if (adapter != null && list?.adapter == null) {
            list?.adapter = adapter
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.create_chat -> {
                parent!!.replaceFragment(
                    0,
                    FragmentCreateChat(),
                    null,
                    true
                )
                true
            }
            R.id.clear_messages_cache -> {
                context ?: return false
                FragmentSettings.showConfirmClearCacheDialog(
                    context = context!!,
                    users = false,
                    groups = false
                )
                true
            }
            else -> false
        }
    }

    override fun setNoItemsViewVisible(visible: Boolean) {
        when (visible) {
            true -> {
                emptyView.visibility = View.VISIBLE
                setProgressBarVisible(false)
            }
            else -> emptyView.visibility = View.GONE
        }
    }

    override fun showNoInternetToast() {
        setRefreshing(false)
        Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorToast() {
        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
    }

    override fun setRefreshing(value: Boolean) {
        when (value) {
            true -> {
                refreshLayout.isRefreshing = true
                setProgressBarVisible(false)
            }
            else -> refreshLayout.isRefreshing = false
        }
    }

    override fun createAdapter(items: ArrayList<VKConversation>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) return

        if (adapter == null) {
            adapter = ConversationAdapter(this, items!!)
            adapter!!.setOnItemClickListener(this)
            adapter!!.setOnItemLongClickListener(this)

            list.adapter = adapter
            list.scrollToPosition(0)
            return
        }

        if (offset != 0) {
            adapter!!.values!!.addAll(items!!)
            adapter!!.notifyDataSetChanged()
            return
        }

        adapter!!.changeItems(items!!)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }

    override fun getCachedConversations(count: Int, offset: Int) {
        val conversations = CacheStorage.conversations ?: return
        conversations.reverse()

        if (!ArrayUtil.isEmpty(conversations)) {
            setRefreshing(true)
            setNoItemsViewVisible(false)
            createAdapter(conversations, 0)
        } else {
            setProgressBarVisible(true)
        }
    }

    override fun loadConversations(count: Int, offset: Int) {
        if (!Util.hasConnection()) {
            showNoInternetToast()
            return
        }

        if (bundle != null)
            setRefreshing(true)

        TaskManager.execute {
            VKApi.messages().conversations
                .filter("all")
                .extended(true)
                .fields(VKUser.FIELDS_DEFAULT + "," + VKGroup.FIELDS_DEFAULT)
                .count(count)
                .offset(offset)
                .execute(VKConversation::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val conversations = models as ArrayList<VKConversation>
                        conversations.reverse()

                        CacheStorage.delete(DatabaseHelper.CONVERSATIONS_TABLE)
                        CacheStorage.insert(
                            DatabaseHelper.CONVERSATIONS_TABLE,
                            conversations as ArrayList<*>
                        )

                        val users = VKConversation.users
                        val groups = VKConversation.groups
                        val messages = ArrayList<VKMessage>()

                        for (conversation in conversations) {
                            conversation.lastMessage ?: continue
                            messages.add(conversation.lastMessage!!)
                        }

                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
                        CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups)
                        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)

                        conversations.reverse()

                        createAdapter(conversations, offset)

                        presenter.onFilledList()
                    }

                    override fun onError(e: Exception) {
                        setRefreshing(false)
                        showErrorToast()
                    }
                })
        }
    }

    override fun clearList() {
        adapter ?: return
        adapter!!.clear()
        adapter!!.notifyDataSetChanged()
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

    private fun openChat(conversation: VKConversation) {
        val args = Bundle().apply {
            putInt("peer_id", conversation.peerId)
            putString("title", conversation.fullTitle)
            putString("photo", conversation.photo)
            putBoolean("can_write", conversation.isCanWrite)
            putSerializable("conversation", conversation)
        }

        if (!conversation.isCanWrite) {
            args.putInt("reason", conversation.reason)
        }

        parent!!.replaceFragment(0, FragmentMessages(), args, true)
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
        loadConversations(CONVERSATIONS_COUNT, 0)
    }

    override fun onItemClick(position: Int) {
        val conversation = adapter!!.getItem(position)

        openChat(conversation)

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

        if (conversation.lastMessage == null || conversation.lastMessageId == -1 || conversation.lastMessage!!.isOut || conversation.isRead || conversation.lastMessage!!.isRead) {
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
            setRefreshing(false)
            return
        }

        setRefreshing(true)

        TaskManager.execute {
            VKApi.messages()
                .deleteConversation()
                .peerId(peerId)
                .execute(null, object : OnResponseListener {
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

                        adapter!!.remove(position)
                        adapter!!.notifyItemRemoved(position)
                        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)

                        setRefreshing(false)
                    }

                    override fun onError(e: Exception) {
                        setRefreshing(true)
                        Log.e("Error delete dialog", Log.getStackTraceString(e))
                        Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    companion object {
        private const val CONVERSATIONS_COUNT = 30

        const val REQUEST_CREATE_CHAT = 1
    }
}
