package ru.melodin.fast.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_photo_view.pager
import kotlinx.android.synthetic.main.fragment_friends.*
import ru.melodin.fast.R
import ru.melodin.fast.adapter.FriendsAdapter
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseFragment

class ParentFragmentFriends : BaseFragment() {

    private var adapter: FriendsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tb.setTitle(R.string.fragment_friends)
//        tb.setNavigationIcon(R.drawable.ic_arrow_back)
//        tb.setNavigationOnClickListener { parent?.onBackPressed() }

        tabLayout.setTabTextColors(ThemeManager.SECONDARY, ThemeManager.MAIN)
        tabLayout.setSelectedTabIndicatorColor(ThemeManager.MAIN)

        createAdapter()
    }

    private fun createAdapter() {
        val titles = arrayListOf(getString(R.string.all), getString(R.string.online))
        adapter = FriendsAdapter(childFragmentManager, titles)
        pager.adapter = adapter
        tabLayout.setupWithViewPager(pager)
    }
}
