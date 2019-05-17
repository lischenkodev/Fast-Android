package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class CreateChatAdapter extends RecyclerAdapter<VKUser, CreateChatAdapter.ViewHolder> {

    public CreateChatAdapter(Context context, ArrayList<VKUser> users) {
        super(context, users);
    }

    public boolean isSelected(int position) {
        return getValues().get(position).isSelected();
    }

    public void setSelected(int position) {
        getValues().get(position).setSelected(true);
    }

    public void toggleSelected(int position) {
        VKUser user = getItem(position);
        user.setSelected(!user.isSelected());
    }

    public SparseArray<VKUser> getSelectedPositions() {
        SparseArray<VKUser> selected = new SparseArray<>();

        for (int i = 0; i < getValues().size(); i++) {
            VKUser user = getValues().get(i);
            if (user.isSelected()) {
                selected.put(i, user);
            }
        }

        return selected;
    }

    public void clearSelect() {
        clearSelect(-1);
    }

    public void clearSelect(int position) {
        if (position != -1) {
            getValues().get(position).setSelected(false);
        } else
            for (int i = 0; i < getValues().size(); i++) {
                VKUser u = getValues().get(i);
                if (u.isSelected()) {
                    u.setSelected(false);
                }
            }
    }

    public int getSelectedCount() {
        int count = 0;

        for (VKUser u : getValues()) {
            if (u.isSelected()) count++;
        }

        return count;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.activity_create_chat_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CreateChatAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        CircleImageView online;

        TextView name;
        TextView lastSeen;

        LinearLayout root;

        AppCompatCheckBox selected;

        Drawable placeholder;


        ViewHolder(View v) {
            super(v);

            selected = v.findViewById(R.id.selected);
            placeholder = getDrawable(R.drawable.placeholder_user);
            root = v.findViewById(R.id.root);

            avatar = v.findViewById(R.id.avatar);
            online = v.findViewById(R.id.online);

            name = v.findViewById(R.id.name);
            lastSeen = v.findViewById(R.id.last_seen);
        }

        void bind(int position) {
            VKUser user = getItem(position);

            name.setText(user.toString());

            if (user.online) {
                lastSeen.setVisibility(View.GONE);
                online.setVisibility(View.VISIBLE);
            } else {
                lastSeen.setVisibility(View.VISIBLE);
                online.setVisibility(View.GONE);
            }

            selected.setChecked(user.isSelected());

            String seen_text = getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w);

            String seen = String.format(seen_text, Util.dateFormatter.format(user.last_seen * 1000));

            if (lastSeen.getVisibility() == View.VISIBLE) {
                lastSeen.setText(seen);
            } else {
                lastSeen.setText("");
            }

            if (TextUtils.isEmpty(user.photo_100)) {
                avatar.setImageDrawable(placeholder);
            } else {
                Picasso.get()
                        .load(user.photo_100)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder)
                        .into(avatar);
            }
        }
    }
}
