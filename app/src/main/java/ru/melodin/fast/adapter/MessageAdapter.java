package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AttachmentInflater;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.BoundedLinearLayout;
import ru.melodin.fast.view.CircleImageView;

public class MessageAdapter extends RecyclerAdapter<VKMessage, MessageAdapter.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FOOTER = 10;

    private int peerId;
    private AttachmentInflater attacher;
    private DisplayMetrics metrics;

    private MessagesActivity activity;

    private LinearLayoutManager manager;

    private ArrayList<Integer> loadingIds = new ArrayList<>();

    private SparseArrayCompat<VKMessage> selectedItems = new SparseArrayCompat<>();

    public MessageAdapter(Context context, ArrayList<VKMessage> messages, int peerId) {
        super(context, messages);

        this.activity = (MessagesActivity) context;

        this.peerId = peerId;
        this.attacher = new AttachmentInflater(context);
        this.metrics = context.getResources().getDisplayMetrics();

        manager = (LinearLayoutManager) ((MessagesActivity) context).getRecyclerView().getLayoutManager();
    }

    public boolean isSelected() {
        return getSelectedCount() > 0;
    }

    public void setSelected(int position, boolean selected) {
        VKMessage message = getItem(position);
        message.setSelected(selected);

        if (selected) {
            selectedItems.append(position, message);
        } else {
            selectedItems.remove(position);
        }
    }

    public void clearSelected() {
        for (VKMessage message : getValues())
            message.setSelected(false);

        selectedItems.clear();
    }

    public boolean isSelected(int position) {
        return getItem(position).isSelected();
    }

    public void toggleSelected(int position) {
        VKMessage message = getItem(position);

        boolean selected = !message.isSelected();
        message.setSelected(selected);

        if (selected) {
            selectedItems.append(position, message);
        } else {
            selectedItems.remove(position);
        }
    }

    public int getSelectedCount() {
        return selectedItems.size();
    }

    public ArrayList<VKMessage> getSelectedMessages() {
        ArrayList<VKMessage> messages = new ArrayList<>();

        for (VKMessage message : getValues())
            if (message.isSelected())
                messages.add(message);

        return messages;
    }

    public SparseArrayCompat<VKMessage> getSelectedItems() {
        return selectedItems;
    }

    public void readNewMessage(final VKMessage message) {
        if (message.isOut()) return;
        ThreadExecutor.execute(new AsyncCallback(((MessagesActivity) context)) {
            @Override
            public void ready() throws Exception {
                VKApi.messages().markAsRead().peerId(message.getPeerId()).execute(Integer.class);
            }

            @Override
            public void done() {
                readMessage(message.getId());
            }

            @Override
            public void error(Exception e) {
                Log.e("Error read message", Log.getStackTraceString(e));
            }
        });
    }

    public void handleClearFlags(Object[] data) {
        int mId = (int) data[1];
        int flags = (int) data[2];
        int peerId = (int) data[3];

        if (peerId != this.peerId) return;

        if (VKMessage.isImportant(flags))
            importantMessage(false, mId);

        if (VKMessage.isUnread(flags))
            readMessage(mId);
    }

    public void handleSetFlags(Object[] data) {
        int mId = (int) data[1];
        int flags = (int) data[2];
        int peerId = (int) data[3];

        if (peerId != this.peerId) return;

        if (VKMessage.isImportant(flags))
            importantMessage(true, mId);

        if (VKMessage.isUnread(flags))
            readMessage(mId);
    }

    public boolean contains(int id) {
        return searchPosition(id) != -1;
    }

    private void importantMessage(boolean important, int mId) {
        int position = searchPosition(mId);
        if (position == -1) return;

        VKMessage message = getItem(position);

        message.setImportant(important);
        notifyItemChanged(position, -1);
    }

    public void updateMessage(VKMessage msg) {
        int position = searchPosition(msg.getId());
        if (position == -1) return;

        remove(position);
        add(position, msg);
        notifyItemChanged(position, -1);
    }

    public void editMessage(VKMessage edited) {
        int position = searchPosition(edited.getId());
        if (position == -1) return;

        remove(position);
        add(position, edited);
        notifyItemChanged(position, -1);
    }

    public void addMessage(VKMessage msg, boolean anim) {
        if (msg.getPeerId() != peerId || (msg.getRandomId() != 0 && containsRandom(msg.getRandomId())))
            return;

        add(msg);

        if (anim)
            notifyItemInserted(getItemCount() - 1);
    }

    private boolean containsRandom(long randomId) {
        for (VKMessage message : getValues())
            if (message.getRandomId() == randomId)
                return true;
        return false;
    }

    private void readMessage(int mId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.getId() == mId) {
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (getValues().size() == position) {
            return TYPE_FOOTER;
        } else {
            return TYPE_NORMAL;
        }
    }

    @Override
    public VKMessage getItem(int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            return super.getItem(position - 1);
        }
        return super.getItem(position);
    }

    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            return new FooterViewHolder(createFooter());
        }

        View v = inflater.inflate(R.layout.activity_messages_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        if (holder.isFooter()) return;
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    public int searchPosition(int mId) {
        for (int i = 0; i < getItemCount(); i++)
            if (getItem(i).getId() == mId) return i;

        return -1;
    }

    private VKUser searchUser(int id) {
        VKUser user = MemoryCache.getUser(id);

        if (user == null) {
            loadUser(id);
            return VKUser.EMPTY;
        }

        return user;
    }

    private VKGroup searchGroup(int id) {
        VKGroup group = MemoryCache.getGroup(id);

        if (group == null) {
            loadGroup(id);
            return VKGroup.EMPTY;
        }

        return group;
    }

    private void loadUser(Integer id) {
        if (loadingIds.contains(id)) return;
        if (id == 0) return;
        if (id < 0)
            id *= -1;

        loadingIds.add(id);

        final Integer userId = id;

        ThreadExecutor.execute(new AsyncCallback(((MessagesActivity) context)) {
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

    private void loadGroup(final Integer groupId) {
        if (loadingIds.contains(groupId)) return;
        loadingIds.add(groupId);
        ThreadExecutor.execute(new AsyncCallback(((MessagesActivity) context)) {
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

    private void updateUser(int userId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.getFromId() == userId) {
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    private void updateGroup(int groupId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.getFromId() == groupId) {
                notifyItemChanged(i, -1);
                break;
            }
        }
    }


    private void onAvatarLongClick(int position) {
        VKUser user = CacheStorage.getUser(getItem(position).getFromId());
        if (user == null) return;

        Toast.makeText(context, user.toString(), Toast.LENGTH_SHORT).show();
    }

    class FooterViewHolder extends ViewHolder {

        FooterViewHolder(View v) {
            super(v);
        }

        @Override
        public boolean isFooter() {
            return true;
        }

    }

    private View createFooter() {
        View footer = new View(context);
        footer.setVisibility(View.VISIBLE);
        footer.setBackgroundColor(Color.TRANSPARENT);
        footer.setVisibility(View.INVISIBLE);
        footer.setEnabled(false);
        footer.setClickable(false);
        footer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) Util.px(66)));

        return footer;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        ImageView read;
        ImageView important;

        TextView text;
        TextView time;

        LinearLayout mainContainer;

        BoundedLinearLayout bubble;
        LinearLayout attachments;
        LinearLayout photos;
        LinearLayout serviceContainer;
        LinearLayout messageContainer;
        LinearLayout timeContainer;
        LinearLayout bubbleContainer;

        Space space;

        Drawable circle, sending, placeholder;
        @ColorInt
        int alphaAccentColor;

        public boolean isFooter() {
            return false;
        }

        ViewHolder(View v) {
            super(v);

            alphaAccentColor = ColorUtil.alphaColor(ThemeManager.getAccent(), 0.3f);

            space = v.findViewById(R.id.space);
            text = v.findViewById(R.id.text);
            time = v.findViewById(R.id.time);

            placeholder = new ColorDrawable(Color.TRANSPARENT);

            avatar = v.findViewById(R.id.avatar);
            read = v.findViewById(R.id.read_indicator);
            important = v.findViewById(R.id.important);

            circle = new ColorDrawable(ThemeManager.getAccent());
            sending = getDrawable(R.drawable.ic_vector_access_time);

            bubbleContainer = v.findViewById(R.id.bubble_container);
            messageContainer = v.findViewById(R.id.message_container);
            serviceContainer = v.findViewById(R.id.service_container);
            mainContainer = v.findViewById(R.id.root);
            bubble = v.findViewById(R.id.bubble);
            attachments = v.findViewById(R.id.attachments);
            photos = v.findViewById(R.id.photos);
            timeContainer = v.findViewById(R.id.time_container);
        }

        void bind(final int position) {
            final VKMessage item = getItem(position);
            final VKUser user = searchUser(item.getFromId());
            final VKGroup group = searchGroup(VKGroup.toGroupId(item.getFromId()));

            itemView.setBackgroundColor(item.isSelected() ? alphaAccentColor : 0);


            read.setVisibility(item.isOut() ? item.isRead() ? View.GONE : View.GONE : View.GONE);

            String s = item.getUpdateTime() > 0 ? getString(R.string.edited) + ", " : "";
            String time_ = s + Util.dateFormatter.format(item.isAdded() ? item.getDate() : item.getDate() * 1000L);

            time.setText(time_);

            int gravity = item.isOut() ? Gravity.END : Gravity.START;

            timeContainer.setGravity(gravity);
            bubbleContainer.setGravity(gravity);

            ViewUtil.fadeView(important, item.isImportant(),
                    new Runnable() {
                        @Override
                        public void run() {
                            if (item.isImportant())
                                important.setVisibility(View.VISIBLE);
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            if (!item.isImportant())
                                important.setVisibility(View.GONE);
                        }
                    });

            bubble.setVisibility(View.VISIBLE);

            String avatar_link = item.isFromGroup() ? group.photo_100 : user.photo_100;

            if (item.isFromUser()) {
                avatar_link = user.photo_100;
            } else if (item.isFromGroup()) {
                avatar_link = group.photo_100;
            }

            if (TextUtils.isEmpty(avatar_link)) {
                avatar.setImageDrawable(placeholder);
            } else {
                Picasso.get()
                        .load(avatar_link)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(placeholder)
                        .into(avatar);
            }

            avatar.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View p1) {
                    onAvatarLongClick(position);
                    return true;
                }
            });

            time.setGravity(item.isOut() ? Gravity.END : Gravity.START);
            messageContainer.setGravity(item.isOut() ? Gravity.END : Gravity.START);

            if (TextUtils.isEmpty(item.getText())) {
                text.setText("");
                text.setVisibility(View.GONE);
            } else {
                text.setVisibility(View.VISIBLE);
                text.setText(item.getText().trim());
            }

            int textColor, timeColor, bgColor, linkColor;

            if (item.isOut()) {
                textColor = Color.WHITE;
                bgColor = ThemeManager.getAccent();
                linkColor = Color.WHITE;
            } else {
                linkColor = ThemeManager.getAccent();
                if (ThemeManager.isDark()) {
                    textColor = 0xffeeeeee;
                    bgColor = 0xff404040;
                } else {
                    textColor = 0xff1d1d1d;
                    bgColor = Color.WHITE;
                }
            }

            timeColor = ThemeManager.isDark() ? 0xffdddddd : 0xff404040;

            text.setTextColor(textColor);
            text.setLinkTextColor(linkColor);
            time.setTextColor(timeColor);

            bubble.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

            Drawable bg = context.getResources().getDrawable(R.drawable.msg_in_bg);

            if (item.getAction() != null) {
                avatar.setVisibility(View.GONE);
                messageContainer.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                serviceContainer.setVisibility(View.VISIBLE);

                serviceContainer.removeAllViews();
                attacher.service(item, serviceContainer);

                bg.setTint(Color.TRANSPARENT);
            } else {
                avatar.setVisibility(View.VISIBLE);
                time.setVisibility(View.VISIBLE);
                messageContainer.setVisibility(View.VISIBLE);
                serviceContainer.setVisibility(View.GONE);
                bg.setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
            }

            bubble.setBackground(bg);

            if (!ArrayUtil.isEmpty(item.getFwdMessages()) || !ArrayUtil.isEmpty(item.getAttachments())) {
                attachments.setVisibility(View.VISIBLE);
                attachments.removeAllViews();

                photos.setVisibility(View.VISIBLE);
                photos.removeAllViews();
            } else {
                attachments.setVisibility(View.GONE);
                photos.setVisibility(View.GONE);
            }

            if (!ArrayUtil.isEmpty(item.getAttachments())) {
                attacher.inflateAttachments(item, attachments, photos, -1, false, true);
            }

            if (!ArrayUtil.isEmpty(item.getFwdMessages())) {
                attacher.showForwardedMessages(item, attachments, false, true);
            } else if (item.getReply() != null) {
                attacher.showForwardedMessages(item, attachments, true, true);
            }

            avatar.setVisibility(item.isOut() ? View.GONE : View.VISIBLE);
            space.setVisibility(avatar.getVisibility());
        }
    }


}
