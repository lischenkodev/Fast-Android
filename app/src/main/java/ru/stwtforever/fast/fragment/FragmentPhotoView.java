package ru.stwtforever.fast.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ru.stwtforever.fast.PhotoViewActivity;
import ru.stwtforever.fast.util.Utils;

public class FragmentPhotoView extends Fragment {

    private String url;

    public static FragmentPhotoView newInstance(String url) {
        FragmentPhotoView fragment = new FragmentPhotoView();
        Bundle b = new Bundle();
        b.putString("url", url);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        url = getArguments().getString("url", "");
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

        if (!TextUtils.isEmpty(url) && Utils.hasConnection()) {
            loadImage(url);
        }

        getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PhotoViewActivity) getActivity()).changeTbVisibility();
            }
        });
    }

    private void loadImage(String url) {
        try {
            Picasso.get().load(url).placeholder(new ColorDrawable(Color.GRAY)).into(((ImageView) getView()));
        } catch (Exception ignored) {
        }
    }
}
