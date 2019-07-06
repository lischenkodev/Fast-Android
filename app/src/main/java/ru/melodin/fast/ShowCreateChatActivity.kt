package ru.melodin.fast

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_show_create.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.adapter.ShowCreateAdapter
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.concurrent.AsyncCallback
import ru.melodin.fast.concurrent.ThreadExecutor
import ru.melodin.fast.util.ViewUtil
import java.util.*

class ShowCreateChatActivity : AppCompatActivity(), TextWatcher {

    override fun afterTextChanged(p0: Editable?) {

    }
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        invalidateOptionsMenu()
    }

    private var adapter: ShowCreateAdapter? = null

    private var users: ArrayList<VKUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)

        users = intent.getSerializableExtra("users") as ArrayList<VKUser>

        setContentView(R.layout.activity_show_create)

        tb.inflateMenu(R.menu.activity_create_chat)
        tb.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.create && adapter != null)
                createChat()
        }
        tb.setBackIcon(ContextCompat.getDrawable(this, R.drawable.md_clear))
        tb.setBackVisible(true)
        tb.setOnBackClickListener { onBackPressed() }
        tb.setTitle(R.string.create_chat)

        refresh.isEnabled = false

        chatTitle.addTextChangedListener(this)

        val manager = LinearLayoutManager(this)
        manager.orientation = RecyclerView.VERTICAL

        list.setHasFixedSize(true)
        list.layoutManager = manager

        createAdapter()
    }

    private fun createAdapter() {
        adapter = ShowCreateAdapter(this, users)
        list.adapter = adapter

        tb.setOnClickListener { list.smoothScrollToPosition(0) }
    }

    private fun createChat() {
        ThreadExecutor.execute(object : AsyncCallback(this) {

            var peerId: Int = 0

            lateinit var title_: StringBuilder

            @Throws(Exception::class)
            override fun ready() {
                val ids = ArrayList<Int>()
                for (user in adapter!!.values) {
                    ids.add(user.id)
                }

                title_ = StringBuilder(chatTitle.text.toString().trim())

                if (TextUtils.isEmpty(title_.toString())) {
                    if (users!!.size == 1) {
                        title_.append(users!![0].name)
                    } else
                        for (i in users!!.indices) {
                            val user = adapter!!.getItem(i)
                            title_.append(user.name).append(if (i == users!!.size) "" else ", ")
                        }
                }

                peerId = 2000000000 + VKApi.messages().createChat().title(title_.toString()).userIds(ids).execute(Int::class.java)[0]
            }

            override fun done() {
                val intent = Intent()
                intent.putExtra("title", title_.toString())
                intent.putExtra("peer_id", peerId)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

            override fun error(e: Exception) {
                Log.e("Error create chat", Log.getStackTraceString(e))
                Toast.makeText(this@ShowCreateChatActivity, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
}
