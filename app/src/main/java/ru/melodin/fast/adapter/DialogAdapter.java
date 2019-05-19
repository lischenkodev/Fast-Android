package ru.melodin.fast.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.VKUtils;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.fragment.FragmentDialogs;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.service.LongPollService;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;

public class DialogAdapter extends RecyclerAdapter<VKConversation, DialogAdapter.ViewHolder> {

    private LinearLayoutManager manager;
    private FragmentDialogs fragment;

    public DialogAdapter(FragmentDialogs fragment, ArrayList<VKConversation> values) {
        super(fragment.getContext(), values);
        this.fragment = fragment;
        manager = (LinearLayoutManager) fragment.getRecyclerView().getLayoutManager();
        EventBus.getDefault().register(this);
        UserConfig.getUser();
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
            case LongPollService.KEY_MESSAGE_CLEAR_FLAGS:
                handleClearFlags(data);
                break;
            case LongPollService.KEY_MESSAGE_NEW:
                VKConversation conversation = (VKConversation) data[1];
                addMessage(conversation);
                break;
            case LongPollService.KEY_MESSAGE_EDIT:
                VKMessage message = (VKMessage) data[1];
                editMessage(message);
                break;
            case "update_user":
                updateUser((int) data[1]);
                break;
            case "update_group":
                updateGroup((int) data[1]);
                break;
            case FragmentSettings.KEY_CLEAR_CACHE:
                clear();
                notifyDataSetChanged();
                fragment.checkCount();
                fragment.onRefresh();
                break;
        }
    }

    private void handleClearFlags(Object[] data) {
        int mId = (int) data[1];
        int flags = (int) data[2];

        if (VKMessage.isUnread(flags))
            readMessage(mId);
    }

    private void updateUser(int userId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.isUser()) {
                if (conversation.last.peerId == userId) {
                    notifyItemChanged(i, -1);
                }
            } else if (conversation.isFromUser()) {
                if (conversation.last.fromId == userId) {
                    notifyItemChanged(i, -1);
                }
            }
        }
    }

    private void updateGroup(int groupId) {
        if (groupId > 0)
            groupId *= -1;
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.isGroup()) {
                if (conversation.last.peerId == groupId) {
                    notifyItemChanged(i, -1);
                }
            } else if (conversation.isFromGroup()) {
                if (conversation.last.fromId == groupId) {
                    notifyItemChanged(i, -1);
                }
            }
        }
    }

    private void addMessage(VKConversation conversation) {
        int firstVisiblePosition = manager.findFirstVisibleItemPosition();
        //int totalVisibleItems = manager.findLastCompletelyVisibleItemPosition() + 1;

        int index = searchMessagePosition(conversation.last.peerId);
        if (index >= 0) {
            VKConversation current = getItem(index);

            conversation.photo_50 = current.photo_50;
            conversation.photo_100 = current.photo_100;
            conversation.photo_200 = current.photo_200;
            conversation.pinned = current.pinned;
            conversation.title = current.title;
            conversation.can_write = current.can_write;
            conversation.type = current.type;
            conversation.unread = current.unread + 1;
            conversation.disabled_forever = current.disabled_forever;
            conversation.disabled_until = current.disabled_until;
            conversation.no_sound = current.no_sound;
            conversation.group_channel = current.group_channel;
            conversation.can_change_info = current.can_change_info;
            conversation.can_change_invite_link = current.can_change_invite_link;
            conversation.can_change_pin = current.can_change_pin;
            conversation.can_invite = current.can_invite;
            conversation.can_promote_users = current.can_promote_users;
            conversation.can_see_invite_link = current.can_see_invite_link;
            conversation.membersCount = current.membersCount;

            if (conversation.last.out) {
                conversation.unread = 0;
                conversation.read = false;
            }

            if (index > 0) {
                remove(index);
                add(0, conversation);
                notifyItemMoved(index, 0);
                notifyItemRangeChanged(0, getItemCount(), -1);

                if (firstVisiblePosition <= 1)
                    manager.scrollToPosition(0);
            } else {
                remove(0);
                add(0, conversation);
                notifyItemChanged(0, -1);
            }
        } else {
            if (!conversation.last.out)
                conversation.unread++;
            add(0, conversation);
            notifyItemInserted(0);
            notifyItemRangeChanged(0, getItemCount(), -1);

            if (firstVisiblePosition <= 1)
                manager.scrollToPosition(0);

            CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, conversation);
        }

        CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, conversation.last);
    }

    private void readMessage(int id) {
        int position = searchPosition(id);

        if (position == -1) return;

        VKConversation current = getItem(position);
        current.read = true;
        current.unread = 0;

        notifyItemChanged(position, -1);
    }

    private void editMessage(VKMessage edited) {
        int position = searchPosition(edited.id);
        if (position == -1) return;

        VKConversation current = getItem(position);
        VKMessage last = current.last;
        last.flags = edited.flags;
        last.text = edited.text;
        last.update_time = edited.update_time;
        last.attachments = edited.attachments;

        notifyItemChanged(position, -1);
    }

    private void setUserOnline(boolean online, int userId, int time) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.isUser()) {
                VKUser user = MemoryCache.getUser(userId);
                if (user == null) break;
                user.online = online;
                user.last_seen = time;
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.fragment_dialogs_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        try {
            holder.bind(position);
        } catch (Exception e) {
            Log.e("Error bind holder", Log.getStackTraceString(e));
        }
    }

    @Override
    protected boolean onQueryItem(VKConversation item, String lowerQuery) {
        if (item == null) return false;

        if (item.isUser()) {
            VKUser user = CacheStorage.getUser(item.last.peerId);
            if (user == null) return false;
            if (user.toString().toLowerCase().contains(lowerQuery)) return true;
        }

        if (item.isGroup()) {
            VKGroup group = CacheStorage.getGroup(item.last.peerId);
            if (group == null) return false;
            if (group.name.toLowerCase().contains(lowerQuery)) return true;
        }

        if (item.isChat() || item.group_channel) {
            return item.title.toLowerCase().contains(lowerQuery);
        }

        return false;
    }

    public String getTitle(VKConversation item, VKUser user, VKGroup group) {
        return item.isGroup() ? group.name : item.isUser() ? user.toString() : item.title;
    }

    public String getPhoto(VKConversation item, VKUser user, VKGroup group) {
        return item.isGroup() ? group.photo_200 : item.isUser() ? user.photo_200 : item.photo_200;
    }

    private String getFromPhoto(VKConversation item, VKUser user, VKGroup group) {
        return item.isFromGroup() ? group.photo_100 : item.isFromUser() ? (item.last.out && !item.isChat()) ? UserConfig.user.photo_100 : user.photo_100 : "";
    }

    private VKUser searchUser(int userId) {
        if (userId <= 0) return VKUser.EMPTY;
        VKUser user = MemoryCache.getUser(userId);
        if (user == null) {
            user = VKUser.EMPTY;
            loadUser(userId);
        }
        return user;
    }

    private void loadUser(final int userId) {
        ThreadExecutor.execute(new AsyncCallback(fragment.getActivity()) {
            VKUser user;

            @Override
            public void ready() throws Exception {
                user = VKApi.users().get().userId(userId).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, user);
                EventBus.getDefault().postSticky(new Object[]{"update_user", userId});
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load user", Log.getStackTraceString(e));
            }
        });
    }

    private VKGroup searchGroup(int groupId) {
        if (groupId >= 0) return VKGroup.EMPTY;
        VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(groupId));
        if (group == null) {
            group = VKGroup.EMPTY;
            loadGroup(VKGroup.toGroupId(groupId));
        }
        return group;
    }

    private void loadGroup(final int groupId) {
        ThreadExecutor.execute(new AsyncCallback(fragment.getActivity()) {
            VKGroup group;

            @Override
            public void ready() throws Exception {
                group = VKApi.groups().getById().groupId(groupId).execute(VKGroup.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, group);
                EventBus.getDefault().postSticky(new Object[]{"update_group", groupId});
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load group", Log.getStackTraceString(e));
            }
        });
    }

    private int searchMessagePosition(int peerId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.last.peerId == peerId) {
                return i;
            }
        }
        return -1;
    }

    private int searchPosition(int mId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage m = getItem(i).last;
            if (m.id == mId) {
                return i;
            }
        }

        return -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar, avatarSmall, online, out;
        TextView title, body, time, counter;
        LinearLayout container;
        FrameLayout counterContainer;

        Drawable holderUser = getDrawable(R.drawable.placeholder_user);
        Drawable holderUsers = getDrawable(R.drawable.placeholder_users);

        @ColorInt
        int pushesEnabled, pushesDisabled;

        ViewHolder(@NonNull View v) {
            super(v);

            pushesEnabled = ThemeManager.getAccent();
            pushesDisabled = ThemeManager.isDark() ? ColorUtil.lightenColor(ThemeManager.getPrimary(), 2) : Color.GRAY;

            avatar = v.findViewById(R.id.avatar);
            avatarSmall = v.findViewById(R.id.avatar_small);
            online = v.findViewById(R.id.online);
            out = v.findViewById(R.id.icon_out_message);

            title = v.findViewById(R.id.title);
            body = v.findViewById(R.id.body);
            time = v.findViewById(R.id.date);
            counter = v.findViewById(R.id.counter);

            container = v.findViewById(R.id.container);
            counterContainer = v.findViewById(R.id.counter_container);

            GradientDrawable background = new GradientDrawable();
            background.setColor(ThemeManager.getAccent());
            background.setCornerRadius(200f);

            counter.setBackground(background);
        }

        void bind(int position) {
            VKConversation item = getItem(position);
            VKMessage last = item.last;

            VKGroup fromGroup = searchGroup(last.fromId);
            VKGroup peerGroup = searchGroup(last.peerId);

            VKUser fromUser = searchUser(last.fromId);
            VKUser peerUser = searchUser(last.peerId);

            counter.setText(item.unread > 0 ? String.valueOf(item.unread) : "");
            time.setText(Util.dateFormatter.format(last.date * 1000));

            counter.getBackground().setTint(item.isNotificationsDisabled() ? pushesDisabled : pushesEnabled);

            body.setText(last.text);

            avatarSmall.setVisibility((!item.isChat() && !last.out) || item.group_channel ? View.GONE : View.VISIBLE);

            String peerAvatar = getPhoto(item, peerUser, peerGroup);
            String fromAvatar = getFromPhoto(item, fromUser, fromGroup);

            Picasso.get()
                    .load(peerAvatar)
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(item.isChat() ? holderUsers : holderUser)
                    .into(avatar);

            Picasso.get()
                    .load(fromAvatar)
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(item.isChat() ? holderUsers : holderUser)
                    .into(avatarSmall);

            body.setTextColor(!ThemeManager.isDark() ? -0x70000000 : -0x6f000001);

            if (last.action == null) {
                if ((last.attachments != null || !ArrayUtil.isEmpty(last.fwd_messages)) && TextUtils.isEmpty(last.text)) {
                    String body_ = VKUtils.getAttachmentBody(item.last.attachments, item.last.fwd_messages);

                    String r = "<b>" + body_ + "</b>";
                    Spannable span = new SpannableString(Html.fromHtml(r));
                    span.setSpan(new ForegroundColorSpan(ThemeManager.getAccent()), 0, body_.length(), 0);

                    body.append(span);
                }
            } else {
                String body_ = VKUtils.getActionBody(last, true);

                body.setTextColor(ThemeManager.getAccent());
                body.setText(Html.fromHtml(body_));
            }

            counter.setVisibility(TextUtils.isEmpty(counter.getText().toString()) ? View.GONE : View.VISIBLE);
            out.setVisibility(last.out && !item.read ? View.VISIBLE : View.GONE);
            online.setVisibility(peerUser.online ? View.VISIBLE : View.GONE);
            counterContainer.setVisibility(item.read ? View.GONE : View.VISIBLE);

            title.setText(getTitle(item, peerUser, peerGroup));
        }
    }
}
