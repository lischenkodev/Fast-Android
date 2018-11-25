package ru.stwtforever.fast;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

import ru.stwtforever.fast.common.ThemeManager;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Button b = findViewById(R.id.btn);

        b.setTextColor(ThemeManager.isDark() ? Color.WHITE : Color.BLACK);

        Drawable search = getResources().getDrawable(R.drawable.ic_search);
        search.setTint(ThemeManager.isDark() ? Color.WHITE : 0xff404040);
        search.setBounds(0, 0, 60, 60);

        b.setCompoundDrawablesWithIntrinsicBounds(search, null, null, null);

        if (b.getBackground() == null) return;

        if (ThemeManager.isDark()) {
            b.getBackground().setTint(0xff404040);
        } else {
            b.getBackground().setTint(Color.WHITE);
        }
    }
}
