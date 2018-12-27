package ru.stwtforever.fast;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import ru.stwtforever.fast.adapter.PhotoViewAdapter;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;

public class PhotoViewActivity extends AppCompatActivity {

    private Toolbar tb;
    private LinearLayout items;

    private int state = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        tb = findViewById(R.id.toolbar);
        items = findViewById(R.id.items);
        setSupportActionBar(tb);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tb.getNavigationIcon().setTint(Color.WHITE);


        ArrayList<String> src = getIntent().getStringArrayListExtra("photo");

        getSupportActionBar().setTitle("1 of " + src.size());

        if (!ArrayUtil.isEmpty(src) && Utils.hasConnection()) {
            createAdapter(src);
        }
    }

    public void changeTbVisibility() {
        if (state == 0) {
            state = 1;
            tb.animate().alpha(0).setDuration(300).start();
            items.animate().alpha(0).setDuration(300).start();
        } else {
            state = 0;
            tb.animate().alpha(1).setDuration(300).start();
            items.animate().alpha(1).setDuration(300).start();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    private void createAdapter(final ArrayList<String> urls) {
        ViewPager pager = findViewById(R.id.pager);

        pager.setOffscreenPageLimit(10);
        pager.setAdapter(new PhotoViewAdapter(getSupportFragmentManager(), urls));
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setTitle((position + 1) + " of " + urls.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
