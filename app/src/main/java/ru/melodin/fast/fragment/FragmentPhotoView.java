package ru.melodin.fast.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.Contract;

import ru.melodin.fast.BuildConfig;
import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.api.model.VKPhoto;
import ru.melodin.fast.util.Util;

public class FragmentPhotoView extends Fragment {

    private VKPhoto photo;

    private String url;

    private int yDelta;

    public static FragmentPhotoView newInstance(VKPhoto photo) {
        FragmentPhotoView fragment = new FragmentPhotoView();
        fragment.photo = photo;
        return fragment;
    }

    @NonNull
    @Override
    public FrameLayout getView() {
        return (FrameLayout) super.getView();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout layout = new FrameLayout(getContext());
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setFitsSystemWindows(true);

        ImageView image = new ImageView(getContext());
        image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        image.setAdjustViewBounds(true);

        layout.addView(image);
        return layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (photo == null) return;
        String maxSize = photo.getMaxSize();
        url = maxSize;

        if (TextUtils.isEmpty(maxSize) || !Util.hasConnection()) return;

        loadPhoto(maxSize);

        getView().setOnClickListener(v -> ((PhotoViewActivity) getActivity()).changeTbVisibility());
        getView().getChildAt(0).setOnTouchListener(getOnTouchListener());
    }

    public String getUrl() {
        return url;
    }

    private void loadPhoto(String url) {
        try {
            Picasso.get().load(url).placeholder(new ColorDrawable(Color.GRAY)).into((ImageView) getView().getChildAt(0));
        } catch (Exception e) {
            Log.e("Error load photo", Log.getStackTraceString(e));
        }
    }

    @NonNull
    @Contract(pure = true)
    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener getOnTouchListener() {
        return (view, event) -> {
            final int y = (int) event.getRawY();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    FrameLayout.LayoutParams lParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                    yDelta = y - lParams.topMargin;
                    break;

                case MotionEvent.ACTION_UP:
                    int top = view.getTop();
                    if (top < 0) top *= -1;

                    int max = getResources().getDisplayMetrics().heightPixels / 6;

                    if (BuildConfig.DEBUG) {
                        String swipeInfo = " \ntop: " + top + "\nheight: " + getResources().getDisplayMetrics().heightPixels + "\nmax: " + max;
                        Log.d("swipeInfo", swipeInfo);
                    }

                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                    params.topMargin = 0;
                    view.setLayoutParams(params);

                    if (top > max) {
                        if (view.getTop() > 0) {
                            params.topMargin = view.getHeight() * 2;
                        } else {
                            params.topMargin = Math.abs(view.getHeight() * 2);
                        }

                        //view.setLayoutParams(params);
                        getActivity().finish();
                        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                    layoutParams.topMargin = y - yDelta;
                    view.setLayoutParams(layoutParams);
                    break;
            }

            getView().invalidate();
            return true;
        };
    }

}
