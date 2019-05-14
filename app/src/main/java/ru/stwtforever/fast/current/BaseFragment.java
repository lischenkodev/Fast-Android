package ru.stwtforever.fast.current;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseFragment extends Fragment {

    protected CharSequence title = "";
    private RecyclerView recyclerView;

    public BaseFragment() {
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

    protected void setTitle(CharSequence title) {
        getActivity().setTitle(title);
    }

    protected void setTitle(int title) {
        getActivity().setTitle(title);
    }

}
