package ru.stwtforever.fast.adapter;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.helper.*;
import java.util.*;
import ru.stwtforever.fast.util.*;

public class DrawerAdapter extends BaseListViewAdapter<Object[]> {

	public DrawerAdapter(Context context, ArrayList<Object[]> items) {
		super(context, items);
	}
	
	public void setHover(boolean hover, int pos) {
		if (!isHover(pos)) {
			for (int i = 0; i < getValues().size(); i++) {
				getValues().get(i)[2] = i == pos ? hover : false;
			}
			notifyDataSetChanged();
		}
	}
	
	public void clearHover() {
		for (Object[] o : getValues()) {
			o[2] = false;
		}
		
		notifyDataSetChanged();
	}
	
	public boolean isHover(int i) {
		return (boolean) getValues().get(i)[2];
	}
	
	@Override
	public View getView(int position, View v, ViewGroup parent) {
		if (v == null)
			v = inflater.inflate(R.layout.drawer_item, parent, false);
		
		Object[] item = getValues().get(position);
			
		ImageView icon = v.findViewById(R.id.icon);
		TextView title = v.findViewById(R.id.title);
		LinearLayout root = v.findViewById(R.id.root);
		
		FontHelper.setFont(title, FontHelper.PS_REGULAR);
		
		String s = (String) item[0];
		int i = (int) item[1];
		boolean select = (boolean) item[2];
		
		Drawable ic = null;
		
		if (i != 0) 
			ic = context.getResources().getDrawable(i);
		
		int color, bgColor = 0;
		
		if (select) {
			color = ThemeManager.getAccent();
			bgColor = ColorUtil.alphaColor(color, 0.5f);
		} else {
			color = ThemeManager.isDark() ? Color.WHITE : Color.DKGRAY;
			bgColor = 0;
		}
		
		if (ic != null) {
			ic.setTint(color);
			icon.setImageDrawable(ic);
		}
		
		title.setTextColor(color);
		root.setBackgroundColor(bgColor);
		title.setText(s);
		
		return v;
	}
	
	
}
