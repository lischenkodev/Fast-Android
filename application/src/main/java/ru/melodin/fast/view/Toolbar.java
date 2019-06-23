package ru.melodin.fast.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import ru.melodin.fast.R;

public class Toolbar extends androidx.appcompat.widget.Toolbar {

    private TextView title;
    private ImageView avatar;

    public Toolbar(Context context) {
        super(context);
        init();
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Toolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.abc_toolbar, this);
        title = findViewById(R.id.title);
        avatar = findViewById(R.id.avatar);
    }

    public void setAvatar(Drawable drawable) {
        avatar.setImageDrawable(drawable);
    }

    public void setAvatar(int resId) {
        avatar.setImageResource(resId);
    }

    public ImageView getAvatar() {
        return avatar;
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title.setText(title.toString().trim());
    }

    @Override
    public void setTitle(int resId) {
        String text = getContext().getString(resId);
        this.title.setText(text.trim());
    }

    @Override
    public String getTitle() {
        return title.getText().toString().trim();
    }
}
