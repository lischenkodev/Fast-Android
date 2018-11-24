package ru.stwtforever.fast;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import ru.stwtforever.fast.adapter.MaterialsFragmentAdapter;
import ru.stwtforever.fast.common.ThemeManager;

public class MaterialsActivity extends AppCompatActivity {

    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager viewPager;
    long uid, cid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materials);

        uid = getIntent().getExtras().getLong("uid");
        cid = getIntent().getExtras().getLong("cid");
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.setBackgroundColor(Color.WHITE);
        //tabLayout.setTabTextColors(Color.GRAY, ThemeManager.color);
    }

    private void setupViewPager(ViewPager viewPager) {
        MaterialsFragmentAdapter adapter = new MaterialsFragmentAdapter(getSupportFragmentManager());
        // adapter.addFragment(new FragmentPhotos(), getString(R.string.materials_photo));
        //  //     adapter.addFragment(new FragmentAudios(), getString(R.string.materials_audio));
        //  /  adapter.addFragment(new FragmentVideos(), getString(R.string.materials_video));
        // )/  adapter.addFragment(new FragmentDocuments(), getString(R.string.materials_doc));
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
