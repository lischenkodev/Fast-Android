package ru.melodin.fast.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.MessagesActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import java.util.*

class FragmentFriends : BaseFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var adapter: UserAdapter? = null

    var isLoading: Boolean = false

    override fun onRefresh() {
        getFriends(FRIENDS_COUNT, 0)
    }

    override fun onDestroy() {
        if (adapter != null) adapter!!.destroy()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(getString(R.string.fragment_friends))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar = tb
        recyclerView = list

        tb.setTitle(title)

        refresh.setOnRefreshListener(this)
        refresh.setColorSchemeColors(ThemeManager.accent)
        refresh.setProgressBackgroundColorSchemeColor(ThemeManager.primary)

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list.setHasFixedSize(true)
        list.layoutManager = manager

        getCachedFriends()
        if (savedInstanceState == null)
            getFriends(FRIENDS_COUNT, 0)
    }

    private fun createAdapter(friends: ArrayList<VKUser>?, offset: Int) {
        friends ?: return
        if (ArrayUtil.isEmpty(friends)) return

        if (adapter == null) {
            adapter = UserAdapter(this, friends)
            list!!.adapter = adapter
            return
        }

        if (offset != 0) {
            adapter!!.values!!.addAll(friends)
            adapter!!.notifyItemRangeInserted(adapter!!.itemCount - friends.size - 1, friends.size)
            return
        }

        adapter!!.changeItems(friends)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }

    private fun getCachedFriends() {
        val users = CacheStorage.getFriends(UserConfig.userId, false)

        if (!ArrayUtil.isEmpty(users))
            createAdapter(users, 0)
    }

    private fun getFriends(count: Int, offset: Int) {
        if (isLoading) return
        if (!Util.hasConnection()) {
            refresh.isRefreshing = false
            Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true

        refresh.isRefreshing = true

        TaskManager.execute {

            lateinit var users: ArrayList<VKUser>

                VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java, object : VKApi.OnResponseListener {
                    override fun onSuccess(models: ArrayList<*>?) {

                        if (ArrayUtil.isEmpty(models)) return
                        models?: return

                        users = models as ArrayList<VKUser>

                        if (offset == 0) {
                            CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE)
                            CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, users)
                        }

                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)

                        createAdapter(users, offset)
                        isLoading = true
                        refresh.isRefreshing = false
                    }

                    override fun onError(e: Exception) {
                        refresh.isRefreshing = false
                        Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show()

                    }
                })
        }
    }

    fun openChat(position: Int) {
        val user = adapter!!.getItem(position)

        val intent = Intent(activity, MessagesActivity::class.java)
        intent.putExtra("title", user.toString())
        intent.putExtra("photo", user.photo200)
        intent.putExtra("peer_id", user.id)

        val canWrite = !user.isDeactivated

        intent.putExtra("can_write", canWrite)

        if (!canWrite) {
            intent.putExtra("reason", VKConversation.getReason(VKConversation.Reason.USER_DELETED))
        }

        startActivity(intent)
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
        adb.create().show()
    }

    private fun deleteFriend(position: Int) {
        if (!Util.hasConnection()) {
            refresh.isRefreshing = false
            return
        }

        refresh.isRefreshing = true

        val user = adapter!!.getItem(position)
        val userId = user.id
/*
        TaskManager.execute(object : AsyncCallback(activity) {

            @Throws(Exception::class)
            override fun ready() {
                VKApi.friends().delete().userId(userId).execute()
            }

            override fun done() {
                adapter!!.remove(position)
                adapter!!.notifyItemRemoved(position)
                adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
                refresh.isRefreshing = false

                CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE, DatabaseHelper.USER_ID, userId)
            }

            override fun error(e: Exception) {
                Log.e("Error delete friend", Log.getStackTraceString(e))
                refresh.isRefreshing = false
                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })*/
    }

    companion object {
        private const val FRIENDS_COUNT = 30
    }
}
