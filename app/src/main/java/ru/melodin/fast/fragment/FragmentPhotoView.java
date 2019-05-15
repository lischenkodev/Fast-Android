package ru.melodin.fast.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ru.melodin.fast.PhotoViewActivity;
import ru.melodin.fast.api.model.VKPhoto;
import ru.melodin.fast.util.Util;

public class FragmentPhotoView extends Fragment {

    private VKPhoto photo;

    private String url;

    public static FragmentPhotoView newInstance(VKPhoto photo) {
        FragmentPhotoView fragment = new FragmentPhotoView();
        fragment.photo = photo;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ImageView image = new ImageView(getContext());
        image.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        return image;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String maxSize = photo.getMaxSize();
        url = maxSize;

        if (TextUtils.isEmpty(maxSize) || !Util.hasConnection()) return;

        loadPhoto(maxSize);

        getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PhotoViewActivity) getActivity()).changeTbVisibility();
            }
        });
    }

    public String getUrl() {
        return url;
    }

    private void loadPhoto(String url) {
        try {
            Picasso.get().load(url).placeholder(new ColorDrawable(Color.GRAY)).into((ImageView) getView());
        } catch (Exception e) {
            Log.e("Error load photo", Log.getStackTraceString(e));
        }
    }
}
