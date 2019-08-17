package ru.melodin.fast.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ru.melodin.fast.fragment.FragmentFriends

class FriendsAdapter(fragmentManager: FragmentManager, var titles: ArrayList<String>) :
    FragmentStatePagerAdapter(fragmentManager) {

    private val fragments = arrayListOf<Fragment>(FragmentFriends(), FragmentFriends(true))

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
