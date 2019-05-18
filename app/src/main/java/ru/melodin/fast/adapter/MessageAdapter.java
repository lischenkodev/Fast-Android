package ru.melodin.fast.adapter;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.VKUtils;
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
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.BoundedLinearLayout;
import ru.melodin.fast.view.CircleImageView;

public class MessageAdapter extends RecyclerAdapter<VKMessage, MessageAdapter.ViewHolder> {

    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_FOOTER = 2;

    private int peerId;
    private AttachmentInflater attacher;
    private DisplayMetrics metrics;

    private LinearLayoutManager manager;

    public MessageAdapter(Context context, ArrayList<VKMessage> messages, int peerId) {
        super(context, messages);

        this.peerId = peerId;
        this.attacher = new AttachmentInflater();
        this.metrics = context.getResources().getDisplayMetrics();

        manager = (LinearLayoutManager) ((MessagesActivity) context).getRecyclerView().getLayoutManager();
    }


    public void readNewMessage(final VKMessage message) {
        ThreadExecutor.execute(new AsyncCallback(((MessagesActivity) context)) {
            @Override
            public void ready() throws Exception {
                VKApi.messages().markAsRead().peerId(message.peerId).execute(Integer.class);
            }

            @Override
            public void done() {
                readMessage(message.id);
            }

            @Override
            public void error(Exception e) {

            }
        });
    }

    public boolean contains(int id) {
        for (VKMessage m : getValues()) {
            if (m.id == id) return true;
        }

        return false;
    }

