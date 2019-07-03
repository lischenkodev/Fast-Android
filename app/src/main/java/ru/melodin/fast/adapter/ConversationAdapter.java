package ru.melodin.fast.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.VKUtil;
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
import ru.melodin.fast.fragment.FragmentConversations;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Keys;
import ru.melodin.fast.util.Util;

public class ConversationAdapter extends RecyclerAdapter<VKConversation, ConversationAdapter.ViewHolder> {

    private LinearLayoutManager manager;
    private FragmentConversations fragment;

    private ArrayList<Integer> loadingIds = new ArrayList<>();
    private int lastUpdateId;

    public ConversationAdapter(FragmentConversations fragment, ArrayList<VKConversation> values) {
        super(fragment.getContext(), values);
        this.fragment = fragment;
        manager = (LinearLayoutManager) fragment.getRecyclerView().getLayoutManager();
        UserConfig.getUser();
        EventBus.getDefault().register(this);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
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
            case LongPollEvents.KEY_MESSAGE_CLEAR_FLAGS:
                handleClearFlags(data);
                break;
            case LongPollEvents.KEY_MESSAGE_NEW:
                VKConversation conversation = (VKConversation) data[1];
                addMessage(conversation);
                break;
            case LongPollEvents.KEY_MESSAGE_EDIT:
                VKMessage message = (VKMessage) data[1];
                editMessage(message);
                break;
            case LongPollEvents.KEY_MESSAGE_UPDATE:
                updateMessage((VKMessage) data[1]);
                break;
            case FragmentSettings.KEY_MESSAGES_CLEAR_CACHE:
                clear();
                notifyDataSetChanged();
                fragment.checkCount();
                fragment.onRefresh();
                break;
            case LongPollEvents.KEY_NOTIFICATIONS_CHANGE:
                changeNotifications((int) data[1], (boolean) data[2], (int) data[3]);
                break;
            case Keys.KEY_UPDATE_USER:
                updateUser((int) data[1]);
                break;
            case Keys.KEY_UPDATE_GROUP:
                updateGroup((int) data[1]);
                break;
        }
    }

    private void changeNotifications(int peerId, boolean noSound, int disabledUntil) {
        int position = findConversationPosition(peerId);
        if (position == -1) return;

        VKConversation conversation = getItem(position);
        conversation.setNoSound(noSound);
        conversation.setDisabledUntil(disabledUntil);
        conversation.setDisabledForever(disabledUntil == -1);

        notifyItemChanged(position, -1);

        CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
    }

    private void handleClearFlags(Object[] data) {
        int mId = (int) data[1];
        int flags = (int) data[2];

        if (VKMessage.isUnread(flags))
            readMessage(mId);
    }

    private void updateMessage(VKMessage message) {
        if (message.getId() == lastUpdateId) return;
        lastUpdateId = message.getId();
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.getLast().getId() == message.getId()) {
                conversation.setLast(message);
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    private void updateUser(int userId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation == null) return;
            if (conversation.isUser()) {
                if (conversation.getLast().getPeerId() == userId) {
                    notifyItemChanged(i, -1);
                    break;
                }
            } else if (conversation.isFromUser()) {
                if (conversation.getLast().getFromId() == userId) {
                    notifyItemChanged(i, -1);
                    break;
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
                if (conversation.getLast().getPeerId() == groupId) {
                    notifyItemChanged(i, -1);
                    break;
                }
            } else if (conversation.isFromGroup()) {
                if (conversation.getLast().getFromId() == groupId) {
                    notifyItemChanged(i, -1);
                    break;
                }
            }
        }
    }

    private void addMessage(VKConversation conversation) {
        int firstVisiblePosition = manager.findFirstVisibleItemPosition();

        int index = findConversationPosition(conversation.getLast().getPeerId());
        if (index >= 0) {
            VKConversation current = getItem(index);

            conversation.setPhoto50(current.getPhoto50());
            conversation.setPhoto100(current.getPhoto100());
            conversation.setPhoto200(current.getPhoto200());
            conversation.setPinned(current.getPinned());
            conversation.setTitle(current.getTitle());
            conversation.setCanWrite(current.isCanWrite());
            conversation.setType(current.getType());
            conversation.setUnread(current.getUnread() + 1);
            conversation.setDisabledForever(current.isDisabledForever());
            conversation.setDisabledUntil(current.getDisabledUntil());
            conversation.setNoSound(current.isNoSound());
            conversation.setGroupChannel(current.isGroupChannel());
            conversation.setCanChangeInfo(current.isCanChangeInfo());
            conversation.setCanChangeInviteLink(current.isCanChangeInviteLink());
            conversation.setCanChangePin(current.isCanChangePin());
            conversation.setCanInvite(current.isCanInvite());
            conversation.setCanPromoteUsers(current.isCanPromoteUsers());
            conversation.setCanSeeInviteLink(current.isCanSeeInviteLink());
            conversation.setMembersCount(current.getMembersCount());

            if (conversation.getLast().isOut()) {
                conversation.setUnread(0);
                conversation.setRead(false);
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

            CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, conversation.getLast().getPeerId());
            CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, conversation.getLast(), DatabaseHelper.MESSAGE_ID, conversation.getLast().getId());
        } else {
            if (!conversation.getLast().isOut())
                conversation.setUnread(conversation.getUnread() + 1);
            add(0, conversation);
            notifyItemInserted(0);
            notifyItemRangeChanged(0, getItemCount(), -1);

            if (firstVisiblePosition <= 1)
                manager.scrollToPosition(0);

            CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, conversation);
            CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, conversation.getLast());
        }

        if (conversation.getLast().isNeedUpdate()) {
            loadMessage(conversation.getLast().getId());
        }
    }

    private void loadMessage(final int id) {
        ThreadExecutor.execute(new AsyncCallback(fragment.getActivity()) {
            VKMessage message;

            @Override
            public void ready() throws Exception {
                message = VKApi.messages().getById().messageIds(id).extended(true).filter(VKUser.FIELDS_DEFAULT).execute(VKMessage.class).get(0);
            }

            @Override
            public void done() {
                if (message == null) return;
                CacheStorage.update(DatabaseHelper.MESSAGES_TABLE, message, DatabaseHelper.MESSAGE_ID, id);
                EventBus.getDefault().postSticky(new Object[]{LongPollEvents.KEY_MESSAGE_UPDATE, message});

                updateMessage(message);
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load message", Log.getStackTraceString(e));
            }
        });
    }

    private void readMessage(int id) {
        int position = searchMessagePosition(id);

        if (position == -1) return;

        VKConversation current = getItem(position);
        current.setRead(true);
        current.setUnread(0);

        notifyItemChanged(position, -1);
    }

    private void editMessage(VKMessage edited) {
        int position = searchMessagePosition(edited.getId());
        if (position == -1) return;

        VKConversation current = getItem(position);
        VKMessage last = current.getLast();
        last.setFlags(edited.getFlags());
        last.setText(edited.getText());
        last.setUpdateTime(edited.getUpdateTime());
        last.setAttachments(edited.getAttachments());

        notifyItemChanged(position, -1);
    }

    private void setUserOnline(boolean online, int userId, int time) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.isUser()) {
                VKUser user = MemoryCache.getUser(userId);
                if (user == null) break;
                user.setOnline(online);
                user.setLastSeen(time);
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    private int findConversationPosition(int peerId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.getLast().getPeerId() == peerId)
                return i;
        }

        return -1;
    }

    @Nullable
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    @Override
    protected boolean onQueryItem(VKConversation item, String lowerQuery) {
        if (item == null) return false;

        if (item.isUser()) {
            VKUser user = CacheStorage.getUser(item.getLast().getPeerId());
            if (user == null) return false;
            if (user.toString().toLowerCase().contains(lowerQuery)) return true;
        }

        if (item.isGroup()) {
            VKGroup group = CacheStorage.getGroup(item.getLast().getPeerId());
            if (group == null) return false;
            if (group.getName().toLowerCase().contains(lowerQuery)) return true;
        }

        if (item.isChat() || item.isGroupChannel()) {
            return item.getTitle().toLowerCase().contains(lowerQuery);
        }

        return false;
    }

    public String getTitle(VKConversation item, VKUser user, VKGroup group) {
        int peerId = item.getLast().getPeerId();

        if (peerId > 2_000_000_000) {
            return item.toString();
        } else if (peerId < 0) {
            return group == null ? "Group" : group.toString();
        } else {
            return user == null ? "User" : user.toString();
        }
    }

    public String getPhoto(VKConversation item, VKUser user, VKGroup group) {
        int peerId = item.getLast().getPeerId();

        if (peerId > 2_000_000_000) {
            return item.getPhoto200();
        } else if (peerId < 0) {
            return group == null ? "" : group.getPhoto200();
        } else {
            return user == null ? "" : user.getPhoto200();
        }
    }

    private String getFromPhoto(VKConversation item, VKUser user, VKGroup group) {
        int fromId = item.getLast().getFromId();

        if (fromId < 0) {
            return group == null ? "" : group.getPhoto100();
        } else {
            return item.getLast().isOut() && !item.isChat() ? UserConfig.user.getPhoto100() : user == null ? "" : user.getPhoto100();
        }
    }

    private VKUser searchUser(int userId) {
        VKUser user = MemoryCache.getUser(userId);
        if (user == null) {
            loadUser(userId);
            return VKUser.EMPTY;
        }
        return user;
    }

    private void loadUser(final int userId) {
        if (loadingIds.contains(userId) || userId == 0) return;
        loadingIds.add(userId);
        ThreadExecutor.execute(new AsyncCallback(fragment.getActivity()) {
            VKUser user;

            @Override
            public void ready() throws Exception {
                user = VKApi.users().get().userId(userId).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, user);
                updateUser(userId);
                EventBus.getDefault().postSticky(new Object[]{"update_user", userId});
            }

            @Override
            public void error(Exception e) {
                loadingIds.remove(userId);
                Log.e("Error load user", Log.getStackTraceString(e));
            }
        });
    }

    private VKGroup searchGroup(int groupId) {
        VKGroup group = MemoryCache.getGroup(groupId);
        if (group == null) {
            loadGroup(groupId);
            return VKGroup.EMPTY;
        }
        return group;
    }

    private void loadGroup(final int id) {
        if (loadingIds.contains(id)) return;
        loadingIds.add(id);

        final int groupId = id < 0 ? id * -1 : id;
        ThreadExecutor.execute(new AsyncCallback(fragment.getActivity()) {
            VKGroup group;

            @Override
            public void ready() throws Exception {
                group = VKApi.groups().getById().groupId(groupId).execute(VKGroup.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, group);
                updateGroup(groupId);
                EventBus.getDefault().postSticky(new Object[]{"update_group", groupId});
            }

            @Override
            public void error(Exception e) {
                loadingIds.remove(groupId);
                Log.e("Error load group", Log.getStackTraceString(e));
            }
        });
    }

    private int searchMessagePosition(int mId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage m = getItem(i).getLast();
            if (m.getId() == mId) {
                return i;
            }
        }

        return -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        ImageView avatarSmall;
        ImageView online;
        ImageView out;
        ImageView muted;

        TextView title;
        TextView body;
        TextView time;
        TextView counter;

        LinearLayout container;
        FrameLayout counterContainer;

        Drawable placeholder;

        @ColorInt
        int pushesEnabled, pushesDisabled, accentColor;

        ViewHolder(@NonNull View v) {
            super(v);

            placeholder = getDrawable(R.drawable.avatar_placeholder);

            accentColor = ThemeManager.getAccent();
            pushesEnabled = accentColor;
            pushesDisabled = ThemeManager.isDark() ? ColorUtil.lightenColor(ThemeManager.getPrimary(), 2) : Color.GRAY;

            avatar = v.findViewById(R.id.userAvatar);
            avatarSmall = v.findViewById(R.id.avatar_small);
            online = v.findViewById(R.id.online);
            out = v.findViewById(R.id.icon_out_message);
            muted = v.findViewById(R.id.muted);

            title = v.findViewById(R.id.abc_tb_title);
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

            VKMessage last = item.getLast();

            muted.setVisibility(item.isNotificationsDisabled() ? View.VISIBLE : View.GONE);

            if (last == null) return;

            VKGroup fromGroup = null;
            VKGroup peerGroup = null;

            VKUser fromUser = null;
            VKUser peerUser = null;

            switch (item.getType()) {
                case GROUP:
                    peerGroup = searchGroup(VKGroup.toGroupId(last.getPeerId()));
                    break;
                case USER:
                    peerUser = searchUser(last.getPeerId());
                    break;
            }

            if (peerGroup == null && item.isGroupChannel())
                peerGroup = searchGroup(VKGroup.toGroupId(last.getPeerId()));

            if (item.isFromGroup()) {
                fromGroup = searchGroup(VKGroup.toGroupId(last.getFromId()));
            } else if (item.isFromUser()) {
                fromUser = searchUser(last.getFromId());
            }

            String title_ = getTitle(item, peerUser, peerGroup);
            String peerAvatar = getPhoto(item, peerUser, peerGroup);
            String fromAvatar = getFromPhoto(item, fromUser, fromGroup);

            body.setText(last.getText());
            title.setText(title_);

            counter.setText(item.getUnread() > 0 ? String.valueOf(item.getUnread()) : "");
            time.setText(Util.dateFormatter.format(last.getDate() * 1000));

            counter.getBackground().setTint(item.isNotificationsDisabled() ? pushesDisabled : pushesEnabled);

            if (item.isChat() || last.isOut()) {
                avatarSmall.setVisibility(View.VISIBLE);
            } else {
                avatarSmall.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(peerAvatar)) {
                Picasso.get()
                        .load(peerAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder)
                        .into(avatar);
            } else {
                avatar.setImageDrawable(placeholder);
            }

            if (avatarSmall.getVisibility() == View.VISIBLE)
                if (!TextUtils.isEmpty(fromAvatar)) {
                    Picasso.get()
                            .load(fromAvatar)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(placeholder)
                            .into(avatarSmall);
                } else {
                    avatarSmall.setImageDrawable(placeholder);
                }

            if (last.getAction() == null) {
                if ((last.getAttachments() != null || !ArrayUtil.isEmpty(last.getFwdMessages())) && TextUtils.isEmpty(last.getText())) {
                    String body_ = VKUtil.getAttachmentBody(item.getLast().getAttachments(), item.getLast().getFwdMessages());

                    Spannable span = new SpannableString(body_);
                    span.setSpan(new ForegroundColorSpan(accentColor), 0, body_.length(), 0);

                    body.append(span);
                }
            } else {
                String body_ = VKUtil.getActionBody(last, true);

                Spannable span = new SpannableString(body_);
                span.setSpan(new ForegroundColorSpan(accentColor), 0, body_.length(), 0);
                body.setText(span);
            }

            counter.setVisibility(TextUtils.isEmpty(counter.getText().toString()) ? View.GONE : View.VISIBLE);

            out.setVisibility(last.isOut() && !item.isRead() ? View.VISIBLE : View.GONE);

            online.setImageDrawable(getOnlineIndicator(peerUser));
            online.setVisibility(peerUser == null ? View.GONE : peerUser.isOnline() ? View.VISIBLE : View.GONE);


            counterContainer.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        }

        @Nullable
        private Drawable getOnlineIndicator(VKUser user) {
            return user == null || !user.isOnline() ? null : getDrawable(user.isOnlineMobile() ? R.drawable.ic_online_mobile : R.drawable.ic_online);
        }
    }
}
