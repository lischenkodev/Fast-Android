package ru.melodin.fast.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.fragment.FragmentFriends;
import ru.melodin.fast.service.LongPollService;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class FriendAdapter extends RecyclerAdapter<VKUser, FriendAdapter.ViewHolder> {

    private FragmentFriends fragment;

    public FriendAdapter(FragmentFriends context, ArrayList<VKUser> friends) {
        super(context.getContext(), friends);
        this.fragment = context;
        EventBus.getDefault().register(this);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onReceive(Object[] data) {
        if (ArrayUtil.isEmpty(data)) return;

        String key = (String) data[0];

        switch (key) {
            case LongPollService.KEY_USER_OFFLINE:
                setUserOnline(false, (int) data[1], (int) data[2]);
                break;
            case LongPollService.KEY_USER_ONLINE:
                setUserOnline(true, (int) data[1], (int) data[2]);
                break;
        }
    }

    private void setUserOnline(boolean online, int userId, int time) {
        for (VKUser user : getValues()) {
            if (user.id == userId) {
                user.online = online;
                user.last_seen = time;
                notifyDataSetChanged();
            }
        }
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
            functions = v.findViewById(R.id.functions);
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
                    fragment.showDialog(position, v);
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

            String seen = String.format(seen_text, Util.dateFormatter.format(user.last_seen * 1000));

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
