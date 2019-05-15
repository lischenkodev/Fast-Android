package ru.melodin.fast;

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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import ru.melodin.fast.adapter.PhotoViewAdapter;
import ru.melodin.fast.api.model.VKPhoto;
import ru.melodin.fast.common.PermissionManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.fragment.FragmentPhotoView;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

public class PhotoViewActivity extends AppCompatActivity {

    private Toolbar tb;
    private LinearLayout items;
    private ViewPager pager;

    private int animState = AnimState.SHOWED;
    private int likeState = LikeState.UNLIKED;

    private PhotoViewAdapter adapter;

    private ImageButton like, comment, repost;

    private class LikeState {
        static final int LIKED = 1;
        static final int UNLIKED = 2;
    }

    private class AnimState {
        static final int SHOWED = 1;
        static final int HIDED = 2;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        PermissionManager.setActivity(this);
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

        if (!ArrayUtil.isEmpty(photos) && Util.hasConnection()) {
            createAdapter(photos);
        }

        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                like.getDrawable().setTint(likeState == LikeState.UNLIKED ? Color.RED : Color.WHITE);
                likeState = likeState == LikeState.UNLIKED ? LikeState.LIKED : LikeState.UNLIKED;
            }
        });
    }

    private String getUrl() {
        if (pager == null) return "";
        if (pager.getAdapter() == null) return "";

        int position = pager.getCurrentItem();

        Fragment[] fragments = adapter.getFragments();
        return ((FragmentPhotoView) fragments[position]).getUrl();
    }

    public void changeTbVisibility() {
        if (animState == AnimState.SHOWED) {
            animState = AnimState.HIDED;
            tb.animate().alpha(0).setDuration(300).setListener(hideListener).start();
            items.animate().alpha(0).setDuration(300).start();
        } else {
            animState = AnimState.SHOWED;
            tb.animate().alpha(1).setDuration(300).setListener(showListener).start();
            items.animate().alpha(1).setDuration(300).start();
        }
    }

    private void checkPermissions() {
        if (PermissionManager.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            savePhoto();
        } else {
            PermissionManager.requestPermissions(23, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 23) {
            if (PermissionManager.isGranted(grantResults[0])) {
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
                Util.saveFileByUrl(getUrl());
            }

            @Override
            public void done() {

            }

            @Override
            public void error(Exception e) {

            }

        });
    }

    private void copyUrl() {
        String url = getUrl();
        Util.copyText(url);
        Toast.makeText(this, R.string.url_copied_text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_photo_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void createAdapter(final ArrayList<VKPhoto> photos) {
        if (ArrayUtil.isEmpty(photos)) return;
        pager = findViewById(R.id.pager);

        pager.setOffscreenPageLimit(photos.size() - 1);

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
        //EventBus.getDefault().postSticky(arrayOf < Any > (-1))
        super.onDestroy();
    }

    private Animator.AnimatorListener hideListener = new Animator.AnimatorListener() {
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
                onBackPressed();
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
}
