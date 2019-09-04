package ru.melodin.fast

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.common.*
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.fragment.FragmentConversations
import ru.melodin.fast.fragment.FragmentItems
import ru.melodin.fast.fragment.FragmentMessages
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import java.util.*


open class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var fromRecreate: Boolean = false
    private var currentId = -1
    private var currentFragment: BaseFragment? = null

    private var stacks: SparseArray<Stack<Fragment>> = SparseArray()

    private lateinit var queue: ArrayList<String>

    var goBack = true

    companion object {
        val fragmentConversations = FragmentConversations()
        val fragmentItems = FragmentItems()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.CURRENT_THEME)
        VKApi.config = UserConfig.restore()
        super.onCreate(savedInstanceState)
        initStacks()
        setContentView(R.layout.activity_main)

        navigationView.selectedItemId = R.id.navigation_conversations
        navigationView.setOnNavigationItemSelectedListener(this)

        checkLogin(savedInstanceState)
        CrashManager.checkCrash(this)

        if (UserConfig.isLoggedIn && savedInstanceState == null) {
            trackVisitor()
        }

        EventBus.getDefault().register(this)

        if (!BuildConfig.DEBUG) {
            AppCenter.start(
                application,
                "bd53321b-546a-4579-82fb-c68edb4feb20",
                Analytics::class.java
            )
        }

        AppCenter.start(
            application,
            "bd53321b-546a-4579-82fb-c68edb4feb20",
            Crashes::class.java
        )

        Distribute.setEnabledForDebuggableBuild(true)
        AppCenter.start(
            application,
            "bd53321b-546a-4579-82fb-c68edb4feb20",
            Distribute::class.java
        )
    }

    private fun initStacks() {
        queue = ArrayList()
        stacks.put(R.id.navigation_conversations, Stack())
        stacks.put(R.id.navigation_items, Stack())
    }

    private fun spliceStack(id: Int, size: Int) {
        stacks.get(id).setSize(size)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.THEME_UPDATE -> {
                fromRecreate = true
                applyStyles()
            }
            Keys.AUTHORIZATION_FAILED -> {
                val helper = DatabaseHelper.getInstance(this)
                val db = AppGlobal.database

                helper.dropTables(db)
                helper.onCreate(db)
                UserConfig.clear()

                stopService(Intent(this, LongPollService::class.java))
                finish()
                startActivity(Intent(this, LoginActivity::class.java))
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

            currentFragment = fragmentConversations
            currentId = R.id.navigation_conversations

            replaceFragment(R.id.navigation_conversations, fragmentConversations, null)
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

    override fun onDestroy() {
        super.onDestroy()
        if (!fromRecreate) {
            stopService(Intent(this, LongPollService::class.java))
        }

        EventBus.getDefault().unregister(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == currentId) {
            if (stacks.get(item.itemId).size > 1) {
                spliceStack(item.itemId, 1)
                showFragment(stacks.get(item.itemId).lastElement(), null)
            } else {
                val fragment = stacks.get(item.itemId).lastElement()
                if (fragment is BaseFragment)
                    fragment.scrollToTop()
            }
            return true
        }

        currentId = item.itemId

        if (stacks.get(item.itemId).size > 0) {
            queue.remove(item.itemId.toString())
            queue.add(item.itemId.toString())
            showFragment(stacks.get(item.itemId).lastElement(), null)
            return true
        }

        return when (item.itemId) {
            R.id.navigation_conversations -> {
                replaceFragment(item.itemId, fragmentConversations, null)
                true
            }
            R.id.navigation_items -> {
                replaceFragment(item.itemId, fragmentItems, null)
                true
            }
            else -> false
        }
    }

    fun replaceFragment(navId: Int, fragment: Fragment, arguments: Bundle?) {
        replaceFragment(navId, fragment, arguments, false)
    }

    fun replaceFragment(
        navId: Int,
        fragment: Fragment,
        arguments: Bundle?,
        showOnce: Boolean = false
    ) {
        var id = navId
        id = if (id == 0) currentId else id

        if (id != currentId) {
            navigationView.menu.findItem(id).isChecked = true
        }

        stacks.get(id).push(fragment)
        queue.remove(id.toString())
        queue.add(id.toString())

        arguments?.apply {
            putBoolean("top", stacks.get(id).size == 1)
        }

        showFragment(fragment, arguments, showOnce)
        checkBottomViewVisible(fragment)
    }

    private fun checkBottomViewVisible(fragment: Fragment) {
        if (fragment is BaseFragment) {
            if (fragment.isBottomViewVisible()) {
                showBottomView()
            } else {
                hideBottomView()
            }
        }
    }

    private fun showFragment(fragment: Fragment, arguments: Bundle?) {
        showFragment(fragment, arguments, false)
    }

    private fun showFragment(fragment: Fragment, arguments: Bundle?, showOnce: Boolean) {
        FragmentSelector.currentFragment = fragment
        FragmentSelector.selectFragment(supportFragmentManager, fragment, arguments, showOnce)
    }

    override fun onBackPressed() {
        if (currentFragment is FragmentMessages && !goBack) {
            currentFragment!!.onBackPressed()
        } else {
            stacks.get(currentId).pop()
            if (stacks.get(currentId).empty()) {
                queue.remove(currentId.toString())
                if (queue.size == 0) {
                    super.onBackPressed()
                    return
                }
                currentId = Integer.valueOf(queue[queue.size - 1])
            }

            val fragment = stacks.get(currentId).lastElement()

            showFragment(fragment, null)
            navigationView.menu.findItem(currentId).isChecked = true

            checkBottomViewVisible(fragment)
        }
    }

    private fun showBottomView() {
        navigationView.visibility = View.VISIBLE
    }

    private fun hideBottomView() {
        navigationView.visibility = View.GONE
    }
}