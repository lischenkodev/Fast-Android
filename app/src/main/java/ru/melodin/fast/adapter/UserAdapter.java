package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import ru.melodin.fast.util.Keys;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class UserAdapter extends RecyclerAdapter<VKUser, UserAdapter.ViewHolder> {

    private FragmentFriends fragment;

    public UserAdapter(@NonNull FragmentFriends context, ArrayList<VKUser> friends) {
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
                setUserOnline(false, (long) data[1], (int) data[2]);
                break;
            case LongPollEvents.KEY_USER_ONLINE:
                setUserOnline(true, (long) data[1], (int) data[2]);
                break;
            case Keys.CONNECTED:
                if (fragment == null) return;
                if (fragment.isLoading())
                    fragment.setLoading(false);
                fragment.onRefresh();
                break;
        }
    }

    private void setUserOnline(boolean online, long userId, int time) {
        for (int i = 0; i < getItemCount(); i++) {
            VKUser user = getItem(i);
            if (user.getId() == userId) {
                user.setOnline(online);
                user.setLastSeen(time);
                notifyItemChanged(i, -1);
            }
        }
    }

    class ViewHolder extends RecyclerHolder {

        CircleImageView avatar;
        ImageView online;

        TextView name;
        TextView lastSeen;

        ImageButton message;
        ImageButton functions;


        Drawable placeholder;

        ViewHolder(View v) {
            super(v);

            placeholder = getDrawable(R.drawable.avatar_placeholder);

            avatar = v.findViewById(R.id.userAvatar);
            online = v.findViewById(R.id.online);

            lastSeen = v.findViewById(R.id.lastSeen);
            name = v.findViewById(R.id.name);

            message = v.findViewById(R.id.message);
            functions = v.findViewById(R.id.functions);
        }

        @Override
        public void bind(final int position) {
            if (fragment != null) {
                message.setOnClickListener(v -> fragment.openChat(position));

                functions.setOnClickListener(v -> fragment.showDialog(position, v));
            } else {
                message.setVisibility(View.GONE);
                functions.setVisibility(View.GONE);
            }

            VKUser user = getItem(position);

            name.setText(user.toString());

            online.setImageDrawable(getOnlineIndicator(user));

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

        @Nullable
        private Drawable getOnlineIndicator(@NonNull VKUser user) {
            return !user.isOnline() ? null : getDrawable(user.isOnlineMobile() ? R.drawable.ic_online_mobile : R.drawable.ic_online);
        }
    }
}
