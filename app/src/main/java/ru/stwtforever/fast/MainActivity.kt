package ru.stwtforever.fast

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.cls.BaseFragment
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.database.DatabaseHelper
import ru.stwtforever.fast.database.MemoryCache
import ru.stwtforever.fast.fragment.FragmentDialogs
import ru.stwtforever.fast.fragment.FragmentFriends
import ru.stwtforever.fast.fragment.FragmentGroups
import ru.stwtforever.fast.fragment.FragmentNavDrawer
import ru.stwtforever.fast.helper.DialogHelper
import ru.stwtforever.fast.helper.PermissionHelper
import ru.stwtforever.fast.service.LongPollService
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils
import ru.stwtforever.fast.util.ViewUtils

class MainActivity : AppCompatActivity() {

    private var bottom_toolbar: View? = null

    private var tb_btn_switch: LinearLayout? = null
    private var messages: ImageButton? = null
    private var friends: ImageButton? = null
    private var filter: ImageButton? = null
    private var menu: ImageButton? = null

    private var bg: GradientDrawable? = null

    private val f_dialogs = FragmentDialogs()
    private val f_friends = FragmentFriends()
    private val f_groups = FragmentGroups()

    private var selected_id = -1

    private val click = View.OnClickListener { v ->
        selected_id = -1
        var f: Fragment? = null

        when (v.id) {
            R.id.tb_messages -> {
                f = f_dialogs
                messages!!.background = bg
                messages!!.drawable.setTint(Color.WHITE)
                friends!!.drawable.setTint(ThemeManager.getAccent())
                friends!!.setBackgroundColor(Color.TRANSPARENT)

                replaceFragment(f_dialogs)
            }
            R.id.tb_friends -> {
                f = f_friends
                friends!!.drawable.setTint(Color.WHITE)
                messages!!.drawable.setTint(ThemeManager.getAccent())
                friends!!.background = bg
                messages!!.setBackgroundColor(Color.TRANSPARENT)

                replaceFragment(f_friends)
            }
        }

        if (visibleFragment === f) {
            (f as BaseFragment).recyclerView.scrollToPosition(0)
        }
    }

    private val visibleFragment: Fragment?
        get() {
            val frs = supportFragmentManager.fragments

            if (ArrayUtil.isEmpty(frs)) return null

            var f: Fragment? = null

            for (fr in frs) {
                if (fr.isVisible) {
                    f = fr
                }
            }

            return f
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        PermissionHelper.init(this)
        ViewUtils.applyWindowStyles(this)
        EventBus.getDefault().register(this)
        setTheme(ThemeManager.getCurrentTheme())

        VKApi.config = UserConfig.restore()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bottom_toolbar = findViewById(R.id.toolbar)

        initToolbar()

        checkLogin()

        startService(Intent(this, LongPollService::class.java))
        checkCrash()

        if (UserConfig.isLoggedIn()) {
            trackVisitor()
        }

        if (!PermissionHelper.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionHelper.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 44)
        }
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun checkLogin() {
        UserConfig.restore()
        if (!UserConfig.isLoggedIn()) {
            startLoginActivity()
        } else {
            replaceFragment(f_dialogs)
        }
    }

    private fun trackVisitor() {
        ThreadExecutor.execute(object : AsyncCallback(this) {

            internal var i: Int = 0

            @Throws(Exception::class)
            override fun ready() {
                i = VKApi.stats().trackVisitor().execute<Int>(Int::class.java)[0]
            }

            override fun done() {}

            override fun error(e: Exception) {
                e.printStackTrace()
            }

        })
    }

    private fun checkCrash() {
        if (Utils.getPrefs().getBoolean("isCrashed", false)) {
            val trace = Utils.getPrefs().getString("crashLog", "")
            Utils.getPrefs().edit().putBoolean("isCrashed", false).putString("crashLog", "").apply()


            val adb = AlertDialog.Builder(this)
            adb.setTitle(R.string.warning)

            adb.setMessage(R.string.app_crashed)
            adb.setPositiveButton(android.R.string.ok, null)
            adb.setNeutralButton(R.string.show_error) { _, _ ->
                val adb = AlertDialog.Builder(this@MainActivity)
                adb.setTitle(R.string.error_log)
                adb.setMessage(trace)
                adb.setPositiveButton(android.R.string.ok, null)
                adb.setNeutralButton(R.string.copy) { _, _ -> Utils.copyText(trace) }
                DialogHelper.create(adb).show()
            }
            val alert = DialogHelper.create(adb)
            alert.show()
        }
    }

    private fun initToolbar() {
        filter = bottom_toolbar!!.findViewById(R.id.tb_filter)
        menu = bottom_toolbar!!.findViewById(R.id.tb_menu)

        filter!!.isEnabled = false
        filter!!.drawable.setTint(Color.GRAY)

        menu!!.setOnClickListener {
            val drawer = FragmentNavDrawer()

            val b = Bundle()
            b.putInt("selected_id", selected_id)

            drawer.arguments = b
            drawer.show(supportFragmentManager, "")
        }

        bg = GradientDrawable()
        bg!!.setColor(ThemeManager.getAccent())
        bg!!.cornerRadius = 200f

        messages = bottom_toolbar!!.findViewById(R.id.tb_messages)
        friends = bottom_toolbar!!.findViewById(R.id.tb_friends)

        messages!!.background = bg
        friends!!.setBackgroundColor(Color.TRANSPARENT)

        friends!!.setOnClickListener(click)
        messages!!.setOnClickListener(click)

        tb_btn_switch = bottom_toolbar!!.findViewById(R.id.tb_icons_switcher)
        tb_btn_switch!!.setBackgroundResource(if (ThemeManager.isDark()) R.drawable.tb_switcher_bg_dark else R.drawable.tb_switcher_bg)
    }

    fun itemClick(item: MenuItem) {
        when (item.itemId) {
            R.id.groups -> selected_id = R.id.groups
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.left -> showExitDialog()
        }
    }

    @Subscribe(sticky = true)
    fun onUpdateUser(u: VKUser) {
        return
    }

    private fun showExitDialog() {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.warning)
        adb.setMessage(R.string.exit_message)
        adb.setPositiveButton(R.string.yes) { dialogInterface, i ->
            UserConfig.clear()
            startLoginActivity()
        }
        adb.setNegativeButton(R.string.no, null)
        val alert = adb.create()
        alert.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        MemoryCache.clear()
        DatabaseHelper.getInstance().close()
    }

    private fun replaceFragment(fragment: Fragment) {
        val tr = supportFragmentManager.beginTransaction()

        val fragments = supportFragmentManager.fragments

        if (ArrayUtil.isEmpty(fragments)) {
            tr.add(R.id.fragment_container, fragment, fragment.javaClass.getSimpleName())
        } else {
            var exists = false

            for (f in fragments) {
                if (f.javaClass.getSimpleName() == fragment.javaClass.getSimpleName()) {
                    tr.show(f)
                    exists = true
                } else {
                    tr.hide(f)
                }
            }

            if (!exists) {
                tr.add(R.id.fragment_container, fragment, fragment.javaClass.getSimpleName())
            }
        }

        tr.commit()
    }
}