    public int findPosition(int id) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).id == id) return i;
        }

        return -1;
    }

    public void editMessage(VKMessage edited) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.id == edited.id) {
                message.flags = edited.flags;
                message.text = edited.text;
                message.update_time = edited.update_time;
                message.attachments = edited.attachments;

                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    public void addMessage(VKMessage msg) {
        Log.d("Message info", "Current peerId: " + peerId + "; message peerId: " + msg.peerId + "; containsRandom(randomId = " + msg.randomId + "): " + containsRandom(msg.randomId));
        if (msg.peerId != peerId) return;
        if (containsRandom(msg.randomId)) return;

        add(msg);

        notifyItemInserted(getItemCount() - 1);
        ((MessagesActivity) context).checkCount();

        manager.scrollToPosition(getItemCount() - 1);

        //int lastVisibleItem = manager.findLastCompletelyVisibleItemPosition();
        //int totalVisibleItems = manager.findLastCompletelyVisibleItemPosition() - manager.findFirstCompletelyVisibleItemPosition() + 1;

        //if (lastVisibleItem <= totalVisibleItems) {

        //}
    }

    private boolean containsRandom(long randomId) {
        boolean contains = false;

        for (VKMessage m : getValues()) {
            if (m.randomId == randomId) contains = true;
        }

        return contains;
    }

    public void readMessage(int mId) {
        for (int i = 0; i < getItemCount(); i++) {
            VKMessage message = getItem(i);
            if (message.id == mId) {
                message.read = true;
                notifyItemChanged(i, -1);
                break;
            }
        }
    }

    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_NORMAL:
                View v = inflater.inflate(R.layout.activity_messages_list, parent, false);
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

    private VKUser searchUser(int id) {
        VKUser user = CacheStorage.getUser(id);

        if (user == null) {
            user = VKUser.EMPTY;
        }

        return user;
    }

    private VKGroup searchGroup(int id) {
        VKGroup group = CacheStorage.getGroup(id);

        if (group == null) {
            group = VKGroup.EMPTY;
        }

        return group;
    }

    private void applyActionStyle(VKMessage item, ViewGroup parent) {

    }

    private void showForwardedMessages(VKMessage item, ViewGroup parent) {
        for (int i = 0; i < item.fwd_messages.size(); i++) {
            attacher.message(item, parent, item.fwd_messages.get(i));
        }
    }

    private void showAttachments(VKMessage item, ViewHolder holder) {
        boolean onlyPhotos = true;
        if (TextUtils.isEmpty(item.text)) {
            for (VKModel attach : item.attachments) {
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
                item.attachments, holder.bubble, holder.bubble.getMaxWidth());
    }

    private void inflateAttachments(VKMessage item, ViewGroup parent, ViewGroup images, ArrayList<VKModel> attachments, BoundedLinearLayout bubble, int maxWidth) {
        for (int i = 0; i < attachments.size(); i++) {
            VKModel attach = attachments.get(i);
            if (attach instanceof VKAudio) {
                attacher.audio(item, parent, (VKAudio) attach);
            } else if (attach instanceof VKPhoto) {
                attacher.photo(item, images, (VKPhoto) attach);
            } else if (attach instanceof VKSticker) {
                if (bubble != null) {
                    bubble.setBackgroundColor(Color.TRANSPARENT);
                }
                attacher.sticker(parent, (VKSticker) attach);
            } else if (attach instanceof VKDoc) {
                attacher.doc(item, parent, (VKDoc) attach);
            } else if (attach instanceof VKLink) {
                attacher.link(item, parent, (VKLink) attach);
            } else if (attach instanceof VKVideo) {
                attacher.video(parent, (VKVideo) attach, maxWidth);
            } else {
                attacher.empty(parent, getString(R.string.unknown));
            }
        }
    }

    private void onAvatarLongClick(int position) {
        VKUser user = CacheStorage.getUser(getValues().get(position).fromId);
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

        LinearLayout main_container;

        BoundedLinearLayout bubble;
        LinearLayout attachments;
        LinearLayout photos;
        LinearLayout service_container;
        LinearLayout messageContainer;
        LinearLayout time_container;

        Space space;

        Drawable circle, sending, error, placeholder;

        ViewHolder(View v) {
            super(v);

            space = v.findViewById(R.id.space);
            text = v.findViewById(R.id.text);
            time = v.findViewById(R.id.time);

            placeholder = getDrawable(R.drawable.placeholder_user);

            avatar = v.findViewById(R.id.avatar);
            read = v.findViewById(R.id.read_indicator);
            important = v.findViewById(R.id.important);

            circle = new ColorDrawable(ThemeManager.getAccent());
            sending = getDrawable(R.drawable.ic_vector_access_time);
            error = getDrawable(R.drawable.ic_msg_error);

            messageContainer = v.findViewById(R.id.messageContainer);
            service_container = v.findViewById(R.id.service_container);
            main_container = v.findViewById(R.id.root);
            bubble = v.findViewById(R.id.bubble);
            attachments = v.findViewById(R.id.attachments);
            photos = v.findViewById(R.id.photos);
            time_container = v.findViewById(R.id.time_container);
        }

        public boolean isFooter() {
            return false;
        }

        void bind(final int position) {
            final VKMessage item = getItem(position);
            final VKUser user = searchUser(item.fromId);
            final VKGroup group = searchGroup(VKGroup.toGroupId(item.fromId));

            int editColor;

            if (item.isSelected()) {
                editColor = ColorUtil.alphaColor(ThemeManager.getAccent(), 0.6f);
            } else {
                editColor = 0;
            }

            if (item.status == VKMessage.STATUS_SENDING) {
                read.setImageDrawable(sending);
            } else if (item.status == VKMessage.STATUS_SENT) {
                read.setImageDrawable(circle);
            } else {
                read.setImageDrawable(error);
            }

            read.setVisibility(item.out ? item.read ? View.GONE : View.GONE : View.GONE);

            main_container.setBackgroundColor(editColor);

            String s = item.update_time > 0 ? getString(R.string.edited) + ", " : "";
            String time_ = s + Util.dateFormatter.format(item.isAdded ? item.date : item.date * 1000L);

            time.setText(time_);
            time_container.setGravity(item.out ? Gravity.END : Gravity.START);

            important.setVisibility(item.important ? View.VISIBLE : View.GONE);

            bubble.setVisibility(View.VISIBLE);

            String avatar_link = null;

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

            time.setGravity(item.out ? Gravity.END : Gravity.START);
            messageContainer.setGravity(item.out ? Gravity.END : Gravity.START);

            if (TextUtils.isEmpty(item.text)) {
                text.setText("");
                text.setVisibility(View.GONE);
            } else {
                text.setVisibility(View.VISIBLE);
                text.setText(item.text.trim());
            }

            int textColor, timeColor, bgColor, linkColor;

            if (item.out) {
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


            if (item.action != null) {
                avatar.setVisibility(View.GONE);
                messageContainer.setVisibility(View.GONE);
                time.setVisibility(View.GONE);
                service_container.setVisibility(View.VISIBLE);

                service_container.removeAllViews();
                attacher.service(item, service_container);

                bg.setTint(Color.TRANSPARENT);
            } else {
                avatar.setVisibility(View.VISIBLE);
                time.setVisibility(View.VISIBLE);
                messageContainer.setVisibility(View.VISIBLE);
                service_container.setVisibility(View.GONE);
                bg.setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);
            }

            bubble.setBackground(bg);

            if (!ArrayUtil.isEmpty(item.fwd_messages) || !ArrayUtil.isEmpty(item.attachments)) {
                attachments.setVisibility(View.VISIBLE);
                attachments.removeAllViews();

                photos.setVisibility(View.VISIBLE);
                photos.removeAllViews();
            } else {
                attachments.setVisibility(View.GONE);
                photos.setVisibility(View.GONE);
            }

            if (!ArrayUtil.isEmpty(item.attachments)) {
                showAttachments(item, this);
            }

            if (!ArrayUtil.isEmpty(item.fwd_messages)) {
                showForwardedMessages(item, attachments);
            }

            avatar.setVisibility(item.out ? View.GONE : View.VISIBLE);
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

        void sticker(ViewGroup parent, VKSticker source) {
            final ImageView image = (ImageView)
                    inflater.inflate(R.layout.msg_attach_photo, parent, false);

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
            text.setText(Html.fromHtml(VKUtils.getActionBody(item, false)));

            text.setClickable(false);
            text.setFocusable(false);
            parent.addView(text);
        }

        void video(ViewGroup parent, final VKVideo source, int width) {
            View v = inflater.inflate(R.layout.msg_attach_video, parent, false);

            ImageView image = v.findViewById(R.id.videoImage);
            TextView title = v.findViewById(R.id.videoTitle);
            TextView time = v.findViewById(R.id.videoTime);

            String duration = Util.dateFormatter.format(
                    TimeUnit.SECONDS.toMillis(source.duration));

            title.setText(source.title);
            time.setText(duration);
            image.setLayoutParams(getFrameParams(width));

            loadImage(image, source.photo_130, source.photo_320);
            parent.addView(v);
        }

        public void photo(final VKMessage item, ViewGroup parent, final VKPhoto source) {
            ImageView image = (ImageView) inflater.inflate(R.layout.msg_attach_photo, parent, false);

            image.setLayoutParams(getParams());
            image.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, PhotoViewActivity.class);

                    ArrayList<VKPhoto> urls = new ArrayList<>();

                    for (VKModel m : item.attachments) {
                        if (m instanceof VKPhoto) {
                            urls.add((VKPhoto) m);
                        }
                    }

                    intent.putExtra("photo", urls);
                    context.startActivity(intent);
                }
            });

            if (source.sizes != null)
                loadImage(image, source.sizes.forType("m").src, source.getMaxSize());
            parent.addView(image);
        }

        public void message(VKMessage item, ViewGroup parent, VKMessage source) {
            View v = inflater.inflate(R.layout.msg_attach_message, parent, false);
            v.setClickable(false);
            v.setFocusable(false);

            TextView userName = v.findViewById(R.id.userName);
            TextView userMessage = v.findViewById(R.id.userMessage);
            View line = v.findViewById(R.id.messageLine);

            VKUser user = MemoryCache.getUser(source.fromId);
            if (user == null) {
                user = VKUser.EMPTY;
            }

            if (item.out) {
                line.setBackgroundColor(Color.WHITE);
            } else {
                line.setBackgroundColor(ThemeManager.getAccent());
            }

            if (item.out) {
                userName.setTextColor(Color.WHITE);
                userMessage.setTextColor(0xffdddddd);
            } else {
                if (ThemeManager.isDark()) {
                    userName.setTextColor(Color.WHITE);
                    userMessage.setTextColor(0xffdddddd);
                } else {
                    userName.setTextColor(0xff212121);
                    userName.setTextColor(0xff404040);
                }

            }

            userName.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));
            userMessage.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

            userName.setText(user.toString());

            if (TextUtils.isEmpty(source.text)) {
                userMessage.setVisibility(View.GONE);
            } else {
                userMessage.setText(source.text);
            }

            if (!ArrayUtil.isEmpty(source.attachments)) {
                LinearLayout container = v.findViewById(R.id.msgAttachments);
                inflateAttachments(source, container, container, source.attachments, null, -1);
            }

            if (!ArrayUtil.isEmpty(source.fwd_messages)) {
                LinearLayout container = v.findViewById(R.id.msgAttachments);
                showForwardedMessages(source, container);
            }

            parent.addView(v);
        }

        void doc(VKMessage item, ViewGroup parent, final VKDoc source) {
            if (source.isVoice) {
                Toast.makeText(context, "is voice!", Toast.LENGTH_SHORT).show();
                voice(item, parent, source.voice);
                return;
            }

            if (source.isGrafftiti) {
                Toast.makeText(context, "is graffiti!", Toast.LENGTH_SHORT).show();
                graffiti(parent, source.graffiti);
                return;
            }

            View v = inflater.inflate(R.layout.msg_attach_doc, parent, false);

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

            if (item.out) {
                bgColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = 0xffdddddd;
                iconColor = 0xff212121;
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;

                if (ThemeManager.isDark()) {
                    titleColor = Color.WHITE;
                    bodyColor = 0xffdddddd;
                } else {
                    titleColor = 0xff212121;
                    bodyColor = 0xff404040;
                }
            }

            Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_file);
            arrow.setTint(iconColor);

            icon.setImageDrawable(arrow);
            background.setImageDrawable(new ColorDrawable(bgColor));

            title.setTextColor(titleColor);
            size.setTextColor(bodyColor);

            boolean hasPhoto = source.isPhoto;

            if (hasPhoto) {
                String src = source.photo_sizes.forType("s").src;

                if (!TextUtils.isEmpty(src))
                    Picasso.get()
                            .load(src)
                            .placeholder(new ColorDrawable(Color.TRANSPARENT))
                            .into(background);
            }

            parent.addView(v);
            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    if (source.isVoice || source.isGrafftiti) return;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        }


        void graffiti(ViewGroup parent, VKGraffiti source) {
            final ImageView image = (ImageView)
                    inflater.inflate(R.layout.msg_attach_photo, parent, false);

            image.setLayoutParams(getParams());
            loadImage(image, source.src, source.src);

            image.setClickable(false);
            image.setFocusable(false);

            parent.addView(image);
        }

        void voice(VKMessage item, ViewGroup parent, VKVoice source) {
            View v = inflater.inflate(R.layout.msg_attach_audio, parent, false);

            TextView title = v.findViewById(R.id.audioTitle);
            TextView body = v.findViewById(R.id.audioBody);
            TextView time = v.findViewById(R.id.audioDuration);

            ImageButton play = v.findViewById(R.id.btnAudioPlay);

            String duration = String.format(AppGlobal.locale, "%d:%02d",
                    source.duration / 60,
                    source.duration % 60);
            title.setText(getString(R.string.voice_message));
            body.setText("");
            time.setText(duration);

            int titleColor, bodyColor, iconColor, bgColor;

            if (item.out) {
                bgColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = 0xffdddddd;
                iconColor = 0xff212121;
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;

                if (ThemeManager.isDark()) {
                    titleColor = Color.WHITE;
                    bodyColor = 0xffdddddd;
                } else {
                    titleColor = 0xff212121;
                    bodyColor = 0xff404040;
                }
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
            View v = inflater.inflate(R.layout.msg_attach_audio, parent, false);

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

            if (item.out) {
                bgColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = 0xffdddddd;
                iconColor = 0xff212121;
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;

                if (ThemeManager.isDark()) {
                    titleColor = Color.WHITE;
                    bodyColor = 0xffdddddd;
                } else {
                    titleColor = 0xff212121;
                    bodyColor = 0xff404040;
                }
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

        public void link(VKMessage item, ViewGroup parent, final VKLink source) {
            View v = inflater.inflate(R.layout.msg_attach_doc, parent, false);

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

            if (item.out) {
                bgColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = 0xffdddddd;
                iconColor = 0xff212121;
            } else {
                bgColor = ThemeManager.getAccent();
                iconColor = Color.WHITE;

                if (ThemeManager.isDark()) {
                    titleColor = Color.WHITE;
                    bodyColor = 0xffdddddd;
                } else {
                    titleColor = 0xff212121;
                    bodyColor = 0xff404040;
                }
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
