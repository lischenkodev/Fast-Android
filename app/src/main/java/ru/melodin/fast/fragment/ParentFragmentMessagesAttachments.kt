package ru.melodin.fast.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_messages_attachments.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.MessagesAttachmentsAdapter
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment

class ParentFragmentMessagesAttachments : BaseFragment() {

    private var peerId = 0
    private var adapter: MessagesAttachmentsAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        peerId = arguments!!.getInt("peer_id", -1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages_attachments, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tb.setTitle(R.string.attachments)
        tb.setNavigationIcon(R.drawable.ic_arrow_back)
        tb.setNavigationOnClickListener { onBackPressed() }

        tb.setOnClickListener {
            pager ?: return@setOnClickListener
            adapter ?: return@setOnClickListener
            adapter!!.fragments[pager.currentItem].scrollToTop()
        }

        tabLayout.setTabTextColors(ThemeManager.SECONDARY, ThemeManager.MAIN)
        tabLayout.setSelectedTabIndicatorColor(ThemeManager.MAIN)

        createAdapter()
    }

    private fun createAdapter() {
        val titles = arrayListOf(
            getString(R.string.photos),
            getString(R.string.videos),
            getString(R.string.audios),
            getString(R.string.documents),
            getString(R.string.links)
        )

        pager.offscreenPageLimit = 4

        adapter = MessagesAttachmentsAdapter(fragmentManager!!, peerId, titles)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
    }
}