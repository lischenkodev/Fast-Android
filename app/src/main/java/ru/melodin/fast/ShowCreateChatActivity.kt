package ru.melodin.fast

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_show_create.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar
import java.util.*

class ShowCreateChatActivity : BaseActivity(), TextWatcher {

    override fun afterTextChanged(p0: Editable?) {

    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        invalidateOptionsMenu()
    }

    private var adapter: UserAdapter? = null

    private var users: ArrayList<VKUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.currentTheme)
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)

        users = intent.getSerializableExtra("users") as ArrayList<VKUser>

        setContentView(R.layout.activity_show_create)

        tb.inflateMenu(R.menu.activity_create_chat)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.create && adapter != null)
                    createChat()
            }

        })
        tb.setBackIcon(drawable(R.drawable.md_clear))
        tb.setBackVisible(true)
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
        adapter = UserAdapter(this, users!!, true)
        list.adapter = adapter

        tb.setOnClickListener { list.smoothScrollToPosition(0) }
    }

    private fun createChat() {
        TaskManager.execute {

            val builder: StringBuilder = StringBuilder(chatTitle.text.toString().trim())

            val ids = ArrayList<Int>()
            for (user in adapter!!.values!!) {
                ids.add(user.id)
            }

            if (TextUtils.isEmpty(builder.toString())) {
                builder.append(users!![0].name)

                for (i in 1 until users!!.size) {
                    if (i > 3) break
                    builder.append(", ")
                    builder.append(users!![i].name)
                }
            }

            VKApi.messages().createChat().title(builder.toString()).userIds(ids).execute(Int::class.java, object : VKApi.OnResponseListener {
                override fun onSuccess(models: ArrayList<*>?) {
                    if (ArrayUtil.isEmpty(models)) return
                    models ?: return

                    val peerId = 2_000_000_000 + models[0] as Int

                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("title", title.toString())
                        putExtra("peer_id", peerId)
                    })
                    finish()
                }

                override fun onError(e: Exception) {
                    Log.e("Error create chat", Log.getStackTraceString(e))
                    Toast.makeText(this@ShowCreateChatActivity, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show()
                }
            })
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(-1, -1)
    }

    fun confirmKick(position: Int) {
        if (position == 0) finish()
        if (list.isComputingLayout) return

        adapter!!.remove(position)
        adapter!!.notifyItemRemoved(position)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }
}
