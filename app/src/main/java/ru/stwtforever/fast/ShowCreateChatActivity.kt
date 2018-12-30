package ru.stwtforever.fast

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ru.stwtforever.fast.adapter.ShowCreateAdapter
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.util.ViewUtils
import java.util.*

class ShowCreateChatActivity : AppCompatActivity() {

    private var adapter: ShowCreateAdapter? = null

    private var title: EditText? = null
    private var tb: Toolbar? = null
    private var list: RecyclerView? = null

    private var users: ArrayList<VKUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.applyWindowStyles(this)
        setTheme(ThemeManager.getCurrentTheme())

        super.onCreate(savedInstanceState)
        getIntentData()

        setContentView(R.layout.activity_show_create)
        initViews()
    }

    private fun getIntentData() {
        users = intent.getSerializableExtra("users") as ArrayList<VKUser>
    }

    private fun initViews() {
        title = findViewById(R.id.title)
        title!!.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p1: CharSequence, p2: Int, p3: Int, p4: Int) {}

            override fun afterTextChanged(p1: Editable) {}

            override fun onTextChanged(p1: CharSequence, p2: Int, p3: Int, p4: Int) {
                invalidateOptionsMenu()
            }
        })

        tb = findViewById(R.id.tb)
        setSupportActionBar(tb)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.md_clear)

        supportActionBar!!.title = getString(R.string.create_chat)

        val refresh = findViewById<SwipeRefreshLayout>(R.id.refresh)
        refresh.isEnabled = false

        list = findViewById(R.id.list)

        val manager = LinearLayoutManager(this)
        manager.orientation = RecyclerView.VERTICAL

        list!!.setHasFixedSize(true)
        list!!.layoutManager = manager

        createAdapter()
    }

    private fun createAdapter() {
        adapter = ShowCreateAdapter(this, users)
        list!!.adapter = adapter

        tb!!.setOnClickListener { list!!.scrollToPosition(0) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.create -> if (adapter != null)
                createChat()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createChat() {
        ThreadExecutor.execute(object : AsyncCallback(this) {

            internal var res: Int = 0

            @Throws(Exception::class)
            override fun ready() {
                val ids = ArrayList<Int>()
                for (u in adapter!!.values) {
                    ids.add(u.id)
                }

                val title_ = StringBuilder(title!!.text.toString().trim { it <= ' ' })
                if (TextUtils.isEmpty(title_.toString())) {
                    if (users!!.size == 1) {
                        title_.append(users!![0].name)
                    } else
                        for (i in users!!.indices) {
                            val user = adapter!!.getItem(i)
                            title_.append(user.name).append(if (i == users!!.size) "" else ", ")
                        }
                }

                res = VKApi.messages().createChat().title(title_.toString()).userIds(ids).execute(Int::class.java)[0]
            }

            override fun done() {
                setResult(Activity.RESULT_OK)
                finish()
            }

            override fun error(e: Exception) {
                Log.e("Error create chat", Log.getStackTraceString(e))
                Toast.makeText(this@ShowCreateChatActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
            }

        })
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
