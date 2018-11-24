package ru.stwtforever.fast.adapter;

import android.content.*;
import android.graphics.drawable.*;
import android.support.annotation.*;
import android.support.v7.widget.*;
import android.view.*;

import java.util.*;

public abstract class BaseRecyclerAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private ArrayList<T> values;
    private ArrayList<T> cleanValues;

    protected Context context;
    protected LayoutInflater inflater;

    public BaseRecyclerAdapter(Context context, ArrayList<T> values) {
        this.context = context;
        this.values = values;

        this.inflater = LayoutInflater.from(context);
    }

    protected @ColorInt
    int getColor(int resId) {
        if (context == null) return -1;

        return context.getResources().getColor(resId);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {

    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public T getItem(int position) {
        return values.get(position);
    }

    public void filter(String query) {
        String lowerQuery = query.toLowerCase();

        if (cleanValues == null) {
            cleanValues = new ArrayList<>(values);
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

    public String getString(int res) {
        return context.getString(res);
    }

    public Drawable getDrawable(int res) {
        return context.getDrawable(res);
    }
}
