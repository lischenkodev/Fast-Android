package ru.melodin.fast

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.adapter.CreateChatAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar
import java.util.*

class CreateChatActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener {

    private var adapter: CreateChatAdapter? = null

    private var selecting: Boolean = false

    override fun onRefresh() {
        loadFriends()
    }

    override fun onItemClick(position: Int) {
        adapter!!.toggleSelected(position)
        adapter!!.notifyItemChanged(position, -1)
        setTitle()

        selecting = adapter!!.selectedCount > 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.currentTheme)
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)

        setTitle()

        tb.setBackVisible(true)
        tb.setOnBackClickListener(View.OnClickListener { onBackPressed() })
        tb.inflateMenu(R.menu.activity_create_chat)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.create)
                    getUsers()
            }

        })

        refresh.setOnRefreshListener(this)
        refresh.setColorSchemeColors(ThemeManager.accent)
        refresh.setProgressBackgroundColorSchemeColor(ThemeManager.background)

        val manager = LinearLayoutManager(this)
        manager.orientation = RecyclerView.VERTICAL

        list.setHasFixedSize(true)
        list.layoutManager = manager

        getCachedFriends()
    }

    override fun onBackPressed() {
        if (selecting) {
            adapter!!.clearSelect()
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
            selecting = false

            setTitle()
        } else
            super.onBackPressed()
    }

    private fun getCachedFriends() {
        val users = CacheStorage.getFriends(UserConfig.userId, false)

        if (ArrayUtil.isEmpty(users)) {
            loadFriends()
            return
        }

        createAdapter(users)
    }

    private fun createAdapter(users: ArrayList<VKUser>) {
        if (ArrayUtil.isEmpty(users)) return

        if (adapter == null) {
            adapter = CreateChatAdapter(this, users)
            adapter!!.setOnItemClickListener(this)
            list!!.adapter = adapter
            return
        }

        adapter!!.changeItems(users)
        adapter!!.notifyDataSetChanged()
    }

    private fun loadFriends() {
        if (!Util.hasConnection()) {
            refresh.isRefreshing = false
            return
        }

        setTitle()

        refresh.isRefreshing = true
        TaskManager.execute {

            lateinit var friends: ArrayList<VKUser>

            VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java, object : VKApi.OnResponseListener {
                override fun onSuccess(models: ArrayList<*>?) {
                    if (ArrayUtil.isEmpty(models)) return
                    models?: return

                    friends = models as ArrayList<VKUser>
                    CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE)
                    CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, friends)
                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, friends)

                    createAdapter(friends)
                    refresh.isRefreshing = false

                    setTitle()
                }

                override fun onError(e: Exception) {
                    setTitle()
                    refresh.isRefreshing = false
                    Toast.makeText(this@CreateChatActivity, R.string.error, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun setTitle() {
        val selected = if (adapter == null) 0 else adapter!!.selectedCount

        val title = getString(R.string.select_friends)
        val subtitle = if (selected > 0) String.format(getString(R.string.selected_count), selected.toString()) else ""

        tb.setTitle(title)
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

        val intent = Intent(this, ShowCreateChatActivity::class.java)
        intent.putExtra("users", users)

        startActivityForResult(intent, REQUEST_CREATE_CHAT)

        adapter!!.clearSelect()
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
        setTitle()
    }

    private fun openChat(title: String?, peerId: Int) {
        val intent = Intent(this, MessagesActivity::class.java)

        intent.putExtra("title", title)
        intent.putExtra("peer_id", peerId)
        intent.putExtra("can_write", true)

        finish()
        startActivity(intent)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
