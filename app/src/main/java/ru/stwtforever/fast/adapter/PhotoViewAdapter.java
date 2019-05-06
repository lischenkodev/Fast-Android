package ru.stwtforever.fast.adapter;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import ru.stwtforever.fast.api.model.VKPhoto;
import ru.stwtforever.fast.fragment.FragmentPhotoView;

public class PhotoViewAdapter extends FragmentPagerAdapter {

    private ArrayList<VKPhoto> items;

    private Fragment[] fragments;

    public PhotoViewAdapter(@NonNull FragmentManager fm, ArrayList<VKPhoto> items) {
        super(fm);
        this.items = items;

        fragments = new Fragment[items.size()];

        for (int i = 0; i < fragments.length; i++) {
            fragments[i] = FragmentPhotoView.Companion.newInstance(items.get(i));
        }
    }

    public Fragment[] getFragments() {
        return fragments;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }
}
