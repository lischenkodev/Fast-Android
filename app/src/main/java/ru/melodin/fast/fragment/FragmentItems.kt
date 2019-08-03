package ru.melodin.fast.fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.LoginActivity
import ru.melodin.fast.MainActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.ItemsAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.model.ListItem
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.view.FastToolbar


class FragmentItems : BaseFragment(), RecyclerAdapter.OnItemClickListener {

    private var user: VKUser? = null

    private var fragmentSettings = FragmentSettings()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = tb
        recyclerList = list

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)

        initUser()

        tb.inflateMenu(R.menu.fragment_items)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.menu) {
                    (activity!! as MainActivity).replaceFragment(fragmentSettings)
                }
            }
        })

        logout.setOnClickListener { showExitDialog() }

        EventBus.getDefault().register(this)

        createItems()
    }

    private fun createItems() {
        val items = arrayListOf(
            ListItem(ID_FRIENDS, string(R.string.fragment_friends), drawable(R.drawable.md_people)),
            ListItem(ID_GROUPS, string(R.string.groups), drawable(R.drawable.md_groups))
        )

        val adapter = ItemsAdapter(activity!!, items)
        adapter.setOnItemClickListener(this)
        list.adapter = adapter
    }

    override fun onItemClick(position: Int) {
        val item = (list.adapter as ItemsAdapter).getItem(position)

        when (item.id) {
            ID_FRIENDS -> (activity!! as MainActivity).replaceFragment(MainActivity.fragmentFriends)
            ID_GROUPS -> Toast.makeText(activity!!, R.string.in_progress, Toast.LENGTH_LONG).show()
        }
    }

    private fun initUser() {
        if (user == null) user = UserConfig.getUser() ?: return

        userName.text = user.toString()

        userAvatar ?: return

        if (user != null && !TextUtils.isEmpty(user!!.photo200))
            Picasso.get()
                .load(user!!.photo200)
                .priority(Picasso.Priority.HIGH)
                .placeholder(R.drawable.avatar_placeholder)
                .into(userAvatar)

        userOnline.visibility = View.VISIBLE
        userOnline.setImageDrawable(getOnlineIndicator(user))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        when (hidden) {
            true -> (activity!! as MainActivity).hideNavMenu()
            else -> (activity!! as MainActivity).showNavMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        onHiddenChanged(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.USER_ONLINE -> setUserOnline(
                online = true,
                mobile = data[3] as Boolean,
                userId = data[1] as Int,
                time = data[2] as Int
            )
            Keys.USER_OFFLINE -> setUserOnline(
                online = false,
                mobile = false,
                userId = data[1] as Int,
                time = data[2] as Int
            )
        }
    }

    private fun setUserOnline(online: Boolean, mobile: Boolean, userId: Int, time: Int) {
        if (user!!.id == userId) {
            user!!.isOnline = online
            user!!.isOnlineMobile = mobile
            user!!.lastSeen = time.toLong()

            initUser()
        }
    }

    private fun getOnlineIndicator(user: VKUser?): Drawable? {
        return when {
            user == null -> null
            user.isOnlineMobile -> ContextCompat.getDrawable(context!!, R.drawable.ic_online_mobile)
            user.isOnline -> ContextCompat.getDrawable(context!!, R.drawable.ic_online)
            else -> null
        }
    }

    private fun showExitDialog() {
        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(R.string.warning)
        adb.setMessage(R.string.exit_message)
        adb.setPositiveButton(R.string.yes) { _, _ ->
            activity!!.stopService(Intent(activity!!, LongPollService::class.java))
            startActivity(Intent(activity!!, LoginActivity::class.java))
            activity!!.finish()

            TaskManager.execute {
                UserConfig.clear()
                EventBus.getDefault().postSticky(arrayOf<Any>(Keys.AUTHORIZATION_FAILED))
            }
        }
        adb.setNegativeButton(R.string.no, null)
        adb.create().show()
    }

    companion object {
        const val ID_FRIENDS = 0
        const val ID_GROUPS = 1
    }
}
