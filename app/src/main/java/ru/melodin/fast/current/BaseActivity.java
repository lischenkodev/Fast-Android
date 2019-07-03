package ru.melodin.fast.current;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.util.ViewUtil;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
    }

    public void applyStyles() {
        recreate();
    }

    public Drawable drawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(this, resId);
    }

    @ColorInt
    public int color(@ColorRes int resId) {
        return ContextCompat.getColor(this, resId);
    }

}
