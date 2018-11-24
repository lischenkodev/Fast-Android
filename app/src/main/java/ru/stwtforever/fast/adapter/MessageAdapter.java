package ru.stwtforever.fast.adapter;

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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import ru.stwtforever.fast.MessagesActivity;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.VKUtils;
import ru.stwtforever.fast.api.model.VKAudio;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKDoc;
import ru.stwtforever.fast.api.model.VKGraffiti;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKLink;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKModel;
import ru.stwtforever.fast.api.model.VKPhoto;
import ru.stwtforever.fast.api.model.VKSticker;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.api.model.VKVideo;
import ru.stwtforever.fast.api.model.VKVoice;
import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.MemoryCache;
import ru.stwtforever.fast.helper.FontHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.ColorUtil;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.view.BoundedLinearLayout;
import ru.stwtforever.fast.view.CircleImageView;

public class MessageAdapter extends BaseRecyclerAdapter<VKMessage, MessageAdapter.ViewHolder> {

    private int peerId;

    private LayoutInflater inflater;

    private AttachmentInflater attacher;

    private DisplayMetrics metrics;

    private ArrayList<Integer> mids;

    public MessageAdapter(Context context, ArrayList<VKMessage> msgs, int peerId) {
        super(context, msgs);

        this.peerId = peerId;

        this.mids = new ArrayList<>();

        for (VKMessage m : getValues()) {
            mids.add(m.id);
        }

        this.inflater = LayoutInflater.from(context);

        this.metrics = context.getResources().getDisplayMetrics();
        this.attacher = new AttachmentInflater();

        EventBus.getDefault().register(this);
    }

    public void sendMessage(int id) {
        mids.add(id);
    }

