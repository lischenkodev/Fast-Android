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
import ru.melodin.fast.fragment.FragmentConversations
import ru.melodin.fast.fragment.FragmentFriends
import ru.melodin.fast.fragment.FragmentItems
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*

class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener {

    private val fragmentConversations = FragmentConversations()
    private val fragmentFriends = FragmentFriends()
    private val fragmentItems = FragmentItems()

    private var selectedId = -1
    private var selectedFragment: BaseFragment? = null

    private var paused: Boolean = false
    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtil.applyWindowStyles(window, ThemeManager.primaryDark)
        setTheme(ThemeManager.currentTheme)
        VKApi.config = UserConfig.restore()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

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

    override fun onPause() {
        super.onPause()
        running = false
    }

    override fun onResume() {
        super.onResume()
        running = true

        if (paused && UserConfig.isLoggedIn) {
            try {
                startLongPoll()
                runOnUiThread { paused = false }
            } catch (e: Exception) {
                e.printStackTrace()
                paused = true
            }

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_id", selectedId)
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        val key = data[0] as String
        if (key == ThemeManager.KEY_THEME_UPDATE) {
            applyStyles()
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
            if (running)
                startLongPoll()
            else
                paused = true

            if (savedInstanceState == null) {
                selectedFragment = fragmentConversations
                selectedId = R.id.conversations
            } else
                return

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
        if (AppGlobal.preferences.getBoolean("isCrashed", false)) {
            val trace = AppGlobal.preferences.getString("crashLog", "")
            AppGlobal.preferences.edit().putBoolean("isCrashed", false).putString("crashLog", "").apply()


            if (!AppGlobal.preferences.getBoolean(FragmentSettings.KEY_SHOW_ERROR, false))
                return

            val adb = AlertDialog.Builder(this)
            adb.setTitle(R.string.warning)

            adb.setMessage(R.string.app_crashed)
            adb.setPositiveButton(android.R.string.ok, null)
            adb.setNeutralButton(R.string.show_error) { _, _ ->
                val adb1 = AlertDialog.Builder(this@MainActivity)
                adb1.setTitle(R.string.error_log)
                adb1.setMessage(trace)
                adb1.setPositiveButton(android.R.string.ok, null)
                adb1.setNeutralButton(R.string.copy) { _, _ -> Util.copyText(trace!!) }
                adb1.create().show()
            }

            adb.create().show()
        }
    }

    private fun replaceFragment(fragment: Fragment?) {
        if (fragment == null) return

        val manager = supportFragmentManager
        val transaction = manager.beginTransaction()

        val fragments = manager.fragments
        val classNames = ArrayList<String>(fragments.size)

        val FRAGMENT_CONTAINER = R.id.fragment_container

        if (ArrayUtil.isEmpty(fragments)) {
            transaction.add(FRAGMENT_CONTAINER, fragment, fragment.javaClass.simpleName)
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
                transaction.add(FRAGMENT_CONTAINER, fragment, fragment.javaClass.simpleName)
            }
        }
        transaction.commit()
    }

    public override fun onDestroy() {
        super.onDestroy()
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
        if (selectedFragment != null)
            selectedFragment!!.scrollToTop()
    }
}