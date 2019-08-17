package ru.melodin.fast.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.FragmentSelector
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.CacheStorage.getFriends
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.mvp.contract.UsersContract
import ru.melodin.fast.mvp.presenter.UsersPresenter
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util

class FragmentFriends(var onlyOnline: Boolean) : BaseFragment(),
    SwipeRefreshLayout.OnRefreshListener,
    UsersContract.View {

    constructor() : this(false)

    private var adapter: UserAdapter? = null

    var isLoading: Boolean = false

    val presenter = UsersPresenter()

    override fun onRefresh() {
        loadUsers(FRIENDS_COUNT, 0)
    }

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()

        presenter.detachView()
        presenter.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
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

        toolbar = tb.apply {
            visibility = View.GONE
        }

        recyclerList = list

        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setColorSchemeColors(ThemeManager.ACCENT)
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.PRIMARY)

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list.setHasFixedSize(true)
        list.layoutManager = manager

        refreshLayout.isEnabled = false

        getCachedUsers(0, 0)
        if (savedInstanceState == null)
            loadUsers(FRIENDS_COUNT, 0)

        if (adapter != null && list?.adapter == null) {
            list?.adapter = adapter
        }
    }

    override fun getCachedUsers(count: Int, offset: Int) {
        val users = getFriends(UserConfig.userId, onlyOnline)

        if (!ArrayUtil.isEmpty(users)) {
            setRefreshing(true)
            setNoItemsViewVisible(false)
            createAdapter(users, 0)
        } else {
            setProgressBarVisible(true)
        }
    }

    override fun loadUsers(count: Int, offset: Int) {
        if (!Util.hasConnection()) {
            showNoInternetToast()
            return
        }

        setRefreshing(true)

        TaskManager.execute {

            VKApi.friends().get().userId(UserConfig.userId).order("hints")
                .fields(VKUser.FIELDS_DEFAULT)
                .execute(VKUser::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val users = models as ArrayList<VKUser>

                        if (offset == 0) {
                            CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE)
                            CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, users)
                        }

                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)

                        createAdapter(sortUsers(users), offset)

                        presenter.onFilledList()
                    }

                    override fun onError(e: Exception) {
                        setRefreshing(false)
                        showErrorToast()
                    }
                })

            refreshLayout.isRefreshing = false
        }
    }

    private fun sortUsers(users: ArrayList<VKUser>): ArrayList<VKUser> {
        if (!onlyOnline) return users
        val newUsers = arrayListOf<VKUser>()

        for (user in users) {
            if ((onlyOnline && user.isOnline)) {
                newUsers.add(user)
            }
        }

        return newUsers
    }

    override fun createAdapter(items: ArrayList<VKUser>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) return

        if (adapter == null) {
            adapter = UserAdapter(this, items!!)
            list!!.adapter = adapter
            return
        }

        if (offset != 0) {
            adapter!!.values!!.addAll(items!!)
            adapter!!.notifyDataSetChanged()
            return
        }

        adapter!!.changeItems(items!!)
        adapter!!.notifyDataSetChanged()
    }


    override fun showNoInternetToast() {
        setRefreshing(false)
        Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorToast() {
        Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show()
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

    override fun setRefreshing(value: Boolean) {
        when (value) {
            true -> {
                refreshLayout.isRefreshing = true
                setProgressBarVisible(false)
            }
            else -> refreshLayout.isRefreshing = false
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

    override fun clearList() {
        adapter ?: return
        adapter!!.clear()
        adapter!!.notifyDataSetChanged()
    }

    fun openChat(position: Int) {
        val user = adapter!!.getItem(position)

        val args = Bundle().apply {
            putString("title", user.toString())
            putString("photo", user.photo200)
            putInt("peer_id", user.id)
        }

        val canWrite = !user.isDeactivated

        args.putBoolean("can_write", canWrite)

        if (!canWrite) {
            args.putInt("reason", VKConversation.getReason(VKConversation.Reason.USER_DELETED))
        }

        FragmentSelector.selectFragment(fragmentManager!!, FragmentMessages(), args, true)
    }

    fun showDialog(position: Int, v: View) {
        val menu = PopupMenu(activity!!, v)
        menu.inflate(R.menu.fragment_friends_funcs)
        menu.setOnMenuItemClickListener {
            showConfirmDeleteFriend(position)
            true
        }
        menu.show()
    }

    private fun showConfirmDeleteFriend(position: Int) {
        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.confirmation)
        adb.setMessage(R.string.confirm_delete_friend)
        adb.setPositiveButton(R.string.yes) { _, _ -> deleteFriend(position) }
        adb.setNegativeButton(R.string.no, null)
        adb.show()
    }

    private fun deleteFriend(position: Int) {
        if (!Util.hasConnection()) {
            refreshLayout.isRefreshing = false
            return
        }

        refreshLayout.isRefreshing = true

        val user = adapter!!.getItem(position)
        val userId = user.id

        TaskManager.execute {
            VKApi.friends().delete().userId(userId).execute(null, object : OnResponseListener {
                override fun onComplete(models: ArrayList<*>?) {
                    adapter!!.remove(position)
                    adapter!!.notifyItemRemoved(position)
                    adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                    refreshLayout.isRefreshing = false

                    CacheStorage.delete(
                        DatabaseHelper.FRIENDS_TABLE,
                        DatabaseHelper.USER_ID,
                        userId
                    )
                }

                override fun onError(e: Exception) {
                    Log.e("Error delete friend", Log.getStackTraceString(e))
                    refreshLayout.isRefreshing = false
                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    fun onReceive(data: Array<Any>) {
        adapter ?: return
        when (data[0] as String) {
            Keys.USER_OFFLINE -> {
                val index = adapter!!.searchUser(data[1] as Int)
                if (index != -1) {
                    adapter!!.remove(index)
                    adapter!!.notifyDataSetChanged()
                }
            }
            Keys.USER_ONLINE -> {
                val user = MemoryCache.getUser(data[1] as Int)
                if (user != null) {
                    adapter!!.add(0, user)
                    adapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    companion object {
        private const val FRIENDS_COUNT = 30
    }
}