    public void removeMessage(int id) {
        for (int i = 0; i < mids.size(); i++) {
            int mid = mids.get(i);
            if (mid == id) mids.remove(i);
        }

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView avatar;
        ImageView read;

        TextView text;
        TextView time;

        LinearLayout main_container;

        BoundedLinearLayout bubble;
        LinearLayout attachments;
        LinearLayout photos;
        LinearLayout service_container;
        LinearLayout messageContainer;

        MessageAdapter adapter;
        Context context;
        DisplayMetrics metrics;
        Space space;

        Drawable circle, sending, error, placeholder;

        public ViewHolder(Context context, MessageAdapter adapter, View v, DisplayMetrics metrics) {
            super(v);
            this.context = context;
            this.adapter = adapter;
            this.metrics = metrics;

            space = v.findViewById(R.id.space);
            text = v.findViewById(R.id.text);
            time = v.findViewById(R.id.time);

            placeholder = adapter.getDrawable(R.drawable.placeholder_user);

            avatar = v.findViewById(R.id.avatar);
            read = v.findViewById(R.id.read_indicator);

            GradientDrawable circ = new GradientDrawable();
            circ.setCornerRadius(100);
            circ.setColor(ThemeManager.getAccent());

            circle = circ;
            sending = adapter.getDrawable(R.drawable.ic_vector_access_time);
            error = adapter.getDrawable(R.drawable.ic_msg_error);

            messageContainer = v.findViewById(R.id.messageContainer);
            service_container = v.findViewById(R.id.service_container);
            main_container = v.findViewById(R.id.root);
            bubble = v.findViewById(R.id.bubble);
            attachments = v.findViewById(R.id.attachments);
            photos = v.findViewById(R.id.photos);
        }

        public void bind(final int position) {
            final VKMessage item = adapter.getItem(position);
            final VKUser user = adapter.searchUser(item.fromId);
            final VKGroup group = adapter.searchGroup(VKGroup.toGroupId(item.fromId));

            int editColor = 0;

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
            space.setVisibility(item.isChat() ? View.VISIBLE : View.GONE);

            main_container.setBackgroundColor(editColor);

            time.setText((item.update_time > 0 ? adapter.getString(R.string.edited).toLowerCase() + ", " : "") + Utils.dateFormatter.format(item.isAdded ? item.date : item.date * 1000L));

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
                    adapter.onAvatarLongClick(position);
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

            //text.setTextColor(textColor);
            //text.setLinkTextColor(linkColor);
            //time.setTextColor(timeColor);

            bubble.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

            Drawable bg = context.getResources().getDrawable(R.drawable.msg_in_bg);
            //bg.setColorFilter(bgColor, PorterDuff.Mode.MULTIPLY);

            bubble.setBackground(bg);

            if (!TextUtils.isEmpty(item.actionType)) {
                //if (avatar.getVisibility() != View.GONE)
                avatar.setVisibility(View.GONE);

                //if (messageContainer.getVisibility() != View.GONE)
                messageContainer.setVisibility(View.GONE);

                //if (time.getVisibility() != View.GONE)
                time.setVisibility(View.GONE);

                //if (service_container.getVisibility() != View.VISIBLE)
                service_container.setVisibility(View.VISIBLE);

                adapter.applyActionStyle(item, service_container);
            } else {
                //if (avatar.getVisibility() != View.VISIBLE)
                avatar.setVisibility(View.VISIBLE);

                //if (time.getVisibility() != View.VISIBLE)
                time.setVisibility(View.VISIBLE);

                //if (messageContainer.getVisibility() != View.VISIBLE)
                messageContainer.setVisibility(View.VISIBLE);

                //if (service_container.getVisibility() != View.GONE)
                service_container.setVisibility(View.GONE);
            }

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
                adapter.showAttachments(item, this);
            }

            if (!ArrayUtil.isEmpty(item.fwd_messages)) {
                adapter.showForwardedMessages(item, attachments);
            }

            if (!item.out && item.isChat()) {
                avatar.setVisibility(View.VISIBLE);
            } else {
                avatar.setVisibility(View.GONE);
            }

            adapter.setOnClick(itemView, position, item);
        }
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
                addMessage(((VKConversation) data[1]).last);
                break;
            case 5:
                editMessage((VKMessage) data[1]);
                break;
        }
    }

    private void editMessage(VKMessage edited) {
        int position = searchPosition(edited);
        if (position == -1) return;

        VKMessage msg = getValues().get(position);
        if (msg == null) return;

        msg.mask = edited.mask;
        msg.text = edited.text;
        msg.update_time = edited.update_time;
        msg.attachments = edited.attachments;

        notifyItemChanged(position);
    }

    private void addMessage(VKMessage msg) {
        if (msg.out) return;
        if (msg.peerId != peerId) return;

        mids.add(msg.id);

        MessagesActivity root = (MessagesActivity) context;
        LinearLayoutManager m = (LinearLayoutManager) root.getRecycler().getLayoutManager();
        if (m.findLastCompletelyVisibleItemPosition() == getItemCount())
            add(msg, true);
        else {
            add(msg, false);
        }


        root.checkMessagesCount();
    }

    private void readMessage(int id) {
        VKMessage m = searchMessage(id);
        if (m == null) return;

        int position = searchPosition(m);

        m.read = true;

        notifyItemChanged(position);
    }

    public void showHover(int position, boolean b) {
        if (!isHover(position)) {
            getValues().get(position).setSelected(b);
            notifyItemChanged(position);
        }
    }

    public boolean isHover(int position) {
        return getValues().get(position).isSelected();
    }

    public VKMessage searchMessage(Integer id) {
        for (VKMessage m : getValues()) {
            if (m.id == id)
                return m;
        }

        return null;
    }

    public int searchPosition(VKMessage m) {
        int index = -1;

        for (int i = 0; i < getValues().size(); i++) {
            VKMessage msg = getValues().get(i);
            if (msg.id == m.id) {
                index = i;
            }
        }

        return index;
    }

    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.activity_messages_list, parent, false);
        return new ViewHolder(context, MessageAdapter.this, v, metrics);
    }

    @Override
    public void onBindViewHolder(MessageAdapter.ViewHolder holder, final int position) {
        holder.bind(position);
    }

    private void setOnClick(View v, final int i, final VKMessage item) {
        if (v == null || !(TextUtils.isEmpty(item.actionType))) return;

        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MessagesActivity) context).onItemClick(v, i, item);
            }
        });
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
        attacher.service(item, parent);
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
                attacher.photo(item, images, (VKPhoto) attach, maxWidth);
            } else if (attach instanceof VKSticker) {
                if (bubble != null) {
                    bubble.setBackgroundColor(Color.TRANSPARENT);
                }
                attacher.sticker(item, parent, (VKSticker) attach, maxWidth);
            } else if (attach instanceof VKDoc) {
                attacher.doc(item, parent, (VKDoc) attach);
            } else if (attach instanceof VKLink) {
                attacher.link(item, parent, (VKLink) attach);
            } else if (attach instanceof VKVideo) {
                attacher.video(item, parent, (VKVideo) attach, maxWidth);
            } else {
                attacher.empty(item, parent, getString(R.string.unknown));
            }
        }
    }

    private class AttachmentInflater {
        private void loadImage(final ImageView image, String smallSrc, final String normalSrc, final boolean round) {
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
                                        .priority(Picasso.Priority.LOW)
                                        .placeholder(image.getDrawable())
                                        .into(image);
                            }
                        });
            } catch (Exception e) {
                AppGlobal.putException(e);
            }
        }

        private int getHeight(float width, float height, int layoutMaxWidth) {
            float scale = Math.max(width, layoutMaxWidth) /
                    Math.min(width, layoutMaxWidth);
            return Math.round(width < layoutMaxWidth ? height * scale : height / scale);
        }

        private LinearLayout.LayoutParams getParams(float sw, float sh, int layoutWidth) {
            if (layoutWidth == -1) {
                return new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            return new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    getHeight(sw, sh, layoutWidth)
            );
        }

        private FrameLayout.LayoutParams getFrameParams(float sw, float sh, int layoutWidth) {
            if (layoutWidth == -1) {
                return new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    getHeight(sw, sh, layoutWidth)
            );
        }

        public void empty(VKMessage item, ViewGroup parent, String s) {
            final TextView body = new TextView(context);
            body.setText("[" + s + "]");

            body.setClickable(false);
            body.setFocusable(false);

            parent.addView(body);
        }

        public void sticker(VKMessage item, ViewGroup parent, VKSticker source, int width) {
            final ImageView image = (ImageView)
                    inflater.inflate(R.layout.msg_attach_photo, parent, false);

            image.setLayoutParams(getParams(128f, 128f, width));
            loadImage(image, source.src_64, source.src_256, false);

            image.setClickable(false);
            image.setFocusable(false);

            parent.addView(image);
        }

        public void graffiti(VKMessage item, ViewGroup parent, VKGraffiti source, int width) {
            final ImageView image = (ImageView)
                    inflater.inflate(R.layout.msg_attach_photo, parent, false);

            image.setLayoutParams(getParams(128f, 128f, width));
            loadImage(image, source.src, source.src, false);

            image.setClickable(false);
            image.setFocusable(false);

            parent.addView(image);
        }

        public void service(VKMessage item, ViewGroup parent) {
            TextView text = new TextView(context);
            text.setTextColor(ThemeManager.getAccent());
            text.setTypeface(FontHelper.getFont(FontHelper.PS_BOLD));
            text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            text.setGravity(Gravity.CENTER);
            text.setText(Html.fromHtml(VKUtils.getActionBody(item)));

            text.setClickable(false);
            text.setFocusable(false);
            parent.addView(text);
        }

        public void video(VKMessage item, ViewGroup parent, final VKVideo source, int width) {
            View v = inflater.inflate(R.layout.msg_attach_video, parent, false);

            ImageView image = v.findViewById(R.id.videoImage);
            TextView title = v.findViewById(R.id.videoTitle);
            TextView time = v.findViewById(R.id.videoTime);

            String duration = Utils.dateFormatter.format(
                    TimeUnit.SECONDS.toMillis(source.duration));

            title.setText(source.title);
            time.setText(duration);
            image.setLayoutParams(getFrameParams(320f, 240f, width));

            loadImage(image, source.photo_130, source.photo_320, false);
            parent.addView(v);

            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.player));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });
        }

        public void photo(VKMessage item, ViewGroup parent, final VKPhoto source, int width) {
            final ImageView image = (ImageView)
                    inflater.inflate(R.layout.msg_attach_photo, parent, false);

            image.setLayoutParams(getParams(source.width, source.height, width));
            image.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View p1) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.sizes.forType("x").src));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });


            loadImage(image, source.sizes.forType("m").src, source.sizes.forType("m").src, true);
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
                inflateAttachments(item, container, container, source.attachments, null, -1);
            }

            if (!ArrayUtil.isEmpty(source.fwd_messages)) {
                LinearLayout container = v.findViewById(R.id.msgAttachments);
                showForwardedMessages(source, container);
            }

            parent.addView(v);
        }

        public void voice(VKMessage item, ViewGroup parent, VKVoice source) {
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

        public void doc(VKMessage item, ViewGroup parent, final VKDoc source) {
            if (source.voice != null) {
                voice(item, parent, source.voice);
                return;
            }

            if (source.graffiti != null) {
                graffiti(item, parent, source.graffiti, -1);
            }

            View v = inflater.inflate(R.layout.msg_attach_doc, parent, false);

            TextView title = v.findViewById(R.id.docTitle);
            TextView size = v.findViewById(R.id.docBody);
            ImageView background = v.findViewById(R.id.docCircleBackground);
            ImageView icon = v.findViewById(R.id.docIcon);

            title.setText(source.title);
            size.setText(Utils.parseSize(source.size) + " • " + source.ext.toUpperCase());

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

            boolean hasPhoto = source.photo_sizes != null && source.photo_sizes.forType("s") != null;
            icon.setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
            if (hasPhoto) {
                String src = source.photo_sizes.forType("s").src;

                if (!TextUtils.isEmpty(src))
                    Picasso.get()
                            .load(source.photo_sizes.forType("s").src)
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

    private void onAvatarLongClick(int position) {
        VKUser user = CacheStorage.getUser(getValues().get(position).fromId);
        if (user == null) return;

        Toast.makeText(context, user.toString(), Toast.LENGTH_SHORT).show();
    }

    public int getMessagesCount() {
        return getValues().size();
    }

    public void changeItems(ArrayList<VKMessage> messages) {
        if (!ArrayUtil.isEmpty(messages)) {
            this.getValues().clear();
            this.getValues().addAll(messages);
        }
    }

    public void add(VKMessage message, boolean anim) {
        getValues().add(message);
        if (anim) {
            notifyItemInserted(getValues().size() - 1);
        } else {
            notifyDataSetChanged();
        }
    }

    public void insert(ArrayList<VKMessage> messages) {
        this.getValues().addAll(0, messages);
    }

    public void change(VKMessage message) {
        for (int i = 0; i < getValues().size(); i++) {
            if (getValues().get(i).date == message.date) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void destroy() {
        getValues().clear();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }
}
