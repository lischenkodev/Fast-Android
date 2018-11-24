package ru.stwtforever.fast.adapter;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public abstract class BaseListViewAdapter<T> extends BaseAdapter {
	
	private ArrayList<T> values;
    private ArrayList<T> cleanValues;

    protected Context context;
    protected LayoutInflater inflater;

    public BaseListViewAdapter(Context context, ArrayList<T> values) {
        this.context = context;
        this.values = values;

        this.inflater = LayoutInflater.from(context);
    }

    public T getItem(int position) {
        return values.get(position);
    }
	
	@Override
	public int getCount() {
		return values.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    public void filter(String query) {
        String lowerQuery = query.toLowerCase();

        if (cleanValues == null) {
            cleanValues = new ArrayList<>();
        }
        values.clear();

        if (query.isEmpty()) {
            values.addAll(cleanValues);
        } else {
            for (T value : cleanValues) {
                if (onQueryItem(value, lowerQuery)) {
                    values.add(value);
                }
            }
        }

        notifyDataSetChanged();
    }

    public boolean onQueryItem(T item, String lowerQuery) {
        return false;
    }

    public ArrayList<T> getValues() {
        return values;
    }
	
}
