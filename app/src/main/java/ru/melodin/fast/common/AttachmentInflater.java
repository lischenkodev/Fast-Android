package ru.melodin.fast.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

public class AttachmentInflater {

    private Context context;
    private LayoutInflater inflater;
    private DisplayMetrics metrics;

    public synchronized static AttachmentInflater getInstance(Context context) {
        return new AttachmentInflater(context);
    }

    public AttachmentInflater(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.metrics = context.getResources().getDisplayMetrics();
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
                photo(item, images, (VKPhoto) attachment);
            } else if (attachment instanceof VKSticker) {
                sticker(parent, (VKSticker) attachment);
            } else if (attachment instanceof VKDoc) {
                doc(item, parent, (VKDoc) attachment, forwarded, withStyles);
            } else if (attachment instanceof VKLink) {
                link(item, parent, (VKLink) attachment, withStyles);
            } else if (attachment instanceof VKVideo) {
                video(parent, (VKVideo) attachment, maxWidth);
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
        if (TextUtils.isEmpty(smallSrc)) return;
        try {
            Picasso.get()
                    .load(smallSrc)
                    .priority(Picasso.Priority.HIGH)
                    .placeholder(new ColorDrawable(Color.GRAY))
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

    public void wall(VKMessage item, ViewGroup parent, final VKWall source, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

        TextView title = v.findViewById(R.id.title);
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
        loadImage(image, source.url, "");

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void sticker(ViewGroup parent, VKSticker source) {
        final ImageView image = (ImageView) inflater.inflate(R.layout.activity_messages_attach_photo, parent, false);

        image.setLayoutParams(getParams());
        loadImage(image, source.src_256, null);

        image.setClickable(false);
        image.setFocusable(false);

        parent.addView(image);
    }

    public void service(VKMessage item, ViewGroup parent) {
        TextView text = new TextView(context);
        text.setTextColor(ThemeManager.getAccent());

        text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        text.setGravity(Gravity.CENTER);
        text.setText(Html.fromHtml(VKUtil.getActionBody(item, false)));

        text.setClickable(false);
        text.setFocusable(false);
        parent.addView(text);
    }

    public void video(ViewGroup parent, final VKVideo source, int width) {
        View v = inflater.inflate(R.layout.activity_messages_attach_video, parent, false);

        ImageView image = v.findViewById(R.id.image);
        TextView title = v.findViewById(R.id.title);
        TextView time = v.findViewById(R.id.time);

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
            loadImage(image, source.getMaxSize(), source.getMaxSize());
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

        line.setBackground(lineBackground);

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

            lineBackground.setColor(lineColor);
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

        TextView title = v.findViewById(R.id.title);
        TextView size = v.findViewById(R.id.body);
        ImageView icon = v.findViewById(R.id.icon);

        title.setText(source.title);

        String size_ = Util.parseSize(source.size) + " • " + source.ext.toUpperCase();
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

        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_vector_file);
        drawable.setTint(iconColor);

        icon.setImageDrawable(drawable);

        title.setTextColor(titleColor);
        size.setTextColor(bodyColor);

        if (source.photo_sizes != null) {
            String src = source.photo_sizes.forType("s").src;

            if (!TextUtils.isEmpty(src))
                Picasso.get()
                        .load(src)
                        .placeholder(new ColorDrawable(ColorUtil.darkenColor(ThemeManager.getPrimary())))
                        .into(icon);
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

    public View voice(VKMessage item, ViewGroup parent, VKVoice source, boolean forwarded, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false);

        TextView title = v.findViewById(R.id.title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        ImageButton play = v.findViewById(R.id.play);

        String duration = String.format(AppGlobal.locale, "%d:%02d", source.duration / 60, source.duration % 60);
        title.setText(context.getString(R.string.voice_message));
        body.setText(duration);

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

        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_vector_play_arrow);
        drawable.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);

        play.setImageDrawable(drawable);

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

        TextView title = v.findViewById(R.id.title);
        TextView body = v.findViewById(R.id.body);
        TextView time = v.findViewById(R.id.duration);

        ImageButton play = v.findViewById(R.id.play);

        String duration = String.format(AppGlobal.locale, "%d:%02d",
                source.duration / 60,
                source.duration % 60);
        title.setText(source.title);
        body.setText(source.artist);
        time.setText(duration);

        @ColorInt int titleColor, bodyColor, iconColor;

        if (withStyles) {
            if (item.isOut()) {
                iconColor = ThemeManager.getAccent();
                titleColor = Color.WHITE;
                bodyColor = ColorUtil.darkenColor(titleColor);
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

        Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_play_arrow);
        arrow.setTint(iconColor);

        play.setImageDrawable(arrow);

        time.setTextColor(bodyColor);
        title.setTextColor(titleColor);
        body.setTextColor(bodyColor);

        title.setMaxWidth(metrics.widthPixels / 2);
        body.setMaxWidth(metrics.widthPixels / 2);
        time.setMaxWidth(metrics.widthPixels / 2);

        parent.addView(v);
    }

    public void link(VKMessage item, ViewGroup parent, final VKLink source, boolean withStyles) {
        View v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false);

        TextView title = v.findViewById(R.id.title);
        TextView body = v.findViewById(R.id.body);
        ImageView icon = v.findViewById(R.id.icon);

        title.setText(source.title);
        body.setText(TextUtils.isEmpty(source.description) ? source.caption : source.description);

        title.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));
        body.setMaxWidth(metrics.widthPixels - (metrics.widthPixels / 2));

        @ColorInt int titleColor, bodyColor, iconColor;

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

        Drawable arrow = context.getResources().getDrawable(R.drawable.ic_vector_link_arrow);
        arrow.setTint(iconColor);

        icon.setImageDrawable(arrow);

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