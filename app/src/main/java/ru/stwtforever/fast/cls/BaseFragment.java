package ru.stwtforever.fast.cls;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseFragment extends Fragment {
	
	protected CharSequence title = "";
	private RecyclerView list;
	
	public BaseFragment(String title) {
		this.title = title;
	}
	
	public BaseFragment() {
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
	
	public void setList(RecyclerView list) {
		this.list = list;
	}
	
	public RecyclerView getList() {
		return list != null ? list : new RecyclerView(getContext());
	}
}
