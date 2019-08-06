package ru.melodin.fast.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import ru.melodin.fast.R
import ru.melodin.fast.util.ArrayUtil

object FragmentSelector {

    private const val FRAGMENT_CONTAINER = R.id.fragment_container

    var currentFragment: Fragment? = null

    fun selectFragment(
        manager: FragmentManager,
        fragment: Fragment,
        args: Bundle?,
        withBackStack: Boolean
    ) {
        val fragments = manager.fragments

        val transaction = manager.beginTransaction()

        fragment.arguments = args ?: Bundle()

        if (ArrayUtil.isEmpty(fragments))
            transaction.add(FRAGMENT_CONTAINER, fragment, fragment.javaClass.simpleName)
        else {
            var contains = false

            for (f in fragments)
                if (f.javaClass.simpleName == fragment.javaClass.simpleName) {
                    transaction.show(f)
                    contains = true
                } else
                    transaction.hide(f)

            if (!contains)
                transaction.add(FRAGMENT_CONTAINER, fragment, fragment.javaClass.simpleName)
        }

        if (withBackStack)
            transaction.addToBackStack(fragment.javaClass.simpleName)

        transaction.commit()
        currentFragment = fragment
    }

    fun selectFragment(manager: FragmentManager, fragment: Fragment, args: Bundle?) {
        selectFragment(manager, fragment, args, false)
    }

    fun selectFragment(manager: FragmentManager, fragment: Fragment, withBackStack: Boolean) {
        selectFragment(manager, fragment, null, withBackStack)
    }

    fun selectFragment(manager: FragmentManager, fragment: Fragment) {
        selectFragment(manager, fragment, null)
    }

    fun addFragment(
        manager: FragmentManager,
        fragment: Fragment,
        args: Bundle?,
        withBackStack: Boolean
    ) {
        fragment.arguments = args ?: Bundle()

        val transaction = manager.beginTransaction()
        transaction.add(FRAGMENT_CONTAINER, fragment, fragment.javaClass.simpleName)

        if (withBackStack)
            transaction.addToBackStack(fragment.javaClass.simpleName)

        transaction.commit()
        currentFragment = fragment
    }

    fun addFragment(manager: FragmentManager, fragment: Fragment, args: Bundle?) {
        addFragment(manager, fragment, args, false)
    }

    fun addFragment(manager: FragmentManager, fragment: Fragment, withBackStack: Boolean) {
        addFragment(manager, fragment, null, withBackStack)
    }

    fun addFragment(manager: FragmentManager, fragment: Fragment) {
        addFragment(manager, fragment, null, false)
    }
}
