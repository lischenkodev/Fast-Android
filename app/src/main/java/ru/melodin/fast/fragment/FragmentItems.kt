package ru.melodin.fast.fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_items.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.LoginActivity
import ru.melodin.fast.R
import ru.melodin.fast.SettingsActivity
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.concurrent.LowThread
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.view.FastToolbar


class FragmentItems : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = UserConfig.getUser()

        userName.text = user.toString()

        if (!TextUtils.isEmpty(user!!.photo200))
            LowThread {
                activity!!.runOnUiThread {
                    Picasso.get()
                            .load(user.photo200)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(R.drawable.avatar_placeholder)
                            .into(userAvatar)
                }
            }.start()


        userOnline.visibility = View.VISIBLE
        userOnline.setImageDrawable(getOnlineIndicator(user))

        tb.inflateMenu(R.menu.fragment_items)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.menu) {
                    startActivity(Intent(context, SettingsActivity::class.java))
                }
            }

        })

        logout.setOnClickListener { showExitDialog() }
    }

    private fun getOnlineIndicator(user: VKUser): Drawable? {
        return when {
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
            startLoginActivity()
            activity!!.stopService(Intent(activity!!, LongPollService::class.java))
            UserConfig.clear()
            DatabaseHelper.getInstance().dropTables(AppGlobal.database)
            DatabaseHelper.getInstance().onCreate(AppGlobal.database)
        }
        adb.setNegativeButton(R.string.no, null)
        adb.create().show()
    }

    private fun startLoginActivity() {
        startActivity(Intent(activity!!, LoginActivity::class.java))
        activity!!.finish()
    }
}
