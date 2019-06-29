package ru.melodin.fast;

import android.os.Bundle;

import androidx.annotation.Nullable;

import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.current.BaseActivity;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.Toolbar;

public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar tb = findViewById(R.id.tb);
        tb.setTitle(R.string.settings);
        tb.setBackVisible(true);
        tb.setOnBackClickListener(view -> onBackPressed());

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentSettings()).commit();
    }
}
