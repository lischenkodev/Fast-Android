package ru.melodin.fast.current

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.appbar.AppBarLayout

import ru.melodin.fast.view.FastToolbar

abstract class BaseFragment : Fragment() {

    protected var title: CharSequence? = null

    var recyclerView: RecyclerView? = null
    var toolbar: FastToolbar? = null

    protected fun setTitle(title: String) {
        this.title = title
    }

    fun scrollToTop() {
        if (recyclerView != null)
            recyclerView!!.smoothScrollToPosition(0)

        if (toolbar != null)
            expandToolbar()
    }

    private fun expandToolbar() {
        val appBar = toolbar!!.parent as AppBarLayout
        val params = appBar.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        if (behavior != null) {
            behavior.topAndBottomOffset = 0
            behavior.onNestedPreScroll(appBar.parent as CoordinatorLayout, appBar, appBar, 0, 1, IntArray(2))
        }
    }
}
