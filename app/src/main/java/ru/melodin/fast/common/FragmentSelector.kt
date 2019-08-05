package ru.melodin.fast.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import ru.melodin.fast.R
import ru.melodin.fast.util.ArrayUtil

object FragmentSelector {

    private const val FRAGMENT_CONTAINER = R.id.fragment_container

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
            transaction.addToBackStack(null)

        transaction.commit()
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
}
