package ru.melodin.fast.adapter;

import android.animation.Animator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.VKUtil;
import ru.melodin.fast.api.model.VKAudio;
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

    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_FOOTER = 2;

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
        this.attacher = new AttachmentInflater();
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
        return findPosition(id) != -1;
    }

    public int findPosition(int id) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == id) return i;
        }

        return -1;
    }

    private void importantMessage(boolean important, int mId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.getId() == mId) {
                message.setImportant(important);
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    public void editMessage(VKMessage edited) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.getId() == edited.getId()) {
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    public void addMessage(VKMessage msg) {
        if (msg.getPeerId() != peerId || (msg.getRandomId() != 0 && containsRandom(msg.getRandomId())))
            return;

        add(msg);

        notifyItemInserted(getItemCount() - 1);
        ((MessagesActivity) context).checkCount();
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
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_NORMAL:
                View v = inflater.inflate(R.layout.activity_messages_item, parent, false);
                return new ViewHolder(v);
            case TYPE_FOOTER:
                return new FooterViewHolder(createView());
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        if (holder.isFooter()) return;
        super.onBindViewHolder(holder, position);
        holder.bind(position);
    }

    @Override
    public int getItemViewType(int position) {
        return position == getItemCount() - 1 ? TYPE_FOOTER : TYPE_NORMAL;
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

    private void loadUser(final Integer userId) {
        if (loadingIds.contains(userId) || userId == 0) return;
        loadingIds.add(userId);
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

    private void showForwardedMessages(VKMessage item, ViewGroup parent, boolean reply) {
        if (reply)
            attacher.message(item, parent, item.getReply().asMessage(), reply);
        else
            for (int i = 0; i < item.getFwdMessages().size(); i++) {
                attacher.message(item, parent, item.getFwdMessages().get(i), false);
            }
    }

    private void showAttachments(VKMessage item, ViewHolder holder) {
        boolean onlyPhotos = true;
        if (TextUtils.isEmpty(item.getText())) {
            for (VKModel attach : item.getAttachments()) {
                boolean isPhoto = attach instanceof VKPhoto;
                if (!isPhoto) {
                    onlyPhotos = false;
                    break;
                }
            }
            if (onlyPhotos) {
                holder.bubble.setVisibility(View.GONE);
            }
        }

        inflateAttachments(item, holder.attachments, holder.photos,
                item.getAttachments(), holder.bubble, holder.bubble.getMaxWidth(), false);
    }

    private void inflateAttachments(VKMessage item, ViewGroup parent, ViewGroup images, ArrayList<VKModel> attachments, BoundedLinearLayout bubble, int maxWidth, boolean forwarded) {
        for (int i = 0; i < attachments.size(); i++) {
            parent.setVisibility(View.VISIBLE);
            VKModel attachment = attachments.get(i);
            if (attachment instanceof VKAudio) {
                attacher.audio(item, parent, (VKAudio) attachment);
            } else if (attachment instanceof VKPhoto) {
                parent.setVisibility(View.GONE);
                attacher.photo(item, images, (VKPhoto) attachment);
            } else if (attachment instanceof VKSticker) {
                if (bubble != null) {
                    bubble.setBackgroundColor(Color.TRANSPARENT);
                }
                attacher.sticker(parent, (VKSticker) attachment);
            } else if (attachment instanceof VKDoc) {
                attacher.doc(item, parent, (VKDoc) attachment, forwarded);
            } else if (attachment instanceof VKLink) {
                attacher.link(item, parent, (VKLink) attachment);
            } else if (attachment instanceof VKVideo) {
                attacher.video(parent, (VKVideo) attachment, maxWidth);
            } else if (attachment instanceof VKGraffiti) {
                if (bubble != null) {
                    bubble.setBackgroundColor(Color.TRANSPARENT);
                }
                attacher.graffiti(parent, (VKGraffiti) attachment);
            } else if (attachment instanceof VKVoice) {
                attacher.voice(item, parent, (VKVoice) attachment, forwarded);
            } else if (attachment instanceof VKWall) {
                attacher.wall(item, parent, (VKWall) attachment);
            } else {
                attacher.empty(parent, getString(R.string.unknown));
            }
        }
    }

    private void onAvatarLongClick(int position) {
        VKUser user = CacheStorage.getUser(getValues().get(position).getFromId());
        if (user == null) return;

        Toast.makeText(context, user.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @Override
    public VKMessage getItem(int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            return super.getItem(position - 1);
        }
        return super.getItem(position);
    }

    public int getMessagesCount() {
        return getValues().size();
    }

    private View createView() {
        View v = new View(context);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setVisibility(View.INVISIBLE);
        v.setEnabled(false);
        v.setClickable(false);
        v.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) Util.px(66)));

        return v;
    }

    class FooterViewHolder extends ViewHolder {

        FooterViewHolder(View v) {
            super(v);
        }

        public boolean isFooter() {
            return true;
        }

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


        ViewHolder(View v) {
            super(v);

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

        public boolean isFooter() {
            return false;
        }

        void bind(final int position) {
            final VKMessage item = getItem(position);
            final VKUser user = searchUser(item.getFromId());
            final VKGroup group = searchGroup(VKGroup.toGroupId(item.getFromId()));

            if (item.isSelected()) {
                mainContainer.setBackgroundColor(ColorUtil.alphaColor(ThemeManager.getAccent()));
            } else {
                mainContainer.setBackgroundColor(ThemeManager.getBackground());
            }

            int editColor;

            if (item.isSelected()) {
                editColor = ColorUtil.alphaColor(ThemeManager.getAccent(), 0.6f);
            } else {
                editColor = 0;
            }

            read.setVisibility(item.isOut() ? item.isRead() ? View.GONE : View.GONE : View.GONE);

            mainContainer.setBackgroundColor(editColor);

            String s = item.getUpdateTime() > 0 ? getString(R.string.edited) + ", " : "";
            String time_ = s + Util.dateFormatter.format(item.isAdded() ? item.getDate() : item.getDate() * 1000L);

            time.setText(time_);

            int gravity = item.isOut() ? Gravity.END : Gravity.START;

            timeContainer.setGravity(gravity);
            bubbleContainer.setGravity(gravity);

            ViewUtil.fadeView(important, item.isImportant(), new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (item.isImportant())
                        important.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!item.isImportant())
                        important.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

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
                showAttachments(item, this);
            }

            if (!ArrayUtil.isEmpty(item.getFwdMessages())) {
                showForwardedMessages(item, attachments, false);
            } else if (item.getReply() != null) {
                showForwardedMessages(item, attachments, true);
            }

            avatar.setVisibility(item.isOut() ? View.GONE : View.VISIBLE);
            space.setVisibility(avatar.getVisibility());
        }
    }

    private class AttachmentInflater {
        private void loadImage(final ImageView image, String smallSrc, final String normalSrc) {
            if (TextUtils.isEmpty(smallSrc)) return;
            try {
                Picasso.get()
                        .load(smallSrc)
                        .config(Bitmap.Config.RGB_565)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(new ColorDrawable(Color.TRANSPARENT))
                        .into(image, new Callback.EmptyCallback() {
                            @Override
                            public void onSuccess() {
                                if (TextUtils.isEmpty(normalSrc)) return;
                                Picasso.get()
                                        .load(normalSrc)
                                        .priority(Picasso.Priority.NORMAL)
                                        .placeholder(image.getDrawable())
                                        .into(image);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int getHeight(int layoutMaxWidth) {
            float scale = Math.max((float) 320.0, layoutMaxWidth) /
                    Math.min((float) 320.0, layoutMaxWidth);
            return Math.round((float) 320.0 < layoutMaxWidth ? (float) 240.0 * scale : (float) 240.0 / scale);
        }

        private LinearLayout.LayoutParams getParams() {
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private FrameLayout.LayoutParams getFrameParams(int layoutWidth) {
            if (layoutWidth == -1) {
                return new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    getHeight(layoutWidth)
            );
        }

        public void empty(ViewGroup parent, String s) {
            final TextView body = new TextView(context);

            String string = "[" + s + "]";
            body.setText(string);

            body.setClickable(false);
            body.setFocusable(false);

            parent.addView(body);
        }

        void wall(VKMessage item, ViewGroup parent, final VKWall source) {
            View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

            TextView title = v.findViewById(R.id.docTitle);
            TextView body = v.findViewById(R.id.docBody);
            ImageView icon = v.findViewById(R.id.docIcon);
            ImageView background = v.findViewById(R.id.docCircleBackground);

            title.setText(getString(R.string.wall_post));
            body.setText(getString(R.string.link));

            title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
            body.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.isOut()) {
                bgColor = Color.WHITE;
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }

            Drawable arrow = context.getResources().getDrawable(R.drawable.ic_assignment_black_24dp);
            arrow.setTint(iconColor);

            icon.setImageDrawable(arrow);
            background.setImageDrawable(new ColorDrawable(bgColor));

            title.setTextColor(titleColor);
            body.setTextColor(bodyColor);

            parent.addView(v);
        }

        void graffiti(ViewGroup parent, VKGraffiti source) {
            final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

            image.setLayoutParams(getParams());
            loadImage(image, source.url, "");

            image.setClickable(false);
            image.setFocusable(false);

            parent.addView(image);
        }

        void sticker(ViewGroup parent, VKSticker source) {
            final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

            image.setLayoutParams(getParams());
            loadImage(image, source.src_256, "");

            image.setClickable(false);
            image.setFocusable(false);

            parent.addView(image);
        }

        void service(VKMessage item, ViewGroup parent) {
            TextView text = new TextView(context);
            text.setTextColor(ThemeManager.getAccent());

            text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            text.setGravity(Gravity.CENTER);
            text.setText(Html.fromHtml(VKUtil.getActionBody(item, false)));

            text.setClickable(false);
            text.setFocusable(false);
            parent.addView(text);
        }

        void video(ViewGroup parent, final VKVideo source, int width) {
            View v = inflater.inflate(R.layout.activity_messages_attach_video, parent, false);

            ImageView image = v.findViewById(R.id.videoImage);
            TextView title = v.findViewById(R.id.videoTitle);
            TextView time = v.findViewById(R.id.videoTime);

            String duration = Util.dateFormatter.format(TimeUnit.SECONDS.toMillis(source.duration));

            title.setText(source.title);
            time.setText(duration);
            image.setLayoutParams(getFrameParams(width));

            loadImage(image, source.photo_130, source.photo_320);
            parent.addView(v);
        }

        public void photo(final VKMessage item, ViewGroup parent, final VKPhoto source) {
            ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

            image.setLayoutParams(getParams());
            image.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, PhotoViewActivity.class);

                    ArrayList<VKPhoto> photos = new ArrayList<>();

                    for (VKModel m : item.getAttachments()) {
                        if (m instanceof VKPhoto) {
                            photos.add((VKPhoto) m);
                        }
                    }

                    intent.putExtra("selected", source);
                    intent.putExtra("photo", photos);
                    context.startActivity(intent);
                }
            });

            if (source.sizes != null)
                loadImage(image, source.sizes.forType("m").src, source.getMaxSize());
            parent.addView(image);
        }

        public void message(VKMessage item, ViewGroup parent, VKMessage source, boolean reply) {
            if (reply) {
                empty(parent, "reply");
                return;
            }

            View v = inflater.inflate(R.layout.activity_messages_attach_message, parent, false);
            v.setClickable(false);
            v.setFocusable(false);

            TextView name = v.findViewById(R.id.user_name);
            TextView message = v.findViewById(R.id.user_message);
            ImageView avatar = v.findViewById(R.id.user_avatar);
            View line = v.findViewById(R.id.message_line);

            VKUser user = MemoryCache.getUser(source.getFromId());
            if (user == null) {
                user = VKUser.EMPTY;
            }

            if (TextUtils.isEmpty(user.photo_100)) {
                avatar.setVisibility(View.GONE);
            } else {
                avatar.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(user.photo_100)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(new ColorDrawable(ColorUtil.darkenColor(ThemeManager.getPrimary())))
                        .into(avatar);
            }

            GradientDrawable lineBackground = new GradientDrawable();
            lineBackground.setCornerRadius(100);
            lineBackground.setColor(item.isOut() ? Color.WHITE : ThemeManager.getAccent());

            line.setBackground(lineBackground);

            int nameColor, messageColor;

            if (item.isOut()) {
                nameColor = Color.WHITE;
                messageColor = ColorUtil.darkenColor(nameColor);
            } else {
                nameColor = ThemeManager.isDark() ? Color.WHITE : Color.DKGRAY;
                messageColor = ThemeManager.isDark() ? ColorUtil.lightenColor(nameColor) : ColorUtil.darkenColor(nameColor);
            }

            name.setTextColor(nameColor);
            message.setTextColor(messageColor);

            name.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));
            message.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

            name.setText(user.toString());

            if (TextUtils.isEmpty(source.getText())) {
                message.setVisibility(View.GONE);
            } else {
                message.setText(source.getText());
            }

            if (!ArrayUtil.isEmpty(source.getAttachments())) {
                LinearLayout container = v.findViewById(R.id.attachments);
                inflateAttachments(source, container, container, source.getAttachments(), null, -1, true);
            }

            if (!ArrayUtil.isEmpty(source.getFwdMessages())) {
                LinearLayout container = v.findViewById(R.id.forwarded);
                showForwardedMessages(source, container, false);
            }

            parent.addView(v);
        }

        void doc(VKMessage item, ViewGroup parent, final VKDoc source, boolean forwarded) {
            View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

            TextView title = v.findViewById(R.id.docTitle);
            TextView size = v.findViewById(R.id.docBody);
            ImageView background = v.findViewById(R.id.docCircleBackground);
            ImageView icon = v.findViewById(R.id.docIcon);

            title.setText(source.title);

            String size_ = Util.parseSize(source.size) + " • " + source.ext.toUpperCase();
            size.setText(size_);

            title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
            size.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.isOut() || forwarded) {
                bgColor = Color.WHITE;
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }

            Drawable drawable = context.getResources().getDrawable(R.drawable.ic_vector_file);
            drawable.setTint(iconColor);

            icon.setImageDrawable(drawable);
            background.setImageDrawable(new ColorDrawable(bgColor));

            title.setTextColor(titleColor);
            size.setTextColor(bodyColor);

            if (source.photo_sizes != null) {
                String src = source.photo_sizes.forType("s").src;

                if (!TextUtils.isEmpty(src))
                    Picasso.get()
                            .load(src)
                            .placeholder(new ColorDrawable(ColorUtil.darkenColor(ThemeManager.getPrimary())))
                            .into(background);
            }

            parent.addView(v);
            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        }

        void voice(VKMessage item, ViewGroup parent, VKVoice source, boolean forwarded) {
            View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

            TextView title = v.findViewById(R.id.audioTitle);
            TextView body = v.findViewById(R.id.audioBody);
            TextView time = v.findViewById(R.id.audioDuration);

            ImageButton play = v.findViewById(R.id.btnAudioPlay);

            String duration = String.format(AppGlobal.locale, "%d:%02d",
                    source.duration / 60,
                    source.duration % 60);
            title.setText(getString(R.string.voice_message));
            body.setText(duration);

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.isOut() || forwarded) {
                bgColor = Color.WHITE;
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }

            Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_play_arrow);
            arrow.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(100);
            bg.setColor(bgColor);

            play.setBackground(bg);
            play.setImageDrawable(arrow);

            time.setTextColor(bodyColor);
            title.setTextColor(titleColor);
            body.setTextColor(bodyColor);

            title.setMaxWidth(metrics.widthPixels / 2);
            body.setMaxWidth(metrics.widthPixels / 2);
            time.setMaxWidth(metrics.widthPixels / 2);

            parent.addView(v);
        }

        public void audio(VKMessage item, ViewGroup parent, VKAudio source) {
            View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

            TextView title = v.findViewById(R.id.audioTitle);
            TextView body = v.findViewById(R.id.audioBody);
            TextView time = v.findViewById(R.id.audioDuration);

            ImageButton play = v.findViewById(R.id.btnAudioPlay);

            String duration = String.format(AppGlobal.locale, "%d:%02d",
                    source.duration / 60,
                    source.duration % 60);
            title.setText(source.title);
            body.setText(source.artist);
            time.setText(duration);

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.isOut()) {
                bgColor = Color.WHITE;
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }

            Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_play_arrow);
            arrow.setTint(iconColor);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(100);
            bg.setColor(bgColor);

            play.setBackground(bg);
            play.setImageDrawable(arrow);

            time.setTextColor(bodyColor);
            title.setTextColor(titleColor);
            body.setTextColor(bodyColor);

            title.setMaxWidth(metrics.widthPixels / 2);
            body.setMaxWidth(metrics.widthPixels / 2);
            time.setMaxWidth(metrics.widthPixels / 2);

            parent.addView(v);
        }

        public void link(VKMessage item, ViewGroup parent, final VKLink source) {
            View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

            TextView title = v.findViewById(R.id.docTitle);
            TextView body = v.findViewById(R.id.docBody);
            ImageView icon = v.findViewById(R.id.docIcon);
            ImageView background = v.findViewById(R.id.docCircleBackground);


            title.setText(source.title);
            body.setText(TextUtils.isEmpty(source.description)
                    ? source.caption
                    : source.description);

            title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
            body.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.isOut()) {
                bgColor = Color.WHITE;
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }

            Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_link_arrow);
            arrow.setTint(iconColor);

            icon.setImageDrawable(arrow);
            background.setImageDrawable(new ColorDrawable(bgColor));

            title.setTextColor(titleColor);
            body.setTextColor(bodyColor);

            parent.addView(v);

            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage("com.android.chrome");
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        intent.setPackage(null);
                        context.startActivity(intent);
                    }
                }
            });
        }
    }
}
