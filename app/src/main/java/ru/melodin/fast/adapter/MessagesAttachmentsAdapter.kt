package ru.melodin.fast.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ru.melodin.fast.current.BaseFragment
import ru.melodin.fast.fragment.FragmentMessagesAttachments
import java.util.*

class MessagesAttachmentsAdapter(
    fm: FragmentManager,
    peerId: Int,
    private val titles: ArrayList<String>
) : FragmentStatePagerAdapter(fm) {

    val fragments: ArrayList<BaseFragment> = ArrayList()

    init {
        fragments.add(
            FragmentMessagesAttachments(
                peerId,
                FragmentMessagesAttachments.TYPE_PHOTO
            )
        )
        fragments.add(
            FragmentMessagesAttachments(
                peerId,
                FragmentMessagesAttachments.TYPE_VIDEO
            )
        )
        fragments.add(
            FragmentMessagesAttachments(
                peerId,
                FragmentMessagesAttachments.TYPE_AUDIO
            )
        )
        fragments.add(
            FragmentMessagesAttachments(
                peerId,
                FragmentMessagesAttachments.TYPE_DOC
            )
        )
        fragments.add(
            FragmentMessagesAttachments(
                peerId,
                FragmentMessagesAttachments.TYPE_LINK
            )
        )
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return titles[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}
