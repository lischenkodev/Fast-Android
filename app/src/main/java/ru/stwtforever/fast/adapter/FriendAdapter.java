package ru.stwtforever.fast.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.fragment.FragmentFriends;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.view.CircleImageView;

public class FriendAdapter extends RecyclerAdapter<VKUser, FriendAdapter.ViewHolder> {

    private FragmentFriends fragment;

    public FriendAdapter(FragmentFriends context, ArrayList<VKUser> friends) {
        super(context.getContext(), friends);
        this.fragment = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.fragment_friends_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    private void showDialog(final int position, View v) {
        fragment.showDialog(position, v);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        CircleImageView online;

        TextView name;
        TextView lastSeen;

        ImageButton message;
        ImageButton functions;


        Drawable placeholder;

        ViewHolder(View v) {
            super(v);

            placeholder = getDrawable(R.drawable.placeholder_user);

            avatar = v.findViewById(R.id.avatar);
            online = v.findViewById(R.id.online);

            lastSeen = v.findViewById(R.id.last_seen);
            name = v.findViewById(R.id.name);

            message = v.findViewById(R.id.message);
            functions = v.findViewById(R.id.funcs);
        }

        void bind(final int position) {
            message.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    fragment.openChat(position);
                }
            });

            functions.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    showDialog(position, v);
                }

            });

            VKUser user = getItem(position);

            name.setText(user.toString());

            if (user.online) {
                lastSeen.setVisibility(View.GONE);
                online.setVisibility(View.VISIBLE);
            } else {
                lastSeen.setVisibility(View.VISIBLE);
                online.setVisibility(View.GONE);
            }

            String seen_text = getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w);

            String seen = String.format(seen_text, Utils.dateFormatter.format(user.last_seen * 1000));

            if (lastSeen.getVisibility() == View.VISIBLE) {
                lastSeen.setText(seen);
            } else {
                lastSeen.setText("");
            }

            if (TextUtils.isEmpty(user.photo_100)) {
                avatar.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
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
