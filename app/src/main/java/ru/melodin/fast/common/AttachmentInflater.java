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
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.VKUtil;
import ru.melodin.fast.api.model.VKAudio;
import ru.melodin.fast.api.model.VKDoc;
import ru.melodin.fast.api.model.VKGraffiti;
import ru.melodin.fast.api.model.VKLink;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKModel;
import ru.melodin.fast.api.model.VKPhoto;
import ru.melodin.fast.api.model.VKSticker;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.api.model.VKVideo;
import ru.melodin.fast.api.model.VKVoice;
import ru.melodin.fast.api.model.VKWall;
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.view.CircleImageView;

public class AttachmentInflater {

    public static final String KEY_PLAY_AUDIO = "play_audio";
    public static final String KEY_PAUSE_AUDIO = "pause_audio";
    public static final String KEY_STOP_AUDIO = "stop_audio";
    private Context context;
    private LayoutInflater inflater;
    private DisplayMetrics metrics;

    public AttachmentInflater(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.metrics = context.getResources().getDisplayMetrics();
    }

    public synchronized static AttachmentInflater getInstance(Context context) {
        return new AttachmentInflater(context);
    }

    public void showForwardedMessages(VKMessage item, ViewGroup parent, boolean reply, boolean withStyles) {
        if (reply)
            message(item, parent, item.getReply().asMessage(), reply, withStyles);
        else
            for (int i = 0; i < item.getFwdMessages().size(); i++) {
                message(item, parent, item.getFwdMessages().get(i), false, withStyles);
            }
    }

