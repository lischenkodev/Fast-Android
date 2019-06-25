package ru.melodin.fast.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import ru.melodin.fast.R;
import ru.melodin.fast.SettingsActivity;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.current.BaseFragment;
import ru.melodin.fast.view.Toolbar;

public class FragmentItems extends BaseFragment {

    private Toolbar tb;
    private TextView userName;
    private ImageView userAvatar;
    private ImageView userOnline;

    private VKUser user;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = UserConfig.getUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        userName.setText(user.toString());

        if (!TextUtils.isEmpty(user.getPhoto200())) {
            Picasso.get().load(user.getPhoto200()).into(userAvatar);
        }

        userOnline.setVisibility(View.VISIBLE);
        userOnline.setImageDrawable(getOnlineIndicator(user));

        tb.inflateMenu(R.menu.fragment_items);
        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu) {
                    startActivity(new Intent(getContext(), SettingsActivity.class));
                }
            }
        });
    }

    @Nullable
    private Drawable getOnlineIndicator(@NonNull VKUser user) {
        return !user.isOnline() ? null : ContextCompat.getDrawable(getContext(), user.isOnlineMobile() ? R.drawable.ic_online_mobile : R.drawable.ic_online);
    }

    private void initViews(View v) {
        tb = v.findViewById(R.id.tb);
        userName = v.findViewById(R.id.user_name);
        userAvatar = v.findViewById(R.id.user_avatar);
        userOnline = v.findViewById(R.id.user_online);
    }
}
