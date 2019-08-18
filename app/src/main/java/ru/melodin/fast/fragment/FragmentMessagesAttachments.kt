package ru.melodin.fast.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.list_empty.*
import kotlinx.android.synthetic.main.recycler_list.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.PhotoViewActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.AttachmentsAdapter
import ru.melodin.fast.adapter.RecyclerAdapter
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.mvp.contract.AttachmentsContract
import ru.melodin.fast.mvp.presenter.AttachmentsPresenter
import ru.melodin.fast.util.ArrayUtil

class FragmentMessagesAttachments(private val peerId: Int, private val type: String) :
    BaseFragment(), AttachmentsContract.View, SwipeRefreshLayout.OnRefreshListener,
    RecyclerAdapter.OnItemClickListener {

    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_DOC = "doc"
        const val TYPE_LINK = "link"
    }

    private var adapter: AttachmentsAdapter? = null

    private val presenter = AttachmentsPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter.attachView(this)
    }

    override fun onRefresh() {
        loadAttachments(200, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = tb
        recyclerList = list

        tb.visibility = View.GONE

        refreshLayout.setColorSchemeColors(ThemeManager.ACCENT)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.PRIMARY)

        when (type) {
            TYPE_PHOTO -> {
                list.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply { gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS }
            }

            TYPE_VIDEO -> {
                list.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply { gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS }
            }

            else -> {
                list.setHasFixedSize(true)
                list.layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
            }
        }

        loadAttachments(200, 0)
    }

    override fun onItemClick(position: Int) {
        val item = adapter!!.getItem(position)
        if (type == TYPE_PHOTO) {
            val intent = Intent(context, PhotoViewActivity::class.java)
            intent.putExtra("selected", item as VKPhoto)
            intent.putExtra("photo", adapter!!.values)
            startActivity(intent)
        }
    }

    override fun loadAttachments(count: Int, offset: Int) {
        if (adapter == null || adapter!!.isEmpty) {
            setProgressBarVisible(true)
        } else {
            setProgressBarVisible(false)
            setRefreshing(true)
        }

        VKApi.messages().historyAttachments.mediaType(type).photoSizes(true).count(count)
            .peerId(peerId)
            .execute(VKModel::class.java,
                object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        presenter.onFilledList()

                        createAdapter(models as ArrayList<VKModel>?, 0)
                    }

                    override fun onError(e: Exception) {
                        showErrorToast()
                    }
                })
    }

    override fun createAdapter(items: ArrayList<VKModel>?, offset: Int) {
        if (ArrayUtil.isEmpty(items)) {
            setNoItemsViewVisible(true)
            return
        }

        items ?: return

        setNoItemsViewVisible(false)

        if (adapter == null) {
            adapter = AttachmentsAdapter(context!!, items)
            adapter!!.setOnItemClickListener(this)
            list.adapter = adapter
            return
        }

        adapter!!.changeItems(items)
        adapter!!.notifyDataSetChanged()
    }

    override fun setProgressBarVisible(visible: Boolean) {
        when (visible) {
            true -> {
                progressBar?.visibility = View.VISIBLE
                setRefreshing(false)
                setNoItemsViewVisible(false)
            }
            else -> progressBar?.visibility = View.GONE
        }
    }

    override fun setNoItemsViewVisible(visible: Boolean) {
        when (visible) {
            true -> {
                emptyView?.visibility = View.VISIBLE
                setProgressBarVisible(false)
            }
            else -> emptyView?.visibility = View.GONE
        }
    }

    override fun setRefreshing(value: Boolean) {
        when (value) {
            true -> {
                refreshLayout?.isRefreshing = true
                setProgressBarVisible(false)
            }
            else -> refreshLayout?.isRefreshing = false
        }
    }

    override fun clearList() {
    }

    override fun showNoInternetToast() {
        setRefreshing(false)
        Toast.makeText(activity, R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorToast() {
        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
    }

}
