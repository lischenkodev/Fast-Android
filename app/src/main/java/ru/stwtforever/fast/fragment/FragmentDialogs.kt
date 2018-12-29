package ru.stwtforever.fast.fragment

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.greenrobot.eventbus.EventBus
import ru.stwtforever.fast.CreateChatActivity
import ru.stwtforever.fast.MainActivity
import ru.stwtforever.fast.MessagesActivity
import ru.stwtforever.fast.R
import ru.stwtforever.fast.adapter.DialogAdapter
import ru.stwtforever.fast.adapter.RecyclerAdapter
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKConversation
import ru.stwtforever.fast.api.model.VKGroup
import ru.stwtforever.fast.api.model.VKMessage
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.cls.BaseFragment
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.db.CacheStorage
import ru.stwtforever.fast.db.DatabaseHelper
import ru.stwtforever.fast.db.MemoryCache
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils
import ru.stwtforever.fast.util.ViewUtils
import java.util.*

class FragmentDialogs : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private var refreshLayout: SwipeRefreshLayout? = null

    private var list: RecyclerView? = null

    private var tb: Toolbar? = null

    private var adapter: DialogAdapter? = null

    private var loading: Boolean = false

    override fun onRefresh() {
        getDialogs(0, DIALOGS_COUNT)
    }

    override fun onDestroy() {
        if (adapter != null) {
            adapter!!.destroy()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.title = getString(R.string.fragment_messages)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list = view.findViewById(R.id.list)
        setRecyclerView(list)
        tb = view.findViewById(R.id.tb)

        ViewUtils.applyToolbarStyles(tb!!)

        tb!!.title = title

        tb!!.inflateMenu(R.menu.fragment_dialogs_menu)
        tb!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create_chat -> startActivity(Intent(activity, CreateChatActivity::class.java))
            }
            true
        }

        for (i in 0 until tb!!.menu.size()) {
            val item = tb!!.menu.getItem(i)
            item.icon.setTint(ViewUtils.mainColor)
        }


        refreshLayout = view.findViewById(R.id.refresh)
        refreshLayout!!.setColorSchemeColors(ThemeManager.getAccent())
        refreshLayout!!.setOnRefreshListener(this)
        refreshLayout!!.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground())

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list!!.setHasFixedSize(true)
        list!!.layoutManager = manager

        getMessages()

        initSearchView()
    }

    private fun initSearchView() {
        val search = tb!!.menu.findItem(R.id.search)

        val searchView: SearchView = search!!.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter!!.isEditing = true
                adapter!!.filter(newText!!.toLowerCase())
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!searchView.isIconified)
                    searchView.isIconified = true

                adapter!!.isEditing = false
                search.collapseActionView()
                return false
            }

        })

    }

    private fun showDialog(position: Int) {
        val features = arrayOf(getString(R.string.clean_history))

        val item = adapter!!.values[position]
        val group = CacheStorage.getGroup(VKGroup.toGroupId(item.last.peerId))
        val user = CacheStorage.getUser(item.last.peerId)

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(adapter!!.getTitle(item, user, group))
        adb.setItems(features, object : DialogInterface.OnClickListener {

            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    0 -> showDeleteConfirmDialog()
                }
            }

            private fun showDeleteConfirmDialog() {
                val adb = AlertDialog.Builder(activity!!)
                adb.setTitle(R.string.warning)
                adb.setMessage(R.string.confirm_delete_dialog_message)
                adb.setPositiveButton(R.string.yes, object : DialogInterface.OnClickListener {

                    override fun onClick(dialog: DialogInterface, which: Int) {
                        deleteDialog()
                    }

                    private fun deleteDialog() {
                        ThreadExecutor.execute(object : AsyncCallback(activity) {
                            internal var response = -1

                            @Throws(Exception::class)
                            override fun ready() {
                                val m = adapter!!.values[position]
                                response = VKApi.messages().deleteConversation().peerId(m.last.peerId.toLong()).execute(Int::class.java)[0]
                            }

                            override fun done() {
                                adapter!!.values.removeAt(position)
                                adapter!!.notifyDataSetChanged()
                            }

                            override fun error(e: Exception) {
                                Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show()
                            }
                        })
                    }

                })
                adb.setNegativeButton(R.string.no, null)
                adb.show()
            }
        })
        adb.show()
    }

    private fun getMessages() {
        getCachedDialogs()
        getDialogs(0, DIALOGS_COUNT)
    }

    private fun createAdapter(messages: ArrayList<VKConversation>?, offset: Int) {
        if (ArrayUtil.isEmpty(messages)) {
            return
        }
        if (offset != 0) {
            adapter!!.changeItems(messages)
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, adapter!!.getItem(adapter!!.itemCount - 1))
            return
        }

        if (adapter != null) {
            adapter!!.changeItems(messages)
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, adapter!!.getItem(adapter!!.itemCount - 1))
            return
        }
        adapter = messages?.let { DialogAdapter(activity, it) }
        list!!.adapter = adapter
        adapter!!.setOnItemClickListener(this)
        adapter!!.setOnItemLongClickListener(this)
    }

    private fun getCachedDialogs() {
        val dialogs = CacheStorage.getDialogs()
        if (ArrayUtil.isEmpty(dialogs)) {
            return
        }

        createAdapter(dialogs, 0)
    }

    private fun getDialogs(offset: Int, count: Int) {
        if (!Utils.hasConnection()) {
            refreshLayout!!.isRefreshing = false
            return
        }

        refreshLayout!!.isRefreshing = true
        ThreadExecutor.execute(object : AsyncCallback(activity) {
            private lateinit var conversations: ArrayList<VKConversation>

            @Throws(Exception::class)
            override fun ready() {
                conversations = VKApi.messages().conversations
                        .filter("all")
                        .extended(true)
                        .fields(VKUser.FIELDS_DEFAULT)
                        .offset(offset)
                        .count(count)
                        .execute(VKConversation::class.java)

                if (conversations.isEmpty()) {
                    loading = true
                }

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE)
                    CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, conversations)
                }

                val users = conversations[0].profiles
                val groups = conversations[0].groups
                val messages = ArrayList<VKMessage>()

                for (i in conversations.indices) {
                    val last = conversations[i].last
                    messages.add(last)
                }

                if (!ArrayUtil.isEmpty(messages))
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages)

                if (!ArrayUtil.isEmpty(users))
                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)

                if (!ArrayUtil.isEmpty(groups))
                    CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, conversations[0].groups)
            }

            override fun done() {
                EventBus.getDefault().postSticky(MemoryCache.getUser(UserConfig.userId))
                createAdapter(conversations, offset)
                refreshLayout!!.isRefreshing = false

                if (!conversations.isEmpty()) {
                    loading = false
                }
            }

            override fun error(e: Exception) {
                refreshLayout!!.isRefreshing = false
            }
        })
    }

    private fun openChat(position: Int) {
        val c = adapter!!.values[position]
        val user = CacheStorage.getUser(c.last.peerId)
        val g = CacheStorage.getGroup(VKGroup.toGroupId(c.last.peerId))

        val intent = Intent(activity, MessagesActivity::class.java)
        intent.putExtra("title", adapter!!.getTitle(c, user, g))
        intent.putExtra("photo", adapter!!.getPhoto(c, user, g))
        intent.putExtra("conversation", c)
        intent.putExtra("peer_id", c.last.peerId)
        intent.putExtra("can_write", c.can_write)

        if (!c.can_write) {
            intent.putExtra("reason", c.reason)
        }

        startActivity(intent)
    }

    override fun onItemClick(v: View, position: Int) {
        openChat(position)
        val conversation = adapter!!.getItem(position)

        if (!conversation.read && !Utils.getPrefs().getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
            readMessage(position)
        }
    }

    private fun readMessage(position: Int) {
        ThreadExecutor.execute(object : AsyncCallback(activity) {
            @Throws(Exception::class)
            override fun ready() {
                VKApi.messages().markAsRead().peerId(adapter!!.getItem(position).last.peerId.toLong()).execute(Int::class.java)
            }

            override fun done() {}

            override fun error(e: Exception) {

            }
        })
    }

    fun onBackPressed(activity: MainActivity) {
        val adapter = list!!.adapter as DialogAdapter
        if (adapter.isEditing) {
            adapter.isEditing = false
            val search = tb!!.menu.findItem(R.id.search)
            search.collapseActionView()
        } else {
            activity.backPressed()
        }
    }

    override fun onItemLongClick(v: View, position: Int) {
        showDialog(position)
    }

    companion object {


        private const val DIALOGS_COUNT = 60
    }
}


