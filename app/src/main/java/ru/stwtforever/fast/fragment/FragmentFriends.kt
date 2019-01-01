package ru.stwtforever.fast.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ru.stwtforever.fast.MessagesActivity2
import ru.stwtforever.fast.R
import ru.stwtforever.fast.adapter.FriendAdapter
import ru.stwtforever.fast.adapter.RecyclerAdapter
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKConversation
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.cls.BaseFragment
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.database.CacheStorage
import ru.stwtforever.fast.database.DatabaseHelper
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils
import java.util.*

class FragmentFriends : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemLongClickListener, RecyclerAdapter.OnItemClickListener {

    private var list: RecyclerView? = null
    private var refreshLayout: SwipeRefreshLayout? = null

    private var adapter: FriendAdapter? = null

    private var tb: Toolbar? = null

    private var loading: Boolean = false

    private var noItems: View? = null

    override fun onRefresh() {
        loadFriends(0, 0)
    }

    override fun onItemClick(v: View, position: Int) {

    }

    override fun onItemLongClick(v: View, position: Int) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        this.title = getString(R.string.fragment_friends)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tb = view.findViewById(R.id.tb)
        noItems = view.findViewById(R.id.no_items_layout)

        tb!!.title = title

        list = view.findViewById(R.id.list)
        setRecyclerView(list)

        refreshLayout = view.findViewById(R.id.refresh)
        refreshLayout!!.setOnRefreshListener(this)
        refreshLayout!!.setColorSchemeColors(ThemeManager.getAccent())
        refreshLayout!!.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground())

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list!!.setHasFixedSize(true)
        list!!.layoutManager = manager

        getFriends()
    }

    private fun getFriends() {
        getCachedFriends()
        loadFriends(0, 0)
    }

    private fun getCachedFriends() {
        val users = CacheStorage.getFriends(UserConfig.userId, false)

        if (ArrayUtil.isEmpty(users))
            return

        createAdapter(users, 0)
    }

    private fun loadFriends(count: Int, offset: Int) {
        if (!Utils.hasConnection()) {
            refreshLayout!!.isRefreshing = false
            return
        }

        refreshLayout!!.isRefreshing = true

        ThreadExecutor.execute(object : AsyncCallback(activity) {

            var users: ArrayList<VKUser>? = null

            @Throws(Exception::class)
            override fun ready() {
                users = VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java)

                if (users!!.isEmpty()) {
                    loading = true
                }

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE)
                    CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, users)
                }

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)
            }

            override fun done() {
                createAdapter(users!!, offset)
                refreshLayout!!.isRefreshing = false

                if (!users!!.isEmpty()) {
                    loading = false
                }

                val count: Int = adapter!!.getItem(0)!!.friends_count

                tb!!.title = "$title" + if (adapter!!.itemCount == 0) "" else " ($count)"
            }

            override fun error(e: Exception) {
                refreshLayout!!.isRefreshing = false
                Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun checkCount() {
        noItems!!.visibility = when {
            adapter == null -> View.VISIBLE
            adapter!!.itemCount == 0 -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun createAdapter(users: ArrayList<VKUser>, offset: Int) {
        checkCount()

        if (ArrayUtil.isEmpty(users)) {
            return
        }

        checkCount()

        val isEmpty: Boolean = if (adapter == null) false else adapter!!.itemCount == 0

        if (offset != 0) {
            adapter!!.changeItems(users)
            if (isEmpty)
                adapter!!.notifyItemRangeInserted(0, adapter!!.itemCount)
            else
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount)
            return
        }

        checkCount()

        if (adapter != null) {
            adapter!!.changeItems(users)
            if (isEmpty)
                adapter!!.notifyItemRangeInserted(0, adapter!!.itemCount)
            else
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount)
            return
        }

        adapter = FriendAdapter(this, users)
        list!!.adapter = adapter
        adapter!!.setOnItemClickListener(this)
        adapter!!.setOnItemLongClickListener(this)

        checkCount()
    }

    fun openChat(position: Int) {
        val user = adapter!!.values[position]

        val intent = Intent(activity, MessagesActivity2::class.java)
        intent.putExtra("title", user.toString())
        intent.putExtra("photo", user.photo_200)
        intent.putExtra("peer_id", user.id)

        val canWrite = !user.isDeactivated

        intent.putExtra("can_write", canWrite)

        if (!canWrite) {
            intent.putExtra("reason", VKConversation.REASON_USER_BLOCKED_DELETED)
        }

        startActivity(intent)
    }

    fun showDialog(position: Int, v: View) {
        val m = androidx.appcompat.widget.PopupMenu(activity!!, v)
        m.inflate(R.menu.fragment_friends_funcs)
        m.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.remove_friend -> showConfirmDeleteFriend(position)
            }
            true
        }
        m.show()
    }

    private fun showConfirmDeleteFriend(position: Int) {
        val adb = AlertDialog.Builder(context!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.confirm_delete_friend)
        adb.setPositiveButton(R.string.yes) { dialog, which -> deleteFriend(position) }
        adb.setNegativeButton(R.string.no, null)
        adb.create().show()
    }

    private fun deleteFriend(position: Int) {
        ThreadExecutor.execute(object : AsyncCallback(activity) {

            @Throws(Exception::class)
            override fun ready() {
                VKApi.friends().delete().userId(adapter!!.getItem(position).getId()).execute(Int::class.java)
            }

            override fun done() {
                adapter!!.remove(position)
                adapter!!.notifyItemRemoved(position)
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, adapter!!.getItem(adapter!!.itemCount - 1))
            }

            override fun error(e: Exception) {
                Toast.makeText(activity, getString(R.string.error) + "!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
