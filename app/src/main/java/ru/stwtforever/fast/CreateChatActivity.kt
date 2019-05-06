package ru.stwtforever.fast

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ru.stwtforever.fast.adapter.CreateChatAdapter
import ru.stwtforever.fast.adapter.RecyclerAdapter
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.database.CacheStorage
import ru.stwtforever.fast.database.DatabaseHelper
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Requests
import ru.stwtforever.fast.util.Utils
import ru.stwtforever.fast.util.ViewUtils
import java.util.*

class CreateChatActivity : AppCompatActivity(), RecyclerAdapter.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private var loading: Boolean = false
    private var noItems: View? = null

    private var tb: Toolbar? = null
    private var list: RecyclerView? = null
    private var refreshLayout: SwipeRefreshLayout? = null

    private var adapter: CreateChatAdapter? = null

    private var isSelecting: Boolean = false

    override fun onRefresh() {
        loadFriends(0, 0)
    }

    override fun onItemClick(v: View, position: Int) {
        adapter!!.setSelected(position, !adapter!!.isSelected(position))
        adapter!!.notifyItemChanged(position)
        setTitle()

        isSelecting = adapter!!.selectedCount > 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_chat)
        initViews()

        getFriends()
    }

    override fun onBackPressed() {
        if (isSelecting) {
            adapter!!.clearSelect()
            isSelecting = false
        } else
            super.onBackPressed()

        setTitle()
    }

    private fun getFriends() {
        getCachedFriends()
    }

    private fun getCachedFriends() {
        val users = CacheStorage.getFriends(UserConfig.userId, false)

        if (ArrayUtil.isEmpty(users)) {
            loadFriends(0, 0)
            return
        }

        createAdapter(users, 0)
    }

    private fun createAdapter(users: ArrayList<VKUser>, offset: Int) {
        if (ArrayUtil.isEmpty(users))
            return

        if (offset != 0) {
            adapter!!.changeItems(users)
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, null)
            return
        }

        if (adapter != null) {
            adapter!!.changeItems(users)
            adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, null)
            return
        }

        adapter = CreateChatAdapter(this, users)
        list!!.adapter = adapter
        adapter!!.setOnItemClickListener(this)

        tb!!.setOnClickListener { list!!.scrollToPosition(0) }
    }

    private fun loadFriends(offset: Int, count: Int) {
        if (!Utils.hasConnection()) {
            refreshLayout!!.isRefreshing = false
            return
        }

        setTitle()

        refreshLayout!!.isRefreshing = true
        ThreadExecutor.execute(object : AsyncCallback(this) {

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

                setTitle()
            }

            override fun error(e: Exception) {
                setTitle()
                refreshLayout!!.isRefreshing = false
                Toast.makeText(this@CreateChatActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setTitle() {
        val title = getString(R.string.select_friends)
        val subtitle: String


        val selected = if (adapter == null) 0 else adapter!!.selectedCount

        if (selected > 0) {
            subtitle = String.format(getString(R.string.selected_count), selected.toString())
        } else {
            subtitle = ""
        }

        tb!!.title = title
        tb!!.subtitle = subtitle
    }

    private fun initViews() {
        noItems = findViewById(R.id.no_items_layout)

        tb = findViewById(R.id.tb)
        setTitle()

        setSupportActionBar(tb)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        refreshLayout = findViewById(R.id.refresh)
        refreshLayout!!.setOnRefreshListener(this)
        refreshLayout!!.setColorSchemeColors(ThemeManager.getAccent())
        refreshLayout!!.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground())

        list = findViewById(R.id.list)

        val manager = LinearLayoutManager(this)
        manager.orientation = RecyclerView.VERTICAL

        list!!.setHasFixedSize(true)
        list!!.layoutManager = manager
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.create -> getUsers()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getUsers() {
        val items = adapter!!.selectedPositions

        val values = items.values

        val users = ArrayList(values)

        createChat(users)
    }

    private fun createChat(users: ArrayList<VKUser>) {
        val user = UserConfig.getUser()
        users.add(0, user)

        val b = Bundle()
        b.putSerializable("users", users)

        startActivityForResult(Intent(this, ShowCreateChatActivity::class.java).putExtras(b), Requests.CREATE_CHAT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Requests.CREATE_CHAT) {
            if (resultCode == Activity.RESULT_OK) {
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_create_chat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.create).icon.setTint(ViewUtils.mainColor)
        return super.onPrepareOptionsMenu(menu)
    }
}
