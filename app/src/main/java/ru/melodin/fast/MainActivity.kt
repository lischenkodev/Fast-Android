package ru.melodin.fast

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.FragmentSelector
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.fragment.FragmentConversations
import ru.melodin.fast.fragment.FragmentItems
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.service.LongPollService
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Keys
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil

class MainActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    private var selectedId = -1
    private var selectedFragment: BaseFragment? = null

    private var fromRecreate: Boolean = false

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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_id", selectedId)
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        when (data[0] as String) {
            Keys.KEY_THEME_UPDATE -> {
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


            FragmentSelector.selectFragment(supportFragmentManager, selectedFragment!!)
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
            R.id.menu -> selectedFragment = fragmentItems
        }

        FragmentSelector.selectFragment(supportFragmentManager, selectedFragment!!)
        return true
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        selectedFragment?.scrollToTop()
    }

    fun showBottomView() {
        navigationView.visibility = View.VISIBLE
    }

    fun hideBottomView() {
        navigationView.visibility = View.GONE
    }

    companion object {
        val fragmentConversations = FragmentConversations()
        val fragmentItems = FragmentItems()
    }
}
