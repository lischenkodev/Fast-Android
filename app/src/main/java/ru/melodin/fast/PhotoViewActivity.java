package ru.melodin.fast;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

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

    private AnimState animState = AnimState.SHOWED;
    private LikeState likeState = LikeState.UNLIKED;

    private PhotoViewAdapter adapter;
    private VKPhoto source;

    private ImageButton like, comment, repost;

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

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        changeTbVisibility();

        ArrayList<VKPhoto> photos = (ArrayList<VKPhoto>) getIntent().getSerializableExtra("photo");
        source = (VKPhoto) getIntent().getSerializableExtra("selected");

        if (ArrayUtil.isEmpty(photos)) {
            finish();
            return;
        }

        int selectedPosition = 0;

        if (source != null)
            for (int i = 0; i < photos.size(); i++) {
                VKPhoto photo = photos.get(i);
                if (photo.getId() == source.getId())
                    selectedPosition = i;
            }

        pager = findViewById(R.id.pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setTitle(position + 1, photos.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (Util.hasConnection()) {
            createAdapter(photos);
        }

        if (selectedPosition > 0)
            pager.setCurrentItem(selectedPosition);

        setTitle(selectedPosition + 1, photos.size());

        like.setOnClickListener(v -> {
            like.getDrawable().setTint(likeState == LikeState.UNLIKED ? Color.RED : Color.WHITE);
            likeState = likeState == LikeState.UNLIKED ? LikeState.LIKED : LikeState.UNLIKED;
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
            animState = AnimState.HIDDEN;
            tb.animate().alpha(0).setDuration(300).withEndAction(() -> {
                if (tb.getVisibility() != View.INVISIBLE) {
                    tb.setVisibility(View.INVISIBLE);
                }

                if (items.getVisibility() != View.INVISIBLE) {
                    items.setVisibility(View.INVISIBLE);
                }
            }).start();
            items.animate().alpha(0).setDuration(300).start();
        } else {
            animState = AnimState.SHOWED;
            tb.animate().alpha(1).setDuration(300).withStartAction(() -> {
                if (tb.getVisibility() != View.VISIBLE) {
                    tb.setVisibility(View.VISIBLE);
                }

                if (items.getVisibility() != View.VISIBLE) {
                    items.setVisibility(View.VISIBLE);
                }
            }).start();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 23) {
            if (PermissionManager.isGranted(grantResults[0])) {
                savePhoto();
            } else {
                Toast.makeText(this, R.string.photo_permission_title, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhoto() {
        ThreadExecutor.execute(new AsyncCallback(this) {
            String path;

            @Override
            public void ready() throws Exception {
                path = Util.saveFileByUrl(getUrl());
            }

            @Override
            public void done() {
                Toast.makeText(PhotoViewActivity.this, getString(R.string.saved_into, path), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(PhotoViewActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
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
        pager.setOffscreenPageLimit(photos.size() == 1 ? 1 : photos.size() - 1);

        adapter = new PhotoViewAdapter(getSupportFragmentManager(), photos);
        pager.setAdapter(adapter);
    }

    private void setTitle(int current, int max) {
        getSupportActionBar().setTitle(getString(R.string.photo_of_photo, current, max));
    }

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

    private enum LikeState {
        LIKED, UNLIKED
    }

    private enum AnimState {
        SHOWED, HIDDEN
    }
}
