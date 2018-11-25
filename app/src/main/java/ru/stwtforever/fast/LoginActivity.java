package ru.stwtforever.fast;

import android.content.*;
import android.os.*;

import androidx.appcompat.app.*;

import android.view.*;
import android.widget.*;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.*;

import java.util.*;

import androidx.cardview.widget.CardView;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.helper.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.view.*;

import ru.stwtforever.fast.util.ViewUtils;

public class LoginActivity extends AppCompatActivity {

    private CardView card;
    private FloatingActionButton fab;
    private TextView name;
    private CircleImageView avatar;
    private ImageView logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        logo = findViewById(R.id.logo);
        card = findViewById(R.id.card);
        fab = findViewById(R.id.fab);
        name = findViewById(R.id.name);
        avatar = findViewById(R.id.avatar);

        logo.setColorFilter(getResources().getColor(R.color.primary));
        name.setTypeface(FontHelper.getFont(FontHelper.PS_REGULAR));

        fab.setOnClickListener(closeClick);

        setUserData(null);

        logo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ThemeManager.update(!ThemeManager.isDark());
                ViewUtils.update();
                finish();
                startActivity(getIntent());
                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
            }
        });

        UserConfig.restore();
        UserConfig.updateUser();
        if (UserConfig.isLoggedIn()) {
            VKUser user = UserConfig.getUser();
            setUserData(user);
        }
    }

    private View.OnClickListener logoutClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            showExitDialog();
        }
    };

    private View.OnClickListener closeClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            startMainActivity();
        }
    };

    private View.OnClickListener loginClick = new View.OnClickListener() {

        @Override
        public void onClick(View p1) {
            login();
        }
    };

    private void showExitDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.warning);
        adb.setMessage(R.string.exit_message);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                UserConfig.clear();
                setUserData(null);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    private void login() {
        startActivityForResult(new Intent(this, WebViewLoginActivity.class), Requests.LOGIN);
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Requests.LOGIN && resultCode == RESULT_OK) {
            String token = data.getStringExtra("token");
            int id = data.getIntExtra("id", -1);

            UserConfig config = new UserConfig(token, null, id, UserConfig.FAST_ID);
            config.save();
            VKApi.config = config;

            getCurrentUser(id);
            checkUpdates();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getCurrentUser(final int id) {
        ThreadExecutor.execute(new AsyncCallback(this) {

            VKUser user;

            @Override
            public void ready() throws Exception {
                ArrayList<Integer> ids = new ArrayList<>();
                ids.add(id);
                user = VKApi.users().get().userIds(ids).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);

                ArrayList<VKUser> users = new ArrayList<>();
                users.add(user);

                CacheStorage.insert(DBHelper.USERS_TABLE, users);

                UserConfig.updateUser();
            }

            @Override
            public void done() {
                setUserData(user);
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
            name.setText(R.string.add_account);
            avatar.setImageResource(R.drawable.placeholder_user);

            card.setOnClickListener(loginClick);
            fab.hide();
            return;
        }

        card.setOnClickListener(logoutClick);

        name.setText(user.toString());

        Picasso.get()
                .load(user.photo_200)
                .priority(Picasso.Priority.HIGH)
                .into(avatar, new Callback.EmptyCallback() {
                    @Override
                    public void onSuccess() {
                        ViewUtils.fadeImage(avatar);
                    }
                });

        fab.show();
    }

    private void checkUpdates() {
        OTAManager.checkUpdate(this, false);
    }
}
