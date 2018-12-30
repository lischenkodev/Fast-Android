package ru.stwtforever.fast.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.stwtforever.fast.R;

public class AlertAdapter extends ArrayAdapter<String> {

    private LayoutInflater inflater;
    private ViewHolder holder;
    private ArrayList<String> items;

    public AlertAdapter(@NonNull Context context, ArrayList<String> items) {
        super(context, R.layout.list_item, items);

        inflater = LayoutInflater.from(context);
        this.items = items;
    }

    class ViewHolder {
        TextView text;

        ViewHolder(View v) {
            text = v.findViewById(R.id.text);
        }

        void bind(int position) {
            text.setText(items.get(position));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.bind(position);
        return convertView;
    }
}
