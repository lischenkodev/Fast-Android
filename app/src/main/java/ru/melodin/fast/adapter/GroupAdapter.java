package ru.melodin.fast.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.api.VKUtil;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.view.CircleImageView;

public class GroupAdapter extends RecyclerAdapter<VKGroup, GroupAdapter.ViewHolder> {

    public GroupAdapter(Context context, ArrayList<VKGroup> values) {
        super(context, values);
    }

    @Nullable
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_group, parent, false));
    }

    class ViewHolder extends RecyclerHolder {

        CircleImageView avatar;

        TextView name;
        TextView description;

        ViewHolder(@NonNull View v) {
            super(v);

            avatar = v.findViewById(R.id.userAvatar);
            name = v.findViewById(R.id.name);
            description = v.findViewById(R.id.description);
        }

        @Override
        public void bind(int position) {
            VKGroup group = getItem(position);

            name.setText(group.getName());

            String text = VKUtil.getGroupStringType(context, group.getType()) + " • " + getString(R.string.members_count, group.getMembersCount());
            description.setText(text);

            if (!TextUtils.isEmpty(group.getPhoto200())) {
                Picasso.get()
                        .load(group.getPhoto200())
                        .priority(Picasso.Priority.HIGH)
                        .into(avatar);
            }
        }
    }
}
