package ru.melodin.fast.current

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import ru.melodin.fast.MainActivity

abstract class BaseFragment : Fragment() {

    internal var title: CharSequence? = null

    var recyclerList: RecyclerView? = null
    var toolbar: MaterialToolbar? = null

    var parent: MainActivity? = null

    open fun isBottomViewVisible(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parent = activity as MainActivity?
    }

    fun scrollToTop() {
        if (recyclerList != null)
            recyclerList!!.smoothScrollToPosition(0)

        if (toolbar != null)
            expandToolbar()
    }

    private fun expandToolbar() {
        val appBar = toolbar!!.parent as AppBarLayout
        val params = appBar.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior?
        if (behavior != null) {
            behavior.topAndBottomOffset = 0
            behavior.onNestedPreScroll(
                appBar.parent as CoordinatorLayout,
                appBar,
                appBar,
                0,
                1,
                IntArray(2)
            )
        }
    }

    fun drawable(@DrawableRes resId: Int): Drawable {
        return ContextCompat.getDrawable(context!!, resId)!!
    }

    @ColorInt
    fun color(@ColorRes resId: Int): Int {
        return ContextCompat.getColor(context!!, resId)
    }

    fun string(@StringRes resId: Int): String {
        return getString(resId)
    }

    fun string(@StringRes resId: Int, vararg args: Any): String {
        return getString(resId, *args)
    }

    open fun onBackPressed() {}

}
