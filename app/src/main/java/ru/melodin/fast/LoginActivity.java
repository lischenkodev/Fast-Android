package ru.melodin.fast;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.Requests;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.CircleImageView;

public class LoginActivity extends AppCompatActivity {
    private MaterialCardView card;
    private TextView name;
    private CircleImageView avatar;

    private boolean loggedIn;
    private View.OnClickListener loginClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            login();
        }
    };
    private View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startMainActivity();
        }
    };
    private View.OnLongClickListener logoutClick = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (loggedIn)
                showExitDialog();
            return true;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ViewUtil.applyWindowStyles(getWindow());
        setTheme(ThemeManager.getLoginTheme());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login2);

        findViewById(R.id.logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeManager.switchTheme(!ThemeManager.isDark());
                Util.restart(LoginActivity.this, true);
            }
        });

        card = findViewById(R.id.card);

        card = findViewById(R.id.card);
        name = findViewById(R.id.name);
        avatar = findViewById(R.id.avatar);

        setUserData(null);

        UserConfig.restore();
        UserConfig.getUser();
        if (UserConfig.isLoggedIn()) {
            setUserData(UserConfig.getUser());
        }
    }

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.warning);
        adb.setMessage(R.string.exit_message);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserConfig.clear();
                setUserData(null);
                loggedIn = false;
                card.setOnClickListener(loginClick);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.create().show();
    }

    private void login() {
        startActivityForResult(new Intent(this, WebViewLoginActivity.class), Requests.LOGIN);
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Requests.LOGIN && resultCode == Activity.RESULT_OK) {
            String token = data.getStringExtra("token");
            int id = data.getIntExtra("id", -1);

            UserConfig config = new UserConfig(token, null, id, UserConfig.VK_DESKTOP_ID);
            config.save();
            VKApi.config = config;

            getCurrentUser(id);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getCurrentUser(final int id) {
        ThreadExecutor.execute(new AsyncCallback(this) {

            VKUser user;

            @Override
            public void ready() throws Exception {
                user = VKApi.users().get().userIds(id).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, user);
                UserConfig.getUser();
            }

            @Override
            public void done() {
                loggedIn = true;
                setUserData(user);
                card.setOnClickListener(closeClick);
            }

            @Override
            public void error(Exception e) {
                setUserData(null);
                Toast.makeText(LoginActivity.this, R.string.error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setUserData(VKUser user) {
        if (user == null) {
            loggedIn = false;
            name.setText(R.string.add_account);
            avatar.setVisibility(View.GONE);
            name.setGravity(Gravity.CENTER);

            card.setOnClickListener(loginClick);
            card.setOnLongClickListener(null);
            return;
        }

        loggedIn = true;

        avatar.setVisibility(View.VISIBLE);
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        card.setOnLongClickListener(logoutClick);
        card.setOnClickListener(closeClick);

        name.setText(user.toString());

        Picasso.get()
                .load(user.photo_200)
                .priority(Picasso.Priority.HIGH)
                .into(avatar, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        ViewUtil.fadeView(avatar, true);
                    }
                });
    }

}
