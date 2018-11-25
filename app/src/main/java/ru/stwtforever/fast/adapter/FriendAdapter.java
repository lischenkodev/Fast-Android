package ru.stwtforever.fast.adapter;

import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.widget.*;

import com.squareup.picasso.*;

import java.util.*;

import androidx.recyclerview.widget.RecyclerView;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.cls.*;
import ru.stwtforever.fast.fragment.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.view.*;

import androidx.appcompat.widget.PopupMenu;

import ru.stwtforever.fast.util.Utils;

public class FriendAdapter extends BaseRecyclerAdapter<VKUser, FriendAdapter.ViewHolder> {

    private int position;

    private FragmentFriends fragment;

    private OnItemListener listener;

    public FriendAdapter(FragmentFriends fragment, ArrayList<VKUser> friends) {
        super(fragment.getActivity(), friends);
        this.fragment = fragment;
    }

    private void initListener(View v, final int position) {
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.OnItemClick(v, position);
            }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                listener.onItemLongClick(v, position);
                return true;
            }
        });
    }

    public void setListener(OnItemListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.fragment_friends_list, parent, false);
        return new ViewHolder(fragment, FriendAdapter.this, v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        this.position = position;
        initListener(holder.itemView, position);
        holder.bind(position);
    }

    private void showFuncsDialog(final int position, View v) {
        PopupMenu m = new PopupMenu(context, v);
        m.inflate(R.menu.fragment_friends_funcs);
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.remove_friend:

                        break;
                }
                return true;
            }

        });
        m.show();
    }

    public int getPosition() {
        return position;
    }

    public int getFriendsSize() {
        return getValues().size();
    }

    public void add(ArrayList<VKUser> friends) {
        this.getValues().addAll(friends);
    }

    public void remove(int position) {
        getValues().remove(position);
    }

    public void changeItems(ArrayList<VKUser> friends) {
        if (!ArrayUtil.isEmpty(friends)) {
            this.getValues().clear();
            this.getValues().addAll(friends);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        CircleImageView online;

        TextView name;
        TextView lastSeen;

        ImageButton message;
        ImageButton funcs;

        FriendAdapter adapter;
        FragmentFriends ff;

        Drawable placeholder;

        ViewHolder(FragmentFriends ff, FriendAdapter adapter, View v) {
            super(v);

            this.ff = ff;
            this.adapter = adapter;

            placeholder = adapter.getDrawable(R.drawable.placeholder_user);

            avatar = v.findViewById(R.id.avatar);
            online = v.findViewById(R.id.online);

            lastSeen = v.findViewById(R.id.last_seen);
            name = v.findViewById(R.id.name);

            message = v.findViewById(R.id.message);
            funcs = v.findViewById(R.id.funcs);
        }

        public void bind(final int position) {
            message.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ff.openChat(position);
                }

            });


            funcs.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    adapter.showFuncsDialog(position, v);
                }

            });

            VKUser user = adapter.getItem(position);

            name.setText(user.toString());

            if (user.online) {
                lastSeen.setVisibility(View.GONE);
                online.setVisibility(View.VISIBLE);
            } else {
                lastSeen.setVisibility(View.VISIBLE);
                online.setVisibility(View.GONE);
            }

            String seen_text = adapter.context.getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w);

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