    public void inflateAttachments(VKMessage item, ViewGroup parent, ViewGroup images, int maxWidth, boolean forwarded, boolean withStyles) {
        ArrayList<VKModel> attachments = item.getAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            VKModel attachment = attachments.get(i);
            if (attachment instanceof VKAudio) {
                audio(item, parent, (VKAudio) attachment, withStyles);
            } else if (attachment instanceof VKPhoto) {
                photo(item, images, (VKPhoto) attachment, forwarded ? maxWidth : -1);
            } else if (attachment instanceof VKSticker) {
                sticker(images, (VKSticker) attachment);
            } else if (attachment instanceof VKDoc) {
                doc(item, parent, (VKDoc) attachment, forwarded, withStyles);
            } else if (attachment instanceof VKLink) {
                link(item, parent, (VKLink) attachment, withStyles);
            } else if (attachment instanceof VKVideo) {
                video(parent, (VKVideo) attachment, forwarded ? maxWidth : -1);
            } else if (attachment instanceof VKGraffiti) {
                graffiti(parent, (VKGraffiti) attachment);
            } else if (attachment instanceof VKVoice) {
                voice(item, parent, (VKVoice) attachment, forwarded, withStyles);
            } else if (attachment instanceof VKWall) {
                wall(item, parent, (VKWall) attachment, withStyles);
            } else {
                empty(parent, context.getString(R.string.unknown));
            }
        }
    }

    public void loadImage(final ImageView image, final String smallSrc, final String normalSrc) {
        loadImage(image, smallSrc, normalSrc, null);
    }

    public void loadImage(final ImageView image, final String smallSrc, final String normalSrc, Drawable placeholder) {
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

    private LinearLayout.LayoutParams getParams() {
        return getParams(-1);
    }

    private LinearLayout.LayoutParams getParams(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private LinearLayout.LayoutParams getParams(int layoutWidth) {
        return layoutWidth == -1 ?
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT) :
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        getHeight(layoutWidth));
    }

    private FrameLayout.LayoutParams getFrameParams(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
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

    public void wall(VKMessage item, ViewGroup parent, final VKWall source, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        ImageView icon = v.findViewById(R.id.icon);

        title.setText(context.getString(R.string.wall_post));
        body.setText(context.getString(R.string.link));

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        body.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        @ColorInt int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut()) {
                iconColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
            } else {
                iconColor = ThemeManager.getAccent();
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }
        } else {
            titleColor = ThemeManager.getMain();
            bodyColor = ThemeManager.getSecondary();
            iconColor = titleColor;
        }

        Drawable arrow = context.getResources().getDrawable(R.drawable.ic_assignment_black_24dp);
        arrow.setTint(iconColor);

        icon.setImageDrawable(arrow);

        title.setTextColor(titleColor);
        body.setTextColor(bodyColor);

        parent.addView(v);
    }

    public void graffiti(ViewGroup parent, VKGraffiti source) {
        final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

        image.setLayoutParams(getParams());
        loadImage(image, source.url, null);

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void sticker(ViewGroup parent, VKSticker source) {
        final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

        image.setLayoutParams(getParams(400, 400));
        loadImage(image, ThemeManager.isDark() ? source.getMaxBackgroundSize() : source.getMaxSize(), null);

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void service(VKMessage item, ViewGroup parent) {
        TextView text = (TextView) inflater.inflate(R.layout.activity_messages_service, parent, false);

        text.setText(Html.fromHtml(VKUtil.getActionBody(item, false)));
        parent.addView(text);
    }

    public void video(ViewGroup parent, final VKVideo source, int maxWidth) {
        View v = inflater.inflate(R.layout.activity_messages_attach_video, parent, false);

        ImageView image = v.findViewById(R.id.image);
        TextView time = v.findViewById(R.id.time);

        String duration = String.format(AppGlobal.locale, "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        time.setText(duration);

        image.setLayoutParams(maxWidth == -1 ? getFrameParams(source.getMaxWidth(), FrameLayout.LayoutParams.WRAP_CONTENT) : getFrameParams(maxWidth));

        loadImage(image, source.getMaxSize(), null);
        parent.addView(v);
    }

    public void photo(final VKMessage item, ViewGroup parent, final VKPhoto source, int maxWidth) {
        ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

        image.setLayoutParams(maxWidth == -1 ? getParams(source.getMaxWidth(), source.getMaxHeight()) : getParams(maxWidth));
        image.setOnClickListener(v -> {
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

        loadImage(image, source.getMaxSize(), null);
        parent.addView(image);
    }

    public View message(VKMessage item, ViewGroup parent, VKMessage source, boolean reply, boolean withStyles) {
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

        if (TextUtils.isEmpty(user.getPhoto100())) {
            avatar.setVisibility(View.GONE);
        } else {
            avatar.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(user.getPhoto100())
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(new ColorDrawable(ColorUtil.darkenColor(ThemeManager.getPrimary())))
                    .into(avatar);
        }

        line.setBackgroundColor(withStyles ? ThemeManager.getAccent() : Color.TRANSPARENT);

        @ColorInt int nameColor, messageColor, lineColor;

        if (item != null) {
            if (item.isOut()) {
                lineColor = Color.WHITE;
                nameColor = Color.WHITE;
                messageColor = ColorUtil.darkenColor(nameColor);
            } else {
                lineColor = ThemeManager.getAccent();
                nameColor = ThemeManager.isDark() ? Color.WHITE : Color.DKGRAY;
                messageColor = ThemeManager.isDark() ? ColorUtil.lightenColor(nameColor) : ColorUtil.darkenColor(nameColor);
            }

            line.setBackgroundColor(lineColor);
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
            inflateAttachments(source, container, container, -1, true, withStyles);
        }

        if (!ArrayUtil.isEmpty(source.getFwdMessages())) {
            LinearLayout container = v.findViewById(R.id.forwarded);
            showForwardedMessages(source, container, false, withStyles);
        }

        if (parent != null)
            parent.addView(v);
        return v;
    }

    public void doc(VKMessage item, ViewGroup parent, final VKDoc source, boolean forwarded, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView size = v.findViewById(R.id.body);
        ImageView icon = v.findViewById(R.id.icon);

        title.setText(source.getTitle());

        String size_ = Util.parseSize(source.getSize()) + " • " + source.getExt().toUpperCase();
        size.setText(size_);

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        size.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        @ColorInt int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut() || forwarded) {
                iconColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor, 0.9f);
            } else {
                iconColor = ThemeManager.getAccent();
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }
        } else {
            titleColor = ThemeManager.getMain();
            bodyColor = ThemeManager.getSecondary();
            iconColor = titleColor;
        }

        icon.getDrawable().setTint(iconColor);

        title.setTextColor(titleColor);
        size.setTextColor(bodyColor);

        parent.addView(v);
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getUrl()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    public View voice(final VKMessage item, ViewGroup parent, final VKVoice source, boolean forwarded, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        final ImageButton play = v.findViewById(R.id.play);

        String duration = String.format(AppGlobal.locale, "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        title.setText(context.getString(R.string.voice_message));
        body.setText(duration);

        @ColorInt final int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut() || forwarded) {
                iconColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor, 0.9f);
            } else {
                iconColor = ThemeManager.getAccent();
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }
        } else {
            titleColor = ThemeManager.getMain();
            bodyColor = ThemeManager.getSecondary();
            iconColor = titleColor;
        }

        final Drawable start = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_black_24dp);
        final Drawable stop = ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_black_24dp);

        start.setTint(iconColor);
        stop.setTint(iconColor);

        final boolean playing = item.isPlaying();
        play.setImageDrawable(playing ? stop : start);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playing) {
                    EventBus.getDefault().postSticky(new Object[]{KEY_PAUSE_AUDIO, item.getId()});
                } else {
                    EventBus.getDefault().postSticky(new Object[]{KEY_PLAY_AUDIO, item.getId(), source.getLinkMp3()});
                }
            }
        });

        time.setTextColor(bodyColor);
        title.setTextColor(titleColor);
        body.setTextColor(bodyColor);

        title.setMaxWidth(metrics.widthPixels / 2);
        body.setMaxWidth(metrics.widthPixels / 2);
        time.setMaxWidth(metrics.widthPixels / 2);

        parent.addView(v);
        return v;
    }

    public void audio(VKMessage item, ViewGroup parent, VKAudio source, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

        TextView title = v.findViewById(R.id.abc_tb_title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        ImageButton play = v.findViewById(R.id.play);

        String duration = String.format(AppGlobal.locale, "%d:%02d", source.getDuration() / 60, source.getDuration() % 60);
        title.setText(source.getTitle());
        body.setText(source.getArtist());
        time.setText(duration);

        @ColorInt int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut()) {
                iconColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor, 0.9f);
            } else {
                iconColor = Color.WHITE;
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }
        } else {
            titleColor = ThemeManager.getMain();
            bodyColor = ThemeManager.getSecondary();
            iconColor = titleColor;
        }

        play.getDrawable().setTint(iconColor);

        time.setTextColor(bodyColor);
        title.setTextColor(titleColor);
        body.setTextColor(bodyColor);

        title.setMaxWidth(metrics.widthPixels / 2);
        body.setMaxWidth(metrics.widthPixels / 2);
        time.setMaxWidth(metrics.widthPixels / 2);

        parent.addView(v);
    }

    public void link(VKMessage item, ViewGroup parent, final VKLink source, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_link, parent, false);

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

        if (source != null)
            if (TextUtils.isEmpty(source.getPhoto().getMaxSize())) {
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

        @ColorInt
        int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut()) {
                iconColor = Color.WHITE;
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor, 0.9f);
            } else {
                iconColor = ThemeManager.getAccent();
                titleColor = ThemeManager.getPrimaryInverse();
                bodyColor = ThemeManager.isDark() ? ColorUtil.darkenColor(titleColor) : ColorUtil.lightenColor(titleColor);
            }
        } else {
            titleColor = ThemeManager.getMain();
            bodyColor = ThemeManager.getSecondary();
            iconColor = titleColor;
        }

        icon.getDrawable().setTint(iconColor);
        title.setTextColor(titleColor);
        description.setTextColor(bodyColor);

        parent.addView(v);

        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getUrl()));
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