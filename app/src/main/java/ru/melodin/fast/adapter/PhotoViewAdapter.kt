package ru.melodin.fast.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.fragment.FragmentPhotoView
import java.util.*

class PhotoViewAdapter(fm: FragmentManager, private val items: ArrayList<VKPhoto>) :
    FragmentStatePagerAdapter(fm) {

    var fragments: ArrayList<Fragment> = arrayListOf()

    init {
        for (i in fragments.indices) {
            fragments.add(FragmentPhotoView(items[i]))
        }
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }
}
