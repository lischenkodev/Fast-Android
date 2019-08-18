package ru.melodin.fast.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.fragment.FragmentPhotoView

class PhotoViewAdapter(fm: FragmentManager, private val items: ArrayList<VKPhoto>) :
    FragmentStatePagerAdapter(fm) {

    var fragments = arrayListOf<Fragment>()

    init {
        for (i in items.indices) {
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
