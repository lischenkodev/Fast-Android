package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ru.melodin.fast.R;
import ru.melodin.fast.ShowCreateChatActivity;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.view.CircleImageView;

public class ShowCreateAdapter extends RecyclerAdapter<VKUser, ShowCreateAdapter.ViewHolder> {

    public ShowCreateAdapter(Context context, ArrayList<VKUser> users) {
        super(context, users);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.activity_create_show_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowCreateAdapter.ViewHolder holder, int position) {
        holder.bind(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        CircleImageView online;

        ImageButton remove;

        TextView name;
        TextView invited_by;

        ViewHolder(View v) {
            super(v);
            remove = v.findViewById(R.id.remove);

            avatar = v.findViewById(R.id.avatar);
            online = v.findViewById(R.id.online);

            name = v.findViewById(R.id.name);
            invited_by = v.findViewById(R.id.last_seen);
        }

        void bind(final int position) {
            VKUser user = getItem(position);

            name.setText(user.toString());

            online.setVisibility(user.online ? View.VISIBLE : View.GONE);

            String text = user.id == UserConfig.userId ? getString(R.string.chat_creator) : getString(R.string.invited_by, UserConfig.user.toString());
            invited_by.setText(text);

            if (TextUtils.isEmpty(user.photo_100)) {
                avatar.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            } else {
                Picasso.get()
                        .load(user.photo_100)
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .into(avatar);
            }

            remove.setVisibility(user.id == UserConfig.userId ? View.GONE : View.VISIBLE);

            remove.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    if (getValues().size() >= 2) {
                        remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(0, getItemCount(), getItem(getItemCount() - 1));
                    } else {
                        ((ShowCreateChatActivity) context).finish();
                    }
                }

            });
        }
    }
}
