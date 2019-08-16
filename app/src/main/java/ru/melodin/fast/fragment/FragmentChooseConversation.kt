package ru.melodin.fast.fragment


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.ConversationAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.model.VKConversation
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.mvp.contract.ConversationsContract
import ru.melodin.fast.mvp.presenter.ConversationsPresenter
import ru.melodin.fast.util.ArrayUtil
import java.util.*

class FragmentChooseConversation : BaseFragment(), ConversationsContract.View,
    RecyclerAdapter.OnItemClickListener {

    private var adapter: ConversationAdapter? = null

    private val presenter = ConversationsPresenter()

    override fun isBottomViewVisible(): Boolean {
        return false
    }

    override fun onDestroy() {
        adapter?.destroy()
        super.onDestroy()

        presenter.detachView()
        presenter.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.attachView(this)

        title = string(R.string.choose_conversation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refreshLayout.isEnabled = false
        refreshLayout.isRefreshing = false

        toolbar = tb
        recyclerList = list

        tb.setTitle(title)

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        presenter.viewIsReady()

        getCachedConversations(0, 0)

        if (adapter != null && list?.adapter == null) {
            list?.adapter = adapter
        }
    }

    override fun getCachedConversations(count: Int, offset: Int) {
        val conversations = CacheStorage.conversations ?: return
        conversations.reverse()

        if (!ArrayUtil.isEmpty(conversations)) {
            setNoItemsViewVisible(false)
            setProgressBarVisible(false)
            createAdapter(conversations, 0)
        } else {
            setNoItemsViewVisible(true)
        }
    }

    override fun loadConversations(count: Int, offset: Int) {

    }

    override fun setProgressBarVisible(visible: Boolean) {
        when (visible) {
            true -> {
                progressBar.visibility = View.VISIBLE
                setNoItemsViewVisible(false)
            }
            else -> progressBar.visibility = View.GONE
        }
    }

    override fun setNoItemsViewVisible(visible: Boolean) {
        when (visible) {
            true -> {
                emptyView.visibility = View.VISIBLE
                setProgressBarVisible(false)
            }
            else -> emptyView.visibility = View.GONE
        }
    }

    override fun setRefreshing(value: Boolean) {
    }

    override fun createAdapter(items: ArrayList<VKConversation>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) return

        if (adapter == null) {
            adapter = ConversationAdapter(this, items!!)
            adapter!!.setOnItemClickListener(this)

            list.adapter = adapter
            list.scrollToPosition(0)
            return
        }

        if (offset != 0) {
            adapter!!.values!!.addAll(items!!)
            adapter!!.notifyDataSetChanged()
            return
        }

        adapter!!.changeItems(items!!)
        adapter!!.notifyItemRangeChanged(0, adapter!!.itemCount, -1)
    }

    override fun clearList() {
        adapter?.clear()
        adapter?.notifyDataSetChanged()
    }

    override fun showNoInternetToast() {
        Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorToast() {
        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
    }

    override fun onItemClick(position: Int) {
        val conversation = adapter!!.getItem(position)

        activity?.onBackPressed()
        targetFragment!!.onActivityResult(
            FragmentMessages.REQUEST_CHOOSE_MESSAGE,
            Activity.RESULT_OK,
            Intent().apply {
                putExtras(arguments!!.apply {
                    putInt("peer_id", conversation.peerId)
                    putSerializable("conversation", conversation)
                })
            })
    }

}
