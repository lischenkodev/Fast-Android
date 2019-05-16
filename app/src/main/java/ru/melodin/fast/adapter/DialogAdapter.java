package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import ru.melodin.fast.R;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKUtils;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;

public class DialogAdapter extends RecyclerAdapter<VKConversation, DialogAdapter.ViewHolder> {

    public DialogAdapter(Context context, ArrayList<VKConversation> values) {
        super(context, values);
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceive(Object[] data) {
        if (ArrayUtil.isEmpty(data)) return;

        int type = (int) data[0];

        switch (type) {
            case 3:
                int mId = (int) data[1];
                readMessage(mId);
                break;
            case 4:
                VKConversation conversation = (VKConversation) data[1];
                addMessage(conversation);
                break;
            case 5:
                VKMessage message = (VKMessage) data[1];
                editMessage(message);
                break;
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
        holder.bind(position);
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
        return item == null ? "" : item.isGroup() ? group.name : item.isUser() ? user.toString() : item.title;
    }

    public String getPhoto(VKConversation item, VKUser user, VKGroup group) {
        return item == null ? "" : item.isGroup() ? group.photo_100 : item.isUser() ? user.photo_200 : item.photo_200;
    }

    String getFromPhoto(VKConversation item, VKUser user, VKGroup group) {
        return item == null ? "" : item.isFromGroup() ? group.photo_100 : item.isFromUser() ? (item.last.out && !item.isChat()) ? UserConfig.user.photo_100 : user.photo_100 : "";
    }

    VKUser searchUser(int userId) {
        VKUser user = MemoryCache.getUser(userId);
        if (user == null)
            user = VKUser.EMPTY;
        return user;
    }

    VKGroup searchGroup(int groupId) {
        VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(groupId));
        if (group == null)
            group = VKGroup.EMPTY;
        return group;
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

    private void addMessage(VKConversation conversation) {
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

            remove(index);
            add(0, conversation);
            notifyDataSetChanged();
        } else {
            if (!conversation.last.out)
                conversation.unread++;
            add(0, conversation);
            notifyDataSetChanged();
        }
    }

    private void readMessage(int id) {
        int position = searchPosition(id);

        if (position == -1) return;

        VKConversation current = getItem(position);
        current.read = true;
        current.unread = 0;

        notifyDataSetChanged();
    }

    private void editMessage(VKMessage edited) {
        int position = searchPosition(edited.id);
        if (position == -1) return;

        VKConversation current = getItem(position);
        VKMessage last = current.last;
        last.mask = edited.mask;
        last.text = edited.text;
        last.update_time = edited.update_time;
        last.attachments = edited.attachments;

        notifyDataSetChanged();
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
        ImageView avatar, avatar_small, online, out;
        TextView title, body, date, counter;
        LinearLayout container;
        FrameLayout counterContainer;

        Drawable holderUser = getDrawable(R.drawable.placeholder_user);
        Drawable holderUsers = getDrawable(R.drawable.placeholder_users);

        @ColorInt
        int pushedEnabled, pushedDisabled;

        ViewHolder(@NonNull View v) {
            super(v);

            pushedEnabled = ThemeManager.getAccent();
            pushedDisabled = ThemeManager.isDark() ? ColorUtil.lightenColor(ThemeManager.getPrimary(), 2) : ColorUtil.darkenColor(ThemeManager.getPrimary(), 2);

            avatar = v.findViewById(R.id.avatar);
            avatar_small = v.findViewById(R.id.avatar_small);
            online = v.findViewById(R.id.online);
            out = v.findViewById(R.id.icon_out_message);

            title = v.findViewById(R.id.title);
            body = v.findViewById(R.id.body);
            date = v.findViewById(R.id.date);
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
            date.setText(Util.dateFormatter.format(last.date * 1000));

            counter.getBackground().setTint(item.isNotificationsDisabled() ? pushedDisabled : pushedEnabled);

            body.setText(last.text);

            title.setText(getTitle(item, peerUser, peerGroup));

            avatar_small.setVisibility(!item.isChat() && !last.out ? View.GONE : View.VISIBLE);

            String peerAvatar = getPhoto(item, peerUser, peerGroup);
            String fromAvatar = getFromPhoto(item, fromUser, fromGroup);

            if (TextUtils.isEmpty(fromAvatar)) {
                avatar_small.setImageDrawable(holderUser);
            } else {
                Picasso.get()
                        .load(fromAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(holderUser)
                        .into(avatar_small);
            }

            if (TextUtils.isEmpty(peerAvatar)) {
                avatar.setImageDrawable(item.isChat() ? holderUser : holderUser);
            } else {
                Picasso.get()
                        .load(peerAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(item.isChat() ? holderUsers : holderUser)
                        .into(avatar);
            }

            body.setTextColor(!ThemeManager.isDark() ? -0x70000000 : -0x6f000001);

            if (TextUtils.isEmpty(last.actionType)) {
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
        }
    }
}
