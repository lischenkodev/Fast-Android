package ru.melodin.fast.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;

import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.adapter.MessageAdapter;
import ru.melodin.fast.api.VKUtil;
import ru.melodin.fast.api.model.VKGraffiti;
import ru.melodin.fast.api.model.VKLink;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.api.model.VKSticker;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.api.model.VKVoice;
import ru.melodin.fast.api.model.VKWall;
import ru.melodin.fast.api.model.attachment.VKAudio;
import ru.melodin.fast.api.model.attachment.VKDoc;
import ru.melodin.fast.api.model.attachment.VKPhoto;
import ru.melodin.fast.api.model.attachment.VKVideo;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class AttachmentInflater {

    public static final String KEY_PLAY_AUDIO = "play_audio";
    public static final String KEY_PAUSE_AUDIO = "pause_audio";
    public static final String KEY_STOP_AUDIO = "stop_audio";

    private Context context;
    private LayoutInflater inflater;
    private DisplayMetrics metrics;

    private @Nullable
    MessageAdapter adapter;

    public AttachmentInflater(@Nullable MessageAdapter adapter, Context context) {
        this.adapter = adapter;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.metrics = context.getResources().getDisplayMetrics();
    }

    @Contract("_, _ -> new")
    @NonNull
    public synchronized static AttachmentInflater getInstance(@Nullable MessageAdapter adapter, Context context) {
        return new AttachmentInflater(adapter, context);
    }

    public void showForwardedMessages(VKMessage item, ViewGroup parent, boolean withStyles) {
        for (int i = 0; i < item.getFwdMessages().size(); i++) {
            message(item, parent, item.getFwdMessages().get(i), false, withStyles);
        }
    }

    private void inflateAttachments(@NonNull VKMessage item, ViewGroup parent, ViewGroup images, int maxWidth, boolean forwarded) {
        ArrayList<VKModel> attachments = item.getAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            VKModel attachment = attachments.get(i);
            if (attachment instanceof VKAudio) {
                audio(item, parent, (VKAudio) attachment);
            } else if (attachment instanceof VKPhoto) {
                photo(item, images, (VKPhoto) attachment, forwarded ? maxWidth : -1);
            } else if (attachment instanceof VKSticker) {
                sticker(item, images, (VKSticker) attachment);
            } else if (attachment instanceof VKDoc) {
                doc(item, parent, (VKDoc) attachment);
            } else if (attachment instanceof VKLink) {
                link(item, parent, (VKLink) attachment);
            } else if (attachment instanceof VKVideo) {
                video(item, parent, (VKVideo) attachment, forwarded ? maxWidth : -1);
            } else if (attachment instanceof VKGraffiti) {
                graffiti(item, parent, (VKGraffiti) attachment);
            } else if (attachment instanceof VKVoice) {
                voice(item, parent, (VKVoice) attachment);
            } else if (attachment instanceof VKWall) {
                wall(item, parent, (VKWall) attachment);
            } else {
                empty(parent, context.getString(R.string.unknown));
            }
        }
    }

    private void loadImage(final ImageView image, final String smallSrc, final String normalSrc) {
        loadImage(image, smallSrc, normalSrc, null);
    }

    private void loadImage(final ImageView image, final String smallSrc, final String normalSrc, Drawable placeholder) {
        if (TextUtils.isEmpty(smallSrc)) return;
        try {
            Picasso.get()
                    .load(smallSrc)
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(placeholder == null ? new ColorDrawable(Color.TRANSPARENT) : placeholder)
                    .into(image, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {
                            if (TextUtils.isEmpty(normalSrc))
                                return;

                            Picasso.get()
                                    .load(normalSrc)
                                    .placeholder(image.getDrawable())
                                    .priority(Picasso.Priority.HIGH)
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

    @NonNull
    private LinearLayout.LayoutParams getParams() {
        return getParams(-1);
    }

    @NonNull
    @Contract("_, _ -> new")
    private LinearLayout.LayoutParams getParams(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    @NonNull
    private LinearLayout.LayoutParams getParams(int layoutWidth) {
        return layoutWidth == -1 ?
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT) :
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getHeight(layoutWidth));
    }

    @NonNull
    @Contract("_, _ -> new")
    private FrameLayout.LayoutParams getFrameParams(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
    }

    @NonNull
    @Contract("_ -> new")
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

    public void empty(@NonNull ViewGroup parent, String s) {
        final TextView body = new TextView(context);

        String string = "[" + s + "]";
        body.setText(string);

        body.setClickable(false);
        body.setFocusable(false);

        parent.addView(body);
    }

    public void wall(VKMessage item, ViewGroup parent, final VKWall source) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);
        v.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        v.setOnClickListener(view -> isSelected(item));

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        ImageView icon = v.findViewById(R.id.icon);

        title.setText(context.getString(R.string.wall_post));
        body.setText(context.getString(R.string.link));

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        body.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_assignment_black_24dp);
        icon.setImageDrawable(drawable);

        parent.addView(v);
    }

    public void graffiti(VKMessage item, ViewGroup parent, @NonNull VKGraffiti source) {
        final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);
        image.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        image.setOnClickListener(view -> isSelected(item));

        image.setLayoutParams(getParams());
        loadImage(image, source.url, null);

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void sticker(VKMessage item, ViewGroup parent, VKSticker source) {
        final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);
        image.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        image.setOnClickListener(view -> isSelected(item));

        image.setLayoutParams(getParams(400, 400));
        loadImage(image, ThemeManager.Companion.isDark() ? source.getMaxBackgroundSize() : source.getMaxSize(), null);

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void service(VKMessage item, ViewGroup parent) {
        TextView text = (TextView) inflater.inflate(R.layout.activity_messages_service, parent, false);

        text.setText(Html.fromHtml(VKUtil.INSTANCE.getActionBody(item, false)));
        parent.addView(text);
    }

    public void video(VKMessage item, ViewGroup parent, @NonNull final VKVideo source, int maxWidth) {
        View v = inflater.inflate(R.layout.activity_messages_attach_video, parent, false);
        v.setOnLongClickListener((view) -> {
            simulateLongClick(item);
            return true;
        });

        v.setOnClickListener(view -> isSelected(item));

        ImageView image = v.findViewById(R.id.image);
        TextView time = v.findViewById(R.id.time);

        String duration = String.format(AppGlobal.Companion.getLocale(), "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        time.setText(duration);

        image.setLayoutParams(maxWidth == -1 ? getFrameParams(source.getMaxWidth(), FrameLayout.LayoutParams.WRAP_CONTENT) : getFrameParams(maxWidth));

        loadImage(image, source.getMaxSize(), null);
        parent.addView(v);
    }

    public void photo(final VKMessage item, ViewGroup parent, @NonNull final VKPhoto source, int maxWidth) {
        ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

        image.setLayoutParams(maxWidth == -1 ? getParams(source.getMaxWidth(), source.getMaxHeight()) : getParams(maxWidth));
        image.setOnClickListener(v -> {
            if (isSelected(item)) return;

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
        });

        image.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        loadImage(image, source.getMaxSize(), null);
        parent.addView(image);
    }


    public void reply(VKMessage item, ViewGroup parent, boolean withStyles) {
        View v = message(item, null, item.getReply().asMessage(), true, withStyles);
        v.setOnClickListener(view -> adapter.getActivity().chooseMessage(item.getReply().asMessage()));
        parent.addView(v);
    }

    public View message(VKMessage item, ViewGroup parent, @NonNull VKMessage source, boolean isReply, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_message, parent, false);
        v.setClickable(false);
        v.setFocusable(false);

        if (item != null) {
            v.setOnLongClickListener((view -> {
                simulateLongClick(item);
                return true;
            }));

            v.setOnClickListener(view -> isSelected(item));
        }

        TextView name = v.findViewById(R.id.userName);
        TextView message = v.findViewById(R.id.userMessage);
        ImageView avatar = v.findViewById(R.id.userAvatar);
        View line = v.findViewById(R.id.message_line);

        message.setSingleLine(isReply);

        VKUser user = MemoryCache.INSTANCE.getUser(source.getFromId());
        if (user == null) {
            user = VKUser.EMPTY;
        }

        if (TextUtils.isEmpty(user.getPhoto100()) || isReply) {
            avatar.setVisibility(View.GONE);
        } else {
            avatar.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(user.getPhoto100())
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(avatar);
        }

        line.setBackgroundColor(withStyles ? ThemeManager.Companion.getAccent() : Color.TRANSPARENT);

        @ColorInt int lineColor = item != null ? ThemeManager.Companion.getAccent() : Color.TRANSPARENT;
        line.setBackgroundColor(lineColor);

        name.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));
        message.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 3));

        name.setText(user.toString());

        if (TextUtils.isEmpty(source.getText())) {
            message.setVisibility(View.GONE);
        } else {
            message.setText(source.getText());
        }

        if (!ArrayUtil.INSTANCE.isEmpty(source.getAttachments()) && !isReply) {
            LinearLayout container = v.findViewById(R.id.attachments);
            inflateAttachments(source, container, container, -1, true);
        }

        if (!ArrayUtil.INSTANCE.isEmpty(source.getFwdMessages()) && !isReply) {
            LinearLayout container = v.findViewById(R.id.forwarded);
            showForwardedMessages(source, container, withStyles);
        }

        if (parent != null) parent.addView(v);
        return v;
    }

    public void doc(VKMessage item, ViewGroup parent, @NonNull final VKDoc source) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);
        v.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView size = v.findViewById(R.id.body);

        title.setText(source.getTitle());

        String size_ = Util.INSTANCE.parseSize(source.getSize()) + " • " + source.getExt().toUpperCase();
        size.setText(size_);

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        size.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        parent.addView(v);
        v.setOnClickListener(p1 -> {
            if (isSelected(item)) return;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    public void voice(@NonNull final VKMessage item, ViewGroup parent, @NonNull final VKVoice source) {
        View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);
        v.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        v.setOnClickListener(view -> isSelected(item));

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        final ImageButton play = v.findViewById(R.id.play);

        String duration = String.format(AppGlobal.Companion.getLocale(), "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        title.setText(context.getString(R.string.voice_message));
        body.setText(duration);

        final Drawable start = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_black_24dp);
        final Drawable stop = ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_black_24dp);

        final boolean playing = item.isPlaying();
        play.setImageDrawable(playing ? stop : start);

        play.setOnClickListener(view -> {
            if (playing) {
                EventBus.getDefault().postSticky(new Object[]{KEY_PAUSE_AUDIO, item.getId()});
            } else {
                EventBus.getDefault().postSticky(new Object[]{KEY_PLAY_AUDIO, item.getId(), source.getLinkMp3()});
            }
        });

        title.setMaxWidth(metrics.widthPixels / 2);
        body.setMaxWidth(metrics.widthPixels / 2);
        time.setMaxWidth(metrics.widthPixels / 2);

        parent.addView(v);
    }

    public void audio(@Nullable VKMessage item, ViewGroup parent, @NonNull VKAudio source) {
        View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

        v.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        v.setOnClickListener(view -> isSelected(item));

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        ImageButton play = v.findViewById(R.id.play);
        final Drawable start = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_black_24dp);
        final Drawable stop = ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_black_24dp);

        final boolean playing = item.isPlaying();
        play.setImageDrawable(playing ? stop : start);

        play.setOnClickListener(view -> {
            if (playing) {
                EventBus.getDefault().postSticky(new Object[]{KEY_PAUSE_AUDIO, item.getId()});
            } else {
                EventBus.getDefault().postSticky(new Object[]{KEY_PLAY_AUDIO, item.getId(), source.getUrl()});
            }
        });

        String duration = String.format(AppGlobal.Companion.getLocale(), "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        title.setText(source.getTitle());
        body.setText(source.getArtist());
        time.setText(duration);

        title.setMaxWidth(metrics.widthPixels / 2);
        body.setMaxWidth(metrics.widthPixels / 2);
        time.setMaxWidth(metrics.widthPixels / 2);

        parent.addView(v);
    }

    public void link(VKMessage item, ViewGroup parent, @NonNull final VKLink source) {
        View v = inflater.inflate(R.layout.activity_messages_attach_link, parent, false);
        v.setOnLongClickListener((view -> {
            simulateLongClick(item);
            return true;
        }));

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView description = v.findViewById(R.id.description);
        ImageView icon = v.findViewById(R.id.icon);
        CircleImageView photo = v.findViewById(R.id.photo);

        title.setText(source.getTitle());

        String body = TextUtils.isEmpty(source.getCaption()) ? TextUtils.isEmpty(source.getDescription()) ? "" : source.getDescription() : source.getCaption();

        if (body.isEmpty()) {
            description.setVisibility(View.GONE);
            description.setText("");
        } else {
            description.setVisibility(View.VISIBLE);
            description.setText(body);
        }

        if (source.getPhoto() == null || TextUtils.isEmpty(source.getPhoto().getMaxSize())) {
            photo.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
        } else {
            photo.setVisibility(View.VISIBLE);
            icon.setVisibility(View.GONE);

            Picasso.get()
                    .load(source.getPhoto().getMaxSize())
                    .into(photo);
        }

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        description.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        parent.addView(v);

        v.setOnClickListener(p1 -> {
            if (isSelected(item)) return;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getUrl()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                intent.setPackage(null);
                context.startActivity(intent);
            }
        });
    }

    @Contract(pure = true)
    private boolean isSelected(@NonNull VKMessage item) {
        if (adapter == null) return false;
        if (adapter.isSelected()) {
            simulateClick(item);
            return true;
        }

        return false;
    }

    private void simulateClick(VKMessage item) {
        if (adapter == null) return;

        int position = adapter.searchPosition(item.getId());
        if (position == -1) return;

        adapter.getActivity().onItemClick( position);
    }

    private void simulateLongClick(VKMessage item) {
        if (adapter == null) return;

        int position = adapter.searchPosition(item.getId());
        if (position == -1) return;

        adapter.getActivity().onItemLongClick( position);
    }
}