package ru.melodin.fast.adapter;

import android.content.Context;
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
import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.fragment.FragmentFriends;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class UserAdapter extends RecyclerAdapter<VKUser, UserAdapter.ViewHolder> {

    private FragmentFriends fragment;

    public UserAdapter(FragmentFriends context, ArrayList<VKUser> friends) {
        super(context.getContext(), friends);
        this.fragment = context;
        EventBus.getDefault().register(this);
    }

    public UserAdapter(Context context, ArrayList<VKUser> users) {
        super(context, users);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_user, parent, false);
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
            case LongPollEvents.KEY_USER_OFFLINE:
                setUserOnline(false, (int) data[1], (int) data[2]);
                break;
            case LongPollEvents.KEY_USER_ONLINE:
                setUserOnline(true, (int) data[1], (int) data[2]);
                break;
        }
    }

    private void setUserOnline(boolean online, int userId, int time) {
        for (int i = 0; i < getItemCount(); i++) {
            VKUser user = getItem(i);
            if (user.getId() == userId) {
                user.setOnline(online);
                user.setLastSeen(time);
                notifyItemChanged(i, -1);
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

            placeholder = new ColorDrawable(Color.TRANSPARENT);

            avatar = v.findViewById(R.id.avatar);
            online = v.findViewById(R.id.online);

            lastSeen = v.findViewById(R.id.last_seen);
            name = v.findViewById(R.id.name);

            message = v.findViewById(R.id.message);
            functions = v.findViewById(R.id.functions);
        }

        void bind(final int position) {
            if (fragment != null) {
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
            } else {
                message.setVisibility(View.GONE);
                functions.setVisibility(View.GONE);
            }

            VKUser user = getItem(position);

            name.setText(user.toString());

            if (user.isOnline()) {
                lastSeen.setVisibility(View.GONE);
                online.setVisibility(View.VISIBLE);
            } else {
                lastSeen.setVisibility(View.VISIBLE);
                online.setVisibility(View.GONE);
            }

            String seen = getString(user.getSex() == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w, Util.dateFormatter.format(user.getLastSeen() * 1000));

            if (lastSeen.getVisibility() == View.VISIBLE) {
                lastSeen.setText(seen);
            } else {
                lastSeen.setText("");
            }

            if (TextUtils.isEmpty(user.getPhoto200())) {
                avatar.setImageDrawable(new ColorDrawable(ColorUtil.alphaColor(ThemeManager.getPrimary(), 0.5f)));
            } else {
                Picasso.get()
                        .load(user.getPhoto200())
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder)
                        .into(avatar);
            }
        }
    }
}
