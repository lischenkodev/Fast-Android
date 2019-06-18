package ru.melodin.fast.current;

import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

public abstract class BaseFragment extends Fragment {

    private CharSequence title;
    private RecyclerView recyclerView;
    private Toolbar toolbar;

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    protected void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    protected CharSequence getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public void scrollToTop() {
        if (recyclerView != null)
            recyclerView.smoothScrollToPosition(0);

        if (toolbar != null)
            expandToolbar();
    }

    private void expandToolbar() {
        AppBarLayout appBar = (AppBarLayout) toolbar.getParent();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior != null) {
            behavior.setTopAndBottomOffset(0);
            behavior.onNestedPreScroll((CoordinatorLayout) appBar.getParent(), appBar, null, 0, 1, new int[2]);
        }
    }
}
