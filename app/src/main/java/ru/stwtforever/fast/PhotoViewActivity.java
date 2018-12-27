package ru.stwtforever.fast;

import android.Manifest;
import android.animation.Animator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import ru.stwtforever.fast.adapter.PhotoViewAdapter;
import ru.stwtforever.fast.api.model.VKPhoto;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.fragment.FragmentPhotoView;
import ru.stwtforever.fast.helper.PermissionHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;

public class PhotoViewActivity extends AppCompatActivity {

    private Toolbar tb;
    private LinearLayout items;
    private ViewPager pager;

    private int state = 0, like_state = 0;

    private PhotoViewAdapter adapter;

    private ImageButton like, comment, repost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PermissionHelper.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        tb = findViewById(R.id.toolbar);
        items = findViewById(R.id.items);
        like = findViewById(R.id.like);
        comment = findViewById(R.id.comment);
        repost = findViewById(R.id.repost);

        setSupportActionBar(tb);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<VKPhoto> photos = (ArrayList<VKPhoto>) getIntent().getSerializableExtra("photo");

        getSupportActionBar().setTitle(getString(R.string.photo_of_photo, "1", String.valueOf(photos.size())));

        if (!ArrayUtil.isEmpty(photos) && Utils.hasConnection()) {
            createAdapter(photos);
        }

        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                like.getDrawable().setTint(like_state == 0 ? Color.RED : Color.WHITE);
                like_state = like_state == 0 ? 1 : 0;
            }
        });
    }

    public void changeTbVisibility() {
        if (state == 0) {
            state = 1;
            tb.animate().alpha(0).setDuration(300).setListener(goneListener).start();
            items.animate().alpha(0).setDuration(300).start();
        } else {
            state = 0;
            tb.animate().alpha(1).setDuration(300).setListener(showListener).start();
            items.animate().alpha(1).setDuration(300).start();
        }
    }

    private Animator.AnimatorListener goneListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if (tb.getVisibility() != View.INVISIBLE) {
                tb.setVisibility(View.INVISIBLE);
            }

            if (items.getVisibility() != View.INVISIBLE) {
                items.setVisibility(View.INVISIBLE);
            }
        }
        @Override
        public void onAnimationCancel(Animator animation) {
        }
        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private Animator.AnimatorListener showListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            if (tb.getVisibility() != View.VISIBLE) {
                tb.setVisibility(View.VISIBLE);
            }

            if (items.getVisibility() != View.VISIBLE) {
                items.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }
        @Override
        public void onAnimationCancel(Animator animation) {

        }
        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.save:
                checkPermissions();
                break;
            case R.id.copy_link:
                copyUrl();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        if (PermissionHelper.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            savePhoto();
        } else {
            PermissionHelper.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 23);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 23) {
            if (PermissionHelper.isGrantedPermission(grantResults[0])) {
                savePhoto();
            } else {
                Toast.makeText(this, R.string.fast_rqrs_prmsn_for_save_photo, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhoto() {
        ThreadExecutor.execute(new AsyncCallback(this) {
            @Override
            public void ready() throws Exception {
                Utils.saveFileByUrl(getUrl());
            }

            @Override
            public void done() {
                Toast.makeText(PhotoViewActivity.this, R.string.photo_saved_in_downloads_directory, Toast.LENGTH_LONG).show();
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(PhotoViewActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyUrl() {
        String url = getUrl();
        Utils.copyText(url);
        Toast.makeText(this, R.string.url_copied_text, Toast.LENGTH_SHORT).show();
    }

    private String getUrl() {
        if (pager == null) return null;
        if (pager.getAdapter() == null) return null;

        int selected_position = pager.getCurrentItem();

        Fragment[] fragments = ((PhotoViewAdapter) pager.getAdapter()).getFragments();

        return ((FragmentPhotoView) fragments[selected_position]).getUrl();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_photo_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void createAdapter(final ArrayList<VKPhoto> photos) {
        pager = findViewById(R.id.pager);

        pager.setOffscreenPageLimit(10);

        adapter = new PhotoViewAdapter(getSupportFragmentManager(), photos);
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setTitle(getString(R.string.photo_of_photo, String.valueOf(position + 1), String.valueOf(photos.size())));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().postSticky(new Object[] {-1});
        super.onDestroy();
    }
}
