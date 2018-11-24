package ru.stwtforever.fast.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKUtils;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.cls.OnItemListener;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.MemoryCache;
import ru.stwtforever.fast.fragment.FragmentDialogs;
import ru.stwtforever.fast.helper.FontHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;

public class DialogAdapter extends BaseRecyclerAdapter<VKConversation, DialogAdapter.ViewHolder> {

    private int position;
    private RecyclerView list;
    private LinearLayoutManager manager;

    private OnItemListener listener;

    public DialogAdapter(FragmentDialogs fr, ArrayList<VKConversation> dialogs) {
        super(fr.getActivity(), dialogs);
        list = fr.getList();
        manager = (LinearLayoutManager) list.getLayoutManager();

        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceive(Object[] data) {
        if (ArrayUtil.isEmpty(data)) return;

        int type = (int) data[0];

        switch (type) {
            case 3:
                readMessage((int) data[1]);
                break;
            case 4:
                addMessage((VKConversation) data[1]);
                break;
            case 5:
                editMessage((VKMessage) data[1]);
                break;
        }
    }

    private int searchUserPosition(int user_id) {
        int index = -1;
        for (int i = 0; i < getMessagesCount(); i++) {
            VKConversation conversation = getItem(i);
            if (conversation.isUser() && conversation.last.peerId == user_id) {
                index = i;
            }
        }

        return index;
    }

    private int searchPosition(VKUser user) {
        for (int i = 0; i < getValues().size(); i++) {
            VKConversation msg = getValues().get(i);
            if (!msg.last.isUser()) return -1;

            if (msg.last.fromId == user.id) return i;
        }

        return -1;
    }

    private void addMessage(VKConversation conversation) {
        int index = searchMessageIndex(conversation.last.peerId);
        if (index >= 0) {
            VKConversation current = getItem(index);

            current.last.id = conversation.last.id;
            current.last.flag = conversation.last.flag;
            current.last.peerId = conversation.last.peerId;
            current.last.date = conversation.last.date;
            current.last.text = conversation.last.text;
            current.last.read = conversation.last.read;
            current.last.out = conversation.last.out;
            current.last.fromId = conversation.last.fromId;

            current.read = conversation.read;
            current.type = conversation.type;
            current.unread++;

            if (current.last.out) {
                current.unread = 0;
                current.read = false;
            }

            conversation = current;

            getValues().remove(index);
            getValues().add(0, conversation);
            notifyDataSetChanged();
        } else {
            if (!conversation.last.out)
                conversation.unread++;
            getValues().add(0, conversation);
            notifyDataSetChanged();
        }
    }

    private void readMessage(int id) {
        int position = searchPosition(id);

        if (position == -1) return;

        VKConversation current = getItem(position);
        current.read = true;
        current.unread = 0;

        notifyItemChanged(position);
    }

    private void editMessage(VKMessage edited) {
        int position = searchPosition(edited.id);
        if (position == -1) return;

        VKConversation current = getValues().get(position);
        VKMessage last = current.last;
        last.mask = edited.mask;
        last.text = edited.text;
        last.update_time = edited.update_time;
        last.attachments = edited.attachments;

        notifyItemChanged(position);
    }

    private int searchPosition(int mId) {
        for (int i = 0; i < getValues().size(); i++) {
            VKMessage m = getValues().get(i).last;
            if (m.id == mId) {
                return i;
            }
        }

        return -1;
    }

    private int searchPosition(VKConversation m) {
        for (int i = 0; i < getValues().size(); i++) {
            VKConversation msg = getValues().get(i);
            if (msg.last.id == m.last.id) {
                return i;
            }
        }

        return -1;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        ImageView avatar_small;
        ImageView online;
        ImageView out;

        TextView title;
        TextView body;
        TextView date;
        TextView counter;

        LinearLayout container;

        DialogAdapter adapter;
        Context context;

        Drawable p_user, p_users;

        int c_pushes_enabled;
        int c_pushes_disabled;

        ViewHolder(Context context, DialogAdapter adapter, View v) {
            super(v);

            this.context = context;
            this.adapter = adapter;

            UserConfig.updateUser();

            c_pushes_enabled = ThemeManager.getAccent();
            c_pushes_disabled = ThemeManager.isDark() ? 0xff454545 : 0xffcccccc;

            p_user = adapter.getDrawable(R.drawable.placeholder_user);
            p_users = adapter.getDrawable(R.drawable.placeholder_users);

            avatar = v.findViewById(R.id.avatar);
            avatar_small = v.findViewById(R.id.avatar_small);
            online = v.findViewById(R.id.online);
            out = v.findViewById(R.id.icon_out_message);

            title = v.findViewById(R.id.title);
            body = v.findViewById(R.id.body);
            date = v.findViewById(R.id.date);
            counter = v.findViewById(R.id.counter);

            container = v.findViewById(R.id.container);

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(ThemeManager.getAccent());
            gd.setCornerRadius(60);

            counter.setBackground(gd);
        }

        public void bind(final int position) {
            VKConversation item = adapter.getItem(position);
            VKMessage last = item.last;
            VKGroup group = adapter.searchGroup(last.fromId);
            VKGroup peerGroup = adapter.searchGroup(last.peerId);

            VKUser user = adapter.searchUser(last.fromId);
            VKUser peerUser = adapter.searchUser(last.peerId);

            FontHelper.setFont(title, FontHelper.PS_REGULAR);

            counter.setText(item.unread > 0 ? String.valueOf(item.unread) : "");
            date.setText(Utils.dateFormatter.format(last.date * 1000));

            counter.getBackground().setTint(item.isNotificationsDisabled() ? c_pushes_disabled : c_pushes_enabled);

            body.setText(last.text);

            title.setText(adapter.getTitle(item, peerUser, peerGroup));

            avatar_small.setVisibility((!item.isChat() && !last.out) ? View.GONE : View.VISIBLE);

            String peerAvatar;

            if (item.isGroup()) {
                peerAvatar = peerGroup.photo_100;
            } else if (item.isUser()) {
                peerAvatar = peerUser.photo_100;
            } else {
                peerAvatar = item.photo_100;
            }

            String fromAvatar;

            if (last.out && !item.isChat()) {
                fromAvatar = UserConfig.user.photo_100;
            } else
                fromAvatar = item.isFromUser() ? user.photo_100 : group.photo_100;

            if (fromAvatar != null)
                if (TextUtils.isEmpty(fromAvatar.trim())) {
                    avatar_small.setImageDrawable(p_user);
                } else {
                    Picasso.get()
                            .load(fromAvatar)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(p_user)
                            .into(avatar_small);
                }

            if (peerAvatar != null)
                if (TextUtils.isEmpty(peerAvatar.trim())) {
                    avatar.setImageDrawable(item.isChat() ? p_users : p_user);
                } else {
                    Picasso.get()
                            .load(peerAvatar)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(item.isChat() ? p_users : p_user)
                            .into(avatar);
                }

            body.setTextColor(!ThemeManager.isDark() ? 0x90000000 : 0x90ffffff);

            if (TextUtils.isEmpty(last.actionType)) {
                if ((last.attachments != null
                        || !ArrayUtil.isEmpty(last.fwd_messages))
                        && TextUtils.isEmpty(last.text)) {
                    String body_ = VKUtils.getAttachmentBody(item.last.attachments, item.last.fwd_messages);

                    String r = "<b>" + body_ + "</b>";
                    SpannableString span = new SpannableString(Html.fromHtml(r));
                    span.setSpan(new ForegroundColorSpan(ThemeManager.getAccent()), 0, body_.length(), 0);

                    body.append(span);
                }
            } else {
                String body_ = VKUtils.getActionBody(last);

                body.setTextColor(ThemeManager.getAccent());
                body.setText(Html.fromHtml(body_));
            }

            counter.setVisibility(!last.out && item.unread > 0 ? View.VISIBLE : View.GONE);
            out.setVisibility(last.out && !item.read ? View.VISIBLE : View.GONE);

            if (item.isGroup() || item.isChat()) {
                online.setVisibility(View.GONE);
            } else {
                if (peerUser.online) {
                    online.setVisibility(View.VISIBLE);
                } else {
                    online.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.fragment_dialogs_list, parent, false);
        return new ViewHolder(context, this, v);
    }

    @Override
    public void onBindViewHolder(DialogAdapter.ViewHolder holder, int position) {
        this.position = position;
        initListener(holder.itemView, position);
        holder.bind(position);
    }

    public int getCurrentPosition() {
        return position;
    }

    public int getMessagesCount() {
        return getValues().size();
    }

    public void add(ArrayList<VKConversation> messages) {
        this.getValues().addAll(messages);
    }

    public void remove(int position) {
        getValues().remove(position);
    }


    public String getTitle(VKConversation item, VKUser user, VKGroup group) {
        if (item == null) return null;

        if (item.isGroup()) {
            if (group != null)
                return TextUtils.isEmpty(group.name) ? "" : group.name;
        } else if (item.isUser()) {
            if (user != null)
                return TextUtils.isEmpty(user.toString()) ? "" : user.toString();
        } else {
            return TextUtils.isEmpty(item.title) ? "" : item.title;
        }

        return "";
    }

    public String getPhoto(VKConversation item, VKUser user, VKGroup group) {
        if (item == null) return null;

        if (item.isUser()) {
            if (user != null)
                return user.photo_100;
        } else if (item.isGroup()) {
            if (group != null)
                return group.photo_100;
        } else {
            return item.photo_100;
        }

        return "";
    }

    public void changeItems(ArrayList<VKConversation> messages) {
        if (!ArrayUtil.isEmpty(messages)) {
            this.getValues().clear();
            this.getValues().addAll(messages);
        }
    }

    public VKUser searchUser(int id) {
        VKUser user = MemoryCache.getUser(id);
        if (user == null) {
            user = VKUser.EMPTY;
        }
        return user;
    }

    public VKGroup searchGroup(int id) {
        VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(id));
        if (group == null) {
            group = VKGroup.EMPTY;
        }
        return group;
    }

    public int searchMessageIndex(int peerId) {
        for (int i = 0; i < getValues().size(); i++) {
            VKConversation conv = getItem(i);
            if (conv.last.peerId == peerId) {
                return i;
            }
        }
        return -1;
    }

    public VKConversation searchMessage(int id) {
        for (VKConversation c : getValues()) {
            if (c.last.id == id) {
                return c;
            }
        }
        return null;
    }

    public void destroy() {
        getValues().clear();
        EventBus.getDefault().unregister(this);
    }

    public static Drawable getOnlineIndicator(Context context, VKUser user) {
        return ContextCompat.getDrawable(context, R.drawable.ic_online_circle);
    }
}
