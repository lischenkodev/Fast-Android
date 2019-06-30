package ru.melodin.fast.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;

import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKAudio;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKDoc;
import ru.melodin.fast.api.model.VKGraffiti;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKLink;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.api.model.VKPhoto;
import ru.melodin.fast.api.model.VKSticker;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.api.model.VKVideo;
import ru.melodin.fast.api.model.VKVoice;
import ru.melodin.fast.api.model.VKWall;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.AttachmentInflater;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Keys;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.BoundedLinearLayout;
import ru.melodin.fast.view.CircleImageView;
import ru.melodin.fast.view.TimeTextView;

public class MessageAdapter extends RecyclerAdapter<VKMessage, MessageAdapter.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FOOTER = 10;

    private MediaPlayer mediaPlayer;
    private int playingId = -1;

    private int peerId;
    private AttachmentInflater attacher;
    private DisplayMetrics metrics;

    private MessagesActivity activity;

    private LinearLayoutManager layoutManager;

    private ArrayList<Integer> loadingIds = new ArrayList<>();

    private SparseArrayCompat<VKMessage> selectedItems = new SparseArrayCompat<>();

    public MessageAdapter(Context context, ArrayList<VKMessage> messages, int peerId) {
        super(context, messages);

        this.activity = (MessagesActivity) context;

        this.peerId = peerId;
        this.attacher = new AttachmentInflater(context);
        this.metrics = context.getResources().getDisplayMetrics();

        layoutManager = (LinearLayoutManager) ((MessagesActivity) context).getRecyclerView().getLayoutManager();

        EventBus.getDefault().register(this);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void destroy() {
        releaseMediaPlayer();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onReceive(final Object[] data) {
        String key = (String) data[0];

        switch (key) {
            case AttachmentInflater.KEY_PLAY_AUDIO:
                final int messageId = (int) data[1];

                if (mediaPlayer == null) {
                    releaseMediaPlayer();
                    initMediaPlayer((int) data[1], (String) data[2]);
                } else {
                    if (mediaPlayer.isPlaying()) {
                        releaseMediaPlayer();
                        setPlaying(playingId, false);
                        playingId = -1;
                        initMediaPlayer((int) data[1], (String) data[2]);
                    } else {
                        mediaPlayer.start();
                        setPlaying(messageId, true);
                    }
                }
                break;
            case AttachmentInflater.KEY_PAUSE_AUDIO:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();

                    final int mId = (int) data[1];
                    setPlaying(mId, false);
                }
                break;
            case LongPollEvents.KEY_USER_OFFLINE:
            case LongPollEvents.KEY_USER_ONLINE:
                activity.setUserOnline((int) data[1]);
                break;
            case LongPollEvents.KEY_MESSAGE_CLEAR_FLAGS:
                handleClearFlags(data);
                break;
            case LongPollEvents.KEY_MESSAGE_SET_FLAGS:
                handleSetFlags(data);
                break;
            case LongPollEvents.KEY_MESSAGE_NEW:
                VKConversation conversation = (VKConversation) data[1];

                if (peerId != conversation.getLast().getPeerId()) return;

                VKMessage last = conversation.getLast();

                if (last.getAction() == VKMessage.Action.PIN_MESSAGE) {
                    int position = findPosition(last.getActionId());
                    Log.d("ActionId", last.getActionId() + "");
                    if (position != -1)
                        activity.showPinned(getItem(position));
                } else if (last.getAction() == VKMessage.Action.UNPIN_MESSAGE) {
                    activity.showPinned(null);
                }

                addMessage(last, true);

                int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisibleItem >= getItemCount() - 4) {
                    activity.getRecyclerView().smoothScrollToPosition(getItemCount() - 1);
                }

                if (!last.isOut() && last.getPeerId() == peerId && !AppGlobal.preferences.getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
                    if (!activity.resumed) {
                        activity.notRead = last;
                    } else {
                        readNewMessage(last);
                    }
                }

                break;
            case LongPollEvents.KEY_MESSAGE_EDIT:
                editMessage((VKMessage) data[1]);
                break;
            case LongPollEvents.KEY_MESSAGE_UPDATE:
                updateMessage((VKMessage) data[1]);
                break;
        }
    }

    private void initMediaPlayer(final int messageId, String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                releaseMediaPlayer();
                EventBus.getDefault().postSticky(new Object[]{AttachmentInflater.KEY_STOP_AUDIO});
                setPlaying(messageId, false);
            });
            mediaPlayer.setOnErrorListener((mediaPlayer, i, i1) -> {
                playingId = -1;
                releaseMediaPlayer();
                setPlaying(messageId, false);
                return false;
            });
            mediaPlayer.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.start();
                playingId = messageId;
                setPlaying(messageId, true);
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPlaying(int messageId, boolean playing) {
        int position = findPosition(messageId);
        if (position == -1) return;

        VKMessage message = getItem(position);
        message.setPlaying(playing);
        notifyItemChanged(position, -1);
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

    private int findPosition(int mId) {
        for (int i = 0; i < getValues().size(); i++) {
            VKMessage message = getItem(i);
            if (message.getId() == mId)
                return i;
        }

        return -1;
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
        if (message.isOut() || layoutManager.findLastCompletelyVisibleItemPosition() < searchPosition(message.getId()))
            return;
        ThreadExecutor.execute(new AsyncCallback(((MessagesActivity) context)) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.messages().markAsRead().messageIds(message.getId()).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (response == 1)
                    readMessage(message.getId());
            }

            @Override
            public void error(Exception e) {
                Log.e("Error read message", Log.getStackTraceString(e));
            }
        });
    }

    private void handleClearFlags(Object[] data) {
        int mId = (int) data[1];
        int flags = (int) data[2];
        int peerId = (int) data[3];

        if (peerId != this.peerId) return;

        if (VKMessage.isImportant(flags))
            importantMessage(false, mId);

        if (VKMessage.isUnread(flags))
            readMessage(mId);
    }

    private void handleSetFlags(Object[] data) {
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

    private void updateMessage(VKMessage msg) {
        int position = searchPosition(msg.getId());
        if (position == -1) return;

        remove(position);
        add(position, msg);
        notifyItemChanged(position, -1);
    }

    private void editMessage(VKMessage edited) {
        int position = searchPosition(edited.getId());
        if (position == -1) return;

        remove(position);
        add(position, edited);
        notifyItemChanged(position, -1);
    }

    public void addMessage(VKMessage msg, boolean anim) {
        if (msg.getRandomId() != 0 && containsRandom(msg.getRandomId()))
            return;

        add(msg);
        if (anim) {
            notifyItemInserted(getItemCount() - 1);
            notifyItemRangeChanged(0, getItemCount(), -1);
        }
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            return new FooterViewHolder(createFooter());
        }

        View v = inflater.inflate(R.layout.item_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
                EventBus.getDefault().postSticky(new Object[]{Keys.KEY_UPDATE_USER, userId});
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
                EventBus.getDefault().postSticky(new Object[]{Keys.KEY_UPDATE_GROUP, groupId});
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

    private void inflateAttachments(VKMessage item, ViewGroup parent, BoundedLinearLayout bubble) {
        final Drawable background = bubble.getBackground();
        ArrayList<VKModel> attachments = item.getAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            bubble.setBackground(background);
            bubble.setVisibility(View.VISIBLE);
            parent.setVisibility(View.VISIBLE);
            VKModel attachment = attachments.get(i);
            if (attachment instanceof VKAudio) {
                attacher.audio(item, parent, (VKAudio) attachment, true);
            } else if (attachment instanceof VKPhoto) {
                if (TextUtils.isEmpty(item.getText()))
                    bubble.setBackgroundColor(Color.TRANSPARENT);

                attacher.photo(item, parent, (VKPhoto) attachment, -1);
            } else if (attachment instanceof VKSticker) {
                bubble.setBackgroundColor(Color.TRANSPARENT);
                attacher.sticker(parent, (VKSticker) attachment);
            } else if (attachment instanceof VKDoc) {
                attacher.doc(item, parent, (VKDoc) attachment, false, true);
            } else if (attachment instanceof VKLink) {
                attacher.link(item, parent, (VKLink) attachment, true);
            } else if (attachment instanceof VKVideo) {
                if (TextUtils.isEmpty(item.getText()))
                    bubble.setBackgroundColor(Color.TRANSPARENT);

                attacher.video(parent, (VKVideo) attachment, -1);
            } else if (attachment instanceof VKGraffiti) {
                bubble.setBackgroundColor(Color.TRANSPARENT);
                attacher.graffiti(parent, (VKGraffiti) attachment);
            } else if (attachment instanceof VKVoice) {
                attacher.voice(item, parent, (VKVoice) attachment, false, true);
            } else if (attachment instanceof VKWall) {
                attacher.wall(item, parent, (VKWall) attachment, true);
            } else {
                attacher.empty(parent, context.getString(R.string.unknown));
            }
        }
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

    class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        ImageView indicator;
        ImageView important;

        TimeTextView text;

        LinearLayout mainContainer;

        BoundedLinearLayout bubble;
        LinearLayout attachments;
        LinearLayout serviceContainer;
        LinearLayout messageContainer;
        LinearLayout timeContainer;
        LinearLayout bubbleContainer;

        Drawable sending, error, placeholder;
        GradientDrawable circle;

        @ColorInt
        int alphaAccentColor;

        ViewHolder(View v) {
            super(v);

            alphaAccentColor = ColorUtil.alphaColor(ThemeManager.getAccent(), 0.3f);

            text = v.findViewById(R.id.text);

            placeholder = getDrawable(R.drawable.avatar_placeholder);

            avatar = v.findViewById(R.id.abc_tb_avatar);
            important = v.findViewById(R.id.important);

            circle = null;

            error = getDrawable(R.drawable.ic_error_black_24dp);
            sending = getDrawable(R.drawable.ic_access_time_black_24dp);

            sending.setTint(ThemeManager.getAccent());
            error.setTint(Color.RED);

            bubbleContainer = v.findViewById(R.id.bubble_container);
            messageContainer = v.findViewById(R.id.message_container);
            serviceContainer = v.findViewById(R.id.service_container);
            mainContainer = v.findViewById(R.id.root);
            bubble = v.findViewById(R.id.bubble);
            attachments = v.findViewById(R.id.attachments);
            timeContainer = v.findViewById(R.id.time_container);

            indicator = v.findViewById(R.id.message_state);
        }

        public boolean isFooter() {
            return false;
        }

        void bind(final int position) {
            final VKMessage item = getItem(position);
            final VKUser user = searchUser(item.getFromId());
            final VKGroup group = searchGroup(VKGroup.toGroupId(item.getFromId()));

            itemView.setBackgroundColor(item.isSelected() ? alphaAccentColor : 0);

            String time_ = item.getUpdateTime() > 0 ? getString(R.string.edited) + ", " : "" + Util.dateFormatter.format(item.isAdded() ? item.getDate() : item.getDate() * 1000L);
            text.setTimeText(time_);

            int gravity = item.isOut() ? Gravity.END : Gravity.START;

            timeContainer.setGravity(gravity);
            bubbleContainer.setGravity(gravity);

            important.setVisibility(item.isImportant() ? View.VISIBLE : View.GONE);
            indicator.setVisibility(!item.isOut() ? View.GONE : View.VISIBLE);

            if (indicator.getVisibility() == View.VISIBLE) {
                if (item.getStatus() == VKMessage.Status.SENT && !item.isRead()) {
                    indicator.setImageDrawable(circle);
                } else if (item.getStatus() == VKMessage.Status.ERROR) {
                    indicator.setImageDrawable(error);
                } else if (item.getStatus() == VKMessage.Status.SENDING) {
                    indicator.setImageDrawable(sending);
                } else {
                    indicator.setImageDrawable(null);
                }
            }

            String avatar_link = item.isFromGroup() ? group.getPhoto100() : user.getPhoto100();

            if (item.isFromUser()) {
                avatar_link = user.getPhoto100();
            } else if (item.isFromGroup()) {
                avatar_link = group.getPhoto100();
            }

            avatar.setVisibility(item.isOut() || !item.isChat() ? View.GONE : View.VISIBLE);

            if (avatar.getVisibility() == View.VISIBLE) {
                if (TextUtils.isEmpty(avatar_link)) {
                    avatar.setImageDrawable(placeholder);
                } else {
                    Picasso.get()
                            .load(avatar_link)
                            .priority(Picasso.Priority.HIGH)
                            .placeholder(placeholder)
                            .into(avatar);
                }
            } else {
                avatar.setImageDrawable(null);
            }

            avatar.setOnLongClickListener(p1 -> {
                if (item.isChat()) {
                    onAvatarLongClick(position);
                    return true;
                } else {
                    return false;
                }
            });

            messageContainer.setGravity(item.isOut() ? Gravity.END : Gravity.START);

            if (TextUtils.isEmpty(item.getText())) {
                text.setText("");
                text.setVisibility(View.GONE);
            } else {
                text.setVisibility(View.VISIBLE);
                String text = item.getText().trim();
                this.text.setText(text);
            }

            int textColor, bgColor, linkColor;

            linkColor = ThemeManager.getAccent();
            if (ThemeManager.isDark()) {
                textColor = 0xffeeeeee;
                bgColor = 0xff404040;
            } else {
                textColor = 0xff1d1d1d;
                bgColor = Color.WHITE;
            }

            text.setTextColor(textColor);
            text.setLinkTextColor(linkColor);
            text.setTimeTextColor(textColor);

            bubble.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

            Drawable bg = context.getResources().getDrawable(R.drawable.msg_in_bg);

            if (item.getAction() != null) {
                messageContainer.setVisibility(View.GONE);
                serviceContainer.setVisibility(View.VISIBLE);

                serviceContainer.removeAllViews();
                attacher.service(item, serviceContainer);

                bg = new ColorDrawable(Color.TRANSPARENT);
            } else {
                messageContainer.setVisibility(View.VISIBLE);
                serviceContainer.setVisibility(View.GONE);
                bg.setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
            }

            bubble.setBackground(bg);

            if (!ArrayUtil.isEmpty(item.getFwdMessages()) || !ArrayUtil.isEmpty(item.getAttachments())) {
                attachments.setVisibility(View.VISIBLE);
                attachments.removeAllViews();
            } else {
                attachments.setVisibility(View.GONE);
            }

            if (!ArrayUtil.isEmpty(item.getAttachments())) {
                inflateAttachments(item, attachments, bubble);
            }

            if (!ArrayUtil.isEmpty(item.getFwdMessages())) {
                attacher.showForwardedMessages(item, attachments, false, true);
            } else if (item.getReply() != null) {
                attacher.showForwardedMessages(item, attachments, true, true);
            }
        }
    }

}
