package ru.stwtforever.fast.adapter;

import android.content.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.util.*;
import java.util.*;
import java.text.*;

public class ExceptionAdapter extends BaseListViewAdapter<FException> {

	public ExceptionAdapter(Context context, ArrayList<FException> items) {
		super(context, items);
	}
	
	@Override
	public View getView(final int index, View view, ViewGroup parent) {
		view = inflater.inflate(R.layout.item_exception, parent, false);
		
		final TextView tv = view.findViewById(R.id.text);
		
		final FException e = getItem(index);
		
		tv.setText(new SimpleDateFormat("dd.MM.yyyy, HH:mm").format(e.time));
		
		return view;
	}
}
