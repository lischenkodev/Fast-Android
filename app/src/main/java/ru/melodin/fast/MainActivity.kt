package ru.melodin.fast

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.fragment.FragmentConversations
import ru.melodin.fast.fragment.FragmentFriends
import ru.melodin.fast.fragment.FragmentItems
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*

class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener {

    private lateinit var fragmentConversations: FragmentConversations
    private lateinit var fragmentFriends: FragmentFriends
    private lateinit var fragmentItems: FragmentItems

    private var selectedId = -1
    private var selectedFragment: BaseFragment? = null

    private var fromRecreate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtil.applyWindowStyles(window, ThemeManager.primaryDark)
        setTheme(ThemeManager.currentTheme)
        VKApi.config = UserConfig.restore()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initFragments()

        navigationView.selectedItemId = R.id.conversations
        navigationView.setOnNavigationItemSelectedListener(this)
        navigationView.setOnNavigationItemReselectedListener(this)

        checkLogin(savedInstanceState)
        checkCrash()

        if (UserConfig.isLoggedIn && savedInstanceState == null) {
            trackVisitor()
        }

        EventBus.getDefault().register(this)
    }

    private fun initFragments() {
        fragmentConversations = FragmentConversations()
        fragmentFriends = FragmentFriends()
        fragmentItems = FragmentItems()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_id", selectedId)
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            ThemeManager.KEY_THEME_UPDATE -> {
                fromRecreate = true
                applyStyles()
            }
            Keys.AUTHORIZATION_FAILED -> {
                val helper = DatabaseHelper.getInstance(this)
                val db = AppGlobal.database

                helper.dropTables(db)
                helper.onCreate(db)
                finishAffinity()
            }
        }
    }

    private fun startLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun checkLogin(savedInstanceState: Bundle?) {
        UserConfig.restore()
        if (!UserConfig.isLoggedIn) {
            startLoginActivity()
        } else {
            if (savedInstanceState != null) return

            startLongPoll()

            selectedFragment = fragmentConversations
            selectedId = R.id.conversations


            replaceFragment(selectedFragment)
        }
    }

    private fun startLongPoll() {
        startService(Intent(this, LongPollService::class.java))
    }

    private fun trackVisitor() {
        if (!Util.hasConnection() || BuildConfig.DEBUG) return
        TaskManager.execute {
            try {
                VKApi.stats().trackVisitor().execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkCrash() {
        if (AppGlobal.preferences.getBoolean(FragmentSettings.KEY_CRASHED, false)) {
            val trace = AppGlobal.preferences.getString(FragmentSettings.KEY_CRASH_LOG, "")
            AppGlobal.preferences.edit()
                    .putBoolean(FragmentSettings.KEY_CRASHED, false)
                    .putString(FragmentSettings.KEY_CRASH_LOG, "")
                    .apply()


            if (!AppGlobal.preferences.getBoolean(FragmentSettings.KEY_SHOW_ERROR, false))
                return

            val adb = AlertDialog.Builder(this)
            adb.setTitle(R.string.warning)

            adb.setMessage(R.string.app_crashed)
            adb.setPositiveButton(android.R.string.ok, null)
            adb.setNeutralButton(R.string.show_error) { _, _ ->
                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle(R.string.error_log)
                builder.setMessage(trace)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setNeutralButton(R.string.copy) { _, _ -> Util.copyText(trace!!) }
                builder.show()
            }

            adb.show()
        }
    }

    private fun replaceFragment(fragment: Fragment?) {
        if (fragment == null) return

        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()

        val fragments = manager.fragments
        val classNames = ArrayList<String>(fragments.size)

        val containerViewId = R.id.fragment_container

        if (ArrayUtil.isEmpty(fragments)) {
            transaction.add(containerViewId, fragment, fragment.javaClass.simpleName)
        } else {
            for (f in fragments) {
                transaction.hide(f)
                classNames.add(f.javaClass.simpleName)
            }

            if (classNames.contains(fragment.javaClass.simpleName)) {
                for (f in fragments)
                    if (f.javaClass.simpleName == fragment.javaClass.simpleName) {
                        transaction.show(f)
                        break
                    }
            } else {
                transaction.add(containerViewId, fragment, fragment.javaClass.simpleName)
            }
        }
        transaction.commit()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (!fromRecreate) {
            stopService(Intent(this, LongPollService::class.java))
        }

        EventBus.getDefault().unregister(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        selectedId = item.itemId
        when (item.itemId) {
            R.id.conversations -> selectedFragment = fragmentConversations
            R.id.friends -> selectedFragment = fragmentFriends
            R.id.menu -> selectedFragment = fragmentItems
        }

        replaceFragment(selectedFragment)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        selectedFragment?.scrollToTop()
    }
}
