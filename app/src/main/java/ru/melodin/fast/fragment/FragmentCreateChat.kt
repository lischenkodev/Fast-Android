package ru.melodin.fast.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.CreateChatAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.mvp.contract.UsersContract
import ru.melodin.fast.mvp.presenter.UsersPresenter
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util

class FragmentCreateChat : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
    RecyclerAdapter.OnItemClickListener, UsersContract.View {

    private var adapter: CreateChatAdapter? = null

    private var selecting: Boolean = false

    private val presenter = UsersPresenter()

    override fun onRefresh() {
        loadUsers(0, 0)
    }

    override fun onItemClick(position: Int) {
        adapter!!.toggleSelected(position)
        adapter!!.notifyItemChanged(position, -1)
        changeTitle()

        selecting = adapter!!.selectedCount > 0
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
        return inflater.inflate(R.layout.activity_create_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tb.setTitle(R.string.select_friends)

        tb.setNavigationIcon(R.drawable.ic_arrow_back)
        tb.setNavigationOnClickListener { onBackPressed() }

        tb.inflateMenu(R.menu.activity_create_chat)
        tb.setOnMenuItemClickListener {
            if (it.itemId == R.id.create) {
                getUsers()
                return@setOnMenuItemClickListener true
            }

            false
        }

        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setColorSchemeColors(ThemeManager.ACCENT)
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.BACKGROUND)

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)

        getCachedUsers(0, 0)
    }

    override fun onBackPressed() {
        if (selecting) {
            adapter!!.clearSelect()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
            selecting = false

            changeTitle()
        } else
            super.onBackPressed()
    }

    override fun createAdapter(items: ArrayList<VKUser>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) {
            setNoItemsViewVisible(true)
            return
        }

        setNoItemsViewVisible(false)

        if (adapter == null) {
            adapter = CreateChatAdapter(activity!!, items!!)
            adapter!!.setOnItemClickListener(this)
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

    override fun getCachedUsers(count: Int, offset: Int) {
        val users = CacheStorage.getFriends(UserConfig.userId, false)

        if (ArrayUtil.isEmpty(users)) {
            loadUsers(0, 0)
        } else {
            setRefreshing(false)
            setProgressBarVisible(false)
            setNoItemsViewVisible(false)
            createAdapter(users, offset)
        }
    }

    override fun loadUsers(count: Int, offset: Int) {
        if (!Util.hasConnection()) {
            setRefreshing(false)
            showNoInternetToast()

            if (adapter == null || adapter!!.isEmpty) {
                setNoItemsViewVisible(true)
            }
            return
        }

        changeTitle()

        setRefreshing(true)
        TaskManager.execute {

            lateinit var friends: ArrayList<VKUser>

            VKApi.friends().get().userId(UserConfig.userId).order("hints")
                .fields(VKUser.FIELDS_DEFAULT)
                .count(count)
                .offset(offset)
                .execute(VKUser::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        friends = models as ArrayList<VKUser>
                        CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE)
                        CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, friends)
                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, friends)

                        createAdapter(friends, 0)
                        refreshLayout.isRefreshing = false

                        changeTitle()
                    }

                    override fun onError(e: Exception) {
                        changeTitle()
                        setRefreshing(false)
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

    override fun showNoInternetToast() {
        Toast.makeText(activity!!, R.string.connect_to_the_internet, Toast.LENGTH_LONG)
            .show()
    }

    override fun showErrorToast() {
        Toast.makeText(activity!!, R.string.error, Toast.LENGTH_LONG).show()
    }

    private fun changeTitle() {
        adapter ?: return
        val selected = adapter!!.selectedCount

        val subtitle = if (selected > 0) String.format(
            getString(R.string.selected_count),
            selected.toString()
        ) else ""

        tb.setSubtitle(subtitle)
    }

    private fun getUsers() {
        val items = adapter!!.selectedPositions

        if (items != null) {
            val users = ArrayList<VKUser>(items.size())
            for (i in 0 until items.size()) {
                users.add(items.valueAt(i))
            }

            createChat(users)
        }
    }

    private fun createChat(users: ArrayList<VKUser>) {
        val user = UserConfig.getUser()
        users.add(0, user!!)

        parent!!.replaceFragment(
            0,
            FragmentCreateChatUsers().apply {
                setTargetFragment(this@FragmentCreateChat, REQUEST_CREATE_CHAT)
            },
            Bundle().apply {
                putSerializable("users", users)
            },
            true
        )

        adapter!!.clearSelect()
        adapter!!.notifyDataSetChanged()
        changeTitle()
    }

    private fun openChat(title: String?, peerId: Int) {
        val membersCount = adapter!!.selectedCount + 1

        parent!!.onBackPressed()
        parent!!.replaceFragment(
            0, FragmentMessages(), Bundle().apply {
                putString("title", title)
                putInt("peer_id", peerId)
                putInt("members_count", membersCount)
            },
            true
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CREATE_CHAT) {
            if (resultCode == Activity.RESULT_OK) {
                data ?: return
                val title = data.getStringExtra("title")
                val peerId = data.getIntExtra("peer_id", -1)

                openChat(title, peerId)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        const val REQUEST_CREATE_CHAT = 1

    }
}
