package ru.stwtforever.fast.cls;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseFragment extends Fragment {

    protected CharSequence title = "";
    protected RecyclerView recyclerView;

    public BaseFragment(String title) {
        this.title = title;
    }

    public BaseFragment() {
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    protected CharSequence getTitle() {
        return title;
    }

    protected void setTitle(CharSequence title) {
        getActivity().setTitle(title);
    }

    protected void setTitle(int title) {
        getActivity().setTitle(title);
    }

    protected void setTitle() {
        getActivity().setTitle(title);
    }

}
