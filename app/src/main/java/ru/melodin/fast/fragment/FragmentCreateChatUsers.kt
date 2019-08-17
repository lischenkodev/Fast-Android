package ru.melodin.fast.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_show_create.*
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar
import java.util.*

class FragmentCreateChatUsers : BaseFragment() {

    private var adapter: UserAdapter? = null

    private var users: ArrayList<VKUser>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        users = arguments!!.getSerializable("users") as ArrayList<VKUser>
    }

    override fun isBottomViewVisible(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_show_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tb.inflateMenu(R.menu.activity_create_chat)
        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem): Boolean {
                if (item.itemId == R.id.create && adapter != null) {
                    createChat()

                    return true
                }

                return false
            }

        })
        tb.setBackIcon(drawable(R.drawable.md_clear))
        tb.setBackVisible(true)
        tb.setTitle(R.string.create_chat)

        ViewUtil.applyToolbarMenuItemsColor(tb)

        refreshLayout.isEnabled = false

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity!!, RecyclerView.VERTICAL, false)

        emptyView.visibility = View.GONE
        progressBar.visibility = View.GONE

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

            VKApi.messages().createChat().title(builder.toString()).userIds(ids)
                .execute(Int::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val peerId = 2_000_000_000 + models[0] as Int

                        parent!!.onBackPressed()
                        targetFragment!!.onActivityResult(
                            FragmentCreateChat.REQUEST_CREATE_CHAT,
                            Activity.RESULT_OK,
                            Intent().apply {
                                putExtra("title", title)
                                putExtra("peer_id", peerId)
                            })
                    }

                    override fun onError(e: Exception) {
                        Log.e("Error create chat", Log.getStackTraceString(e))
                        Toast.makeText(
                            activity!!,
                            getString(R.string.error) + ": " + e.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    fun confirmKick(position: Int) {
        if (position == 0) {
            parent!!.onBackPressed()
            return
        }
        if (list.isComputingLayout) return

        adapter!!.remove(position)
        adapter!!.notifyItemRemoved(position)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }
}
