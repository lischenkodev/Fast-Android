package ru.melodin.fast.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.model.ListItem;

public class PopupAdapter extends RecyclerAdapter<ListItem, PopupAdapter.ViewHolder> {

    public static final int ID_CLEAR_DIALOG = 0;
    public static final int ID_NOTIFICATIONS = 1;
    public static final int ID_LEAVE = 2;

    public PopupAdapter(Context context, ArrayList<ListItem> values) {
        super(context, values);
    }

    public int searchPosition(int id) {
        for (ListItem item : getValues())
            if (item.getId() == id)
                return getValues().indexOf(item);

        return -1;
    }

    public void updateItem(@NonNull ListItem item) {
        for (ListItem i : getValues())
            if (i.getId() == item.getId()) {
                i.setTitle(item.getTitle());
                i.setIcon(item.getIcon());
                i.setVisible(item.isVisible());
                break;
            }
    }

    @Override
    public void changeItems(ArrayList<ListItem> items) {
        super.changeItems(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.activity_messages_popup_item, parent, false));
    }

    class ViewHolder extends RecyclerHolder {

        ImageView icon;
        TextView title;

        ViewHolder(@NonNull View v) {
            super(v);

            title = v.findViewById(R.id.title);
            icon = v.findViewById(R.id.icon);
        }

        @Override
        public void bind(int position) {
            ListItem item = getItem(position);

            icon.setImageDrawable(item.getIcon());
            title.setText(item.getTitle());
        }
    }
}