package ru.melodin.fast.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.LoginActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.ItemAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.CrashManager
import ru.melodin.fast.common.FragmentSelector
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.model.ListItem
import ru.melodin.fast.model.ShadowPaddingItem
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Constants
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar


class FragmentItems : BaseFragment(), RecyclerAdapter.OnItemClickListener {

    private var user: VKUser? = null

    private val fragmentSettings = FragmentSettings()
    private val fragmentFriends = ParentFragmentFriends()

    companion object {
        const val ID_FRIENDS = 0
        const val ID_GROUPS = 1
        const val ID_REPORT_BUG = 254
        const val ID_LOGOUT = 255

        fun showExitDialog(activity: Activity?) {
            activity ?: return
            val adb = AlertDialog.Builder(activity)
            adb.setTitle(R.string.warning)
            adb.setMessage(R.string.exit_message)
            adb.setPositiveButton(R.string.yes) { _, _ ->
                val helper = DatabaseHelper.getInstance(activity)
                val db = AppGlobal.database

                helper.dropTables(db)
                helper.onCreate(db)
                UserConfig.clear()

                activity.stopService(Intent(activity, LongPollService::class.java))
                activity.finish()
                activity.startActivity(Intent(activity, LoginActivity::class.java))
            }

            adb.setNegativeButton(R.string.no, null)
            adb.create().show()
        }
    }

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
        ViewUtil.applyToolbarMenuItemsColor(tb)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                if (item.itemId == R.id.item_settings) {
                    parent?.replaceFragment(0, fragmentSettings, null)
                    return true
                }

                return false
            }
        })

        EventBus.getDefault().register(this)

        createItems()
    }

    private fun createItems() {
        val items = arrayListOf(
            ShadowPaddingItem(),
            ListItem(ID_FRIENDS, string(R.string.fragment_friends), drawable(R.drawable.md_people)),
            ListItem(ID_GROUPS, string(R.string.groups), drawable(R.drawable.md_groups)),
            ShadowPaddingItem(),
            ListItem(
                ID_REPORT_BUG,
                string(R.string.report_bug),
                drawable(R.drawable.ic_bug_report_black_24dp)
            ),
            ShadowPaddingItem(),
            ListItem(ID_LOGOUT, string(R.string.logout), drawable(R.drawable.md_exit_to_app))
        )

        val adapter = ItemAdapter(activity!!, items)
        adapter.setOnItemClickListener(this)
        list.adapter = adapter
    }

    override fun onItemClick(position: Int) {
        val item = (list.adapter as ItemAdapter).getItem(position)

        when (item.id) {
            ID_FRIENDS -> parent?.replaceFragment(
                0,
                fragmentFriends,
                null
            )
            ID_GROUPS -> Toast.makeText(activity!!, R.string.in_progress, Toast.LENGTH_LONG).show()
            ID_REPORT_BUG -> showReportDialog()
            ID_LOGOUT -> showExitDialog(activity)
        }
    }

    private fun showReportDialog() {
        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(R.string.warning)
        builder.setMessage(R.string.are_you_sure)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            reportBug()
        }
        builder.setNegativeButton(R.string.no, null)
        builder.show()
    }

    private fun reportBug() {
        FragmentSelector.selectFragment(fragmentManager!!, FragmentMessages(), Bundle().apply {
            putInt("peer_id", Constants.BOT_ID)
            putString("text", "#bug\n\n${CrashManager.getInfo(null)}\n\nDescription: ")
        }, true)
    }

    private fun initUser() {
        if (user == null) user = UserConfig.getUser() ?: return

        if (!TextUtils.isEmpty(user!!.photo200)) {
            Picasso.get()
                .load(user!!.photo200)
                .priority(Picasso.Priority.HIGH)
                .placeholder(R.drawable.avatar_placeholder)
                .into(userAvatar)
        } else {
            userAvatar.setImageResource(R.drawable.avatar_placeholder)
        }

        userName.text = user.toString()

        userOnline.visibility = View.VISIBLE
        userOnline.setImageDrawable(UserAdapter.getOnlineIndicator(activity!!, user!!))
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
}
