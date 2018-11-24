package ru.stwtforever.fast.fragment;


import android.*;
import android.os.*;
import android.support.v4.app.*;
import android.support.v4.widget.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.R;

public class FragmentGroups extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ListView.OnItemClickListener, ListView.OnItemLongClickListener {
	
    private ListView lv;
	private SwipeRefreshLayout refreshLayout;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_list, container, false);
    }

	@Override
	public void onRefresh() {
	}

	@Override
	public void onItemClick(AdapterView<?> p1, View v, int position, long p4) {
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> p1, View v, int position, long p4) {
		return true;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		lv = view.findViewById(R.id.list);

		refreshLayout = view.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));


        lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this);
	}
}
