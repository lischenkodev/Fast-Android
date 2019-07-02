package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.ShowCreateChatActivity;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.view.CircleImageView;

public class ShowCreateAdapter extends RecyclerAdapter<VKUser, ShowCreateAdapter.ViewHolder> {

    public ShowCreateAdapter(Context context, ArrayList<VKUser> users) {
        super(context, users);
        UserConfig.getUser();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.activity_create_show_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        ImageView online;

        ImageButton remove;

        TextView name;
        TextView invitedBy;

        Drawable placeholder;

        ViewHolder(View v) {
            super(v);

            placeholder = getDrawable(R.drawable.avatar_placeholder);

            remove = v.findViewById(R.id.remove);

            avatar = v.findViewById(R.id.userAvatar);
            online = v.findViewById(R.id.online);

            name = v.findViewById(R.id.name);
            invitedBy = v.findViewById(R.id.lastSeen);
        }

        void bind(final int position) {
            VKUser user = getItem(position);

            name.setText(user.toString());

            online.setVisibility(user.isOnline() ? View.VISIBLE : View.GONE);
            online.setImageDrawable(getOnlineIndicator(user));

            String text = user.getId() == UserConfig.userId ? getString(R.string.chat_creator) : getString(R.string.invited_by, UserConfig.user.toString());
            invitedBy.setText(text);

            if (TextUtils.isEmpty(user.getPhoto200())) {
                avatar.setImageDrawable(placeholder);
            } else {
                Picasso.get()
                        .load(user.getPhoto200())
                        .placeholder(placeholder)
                        .into(avatar);
            }

            remove.setVisibility(user.getId() == UserConfig.userId ? View.GONE : View.VISIBLE);

            remove.setOnClickListener(p1 -> {
                if (getValues().size() >= 2) {
                    remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(0, getItemCount(), getItem(getItemCount() - 1));
                } else {
                    ((ShowCreateChatActivity) context).finish();
                }
            });
        }

        @Nullable
        private Drawable getOnlineIndicator(@NonNull VKUser user) {
            return !user.isOnline() ? null : getDrawable(user.isOnlineMobile() ? R.drawable.ic_online_mobile : R.drawable.ic_online);
        }
    }
}
