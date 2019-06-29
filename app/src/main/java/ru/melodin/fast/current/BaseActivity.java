package ru.melodin.fast.current;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

}
