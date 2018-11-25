package ru.stwtforever.fast;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;

import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.fragment.FragmentSettings;
import ru.stwtforever.fast.util.ViewUtils;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar tb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tb = findViewById(R.id.tb);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ViewUtils.applyToolbarStyles(tb);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentSettings()).commit();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }
}
