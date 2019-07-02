package ru.melodin.fast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.current.BaseActivity;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.Requests;
import ru.melodin.fast.util.ViewUtil;

public class LoginActivity extends BaseActivity {

    private String login_, password_;

    private TextInputLayout password, login;
    private MaterialButton sign;

    private TextView webLogin;

    private AlertDialog processDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeManager.getLoginTheme());
        setContentView(R.layout.activity_login);

        sign = findViewById(R.id.btn_login);

        login = findViewById(R.id.login);
        password = findViewById(R.id.password);

        sign.setOnClickListener(view -> login(false));

        findViewById(R.id.logo_text).setOnClickListener(view -> toggleTheme());

        MaterialCardView cardView = findViewById(R.id.card);
        cardView.animate().translationY(200).setDuration(0).withEndAction(() -> cardView.animate().translationY(0).setDuration(500).start()).start();

        password.getEditText().setOnEditorActionListener((textView, i, keyEvent) -> {
            login(true);
            ViewUtil.hideKeyboard(password.getEditText());
            return true;
        });

        webLogin = findViewById(R.id.web_login);
        webLogin.setVisibility(View.GONE);
        webLogin.setOnClickListener(view -> {
            webLogin.setEnabled(false);
            startWebLogin();
        });
    }

    private void login(boolean fromKeyboard) {
        String login = this.login.getEditText().getText().toString().trim();
        String password = this.password.getEditText().getText().toString().trim();

        if (login.isEmpty() || password.isEmpty()) {
            if (!fromKeyboard)
                Snackbar.make(sign, R.string.all_necessary_data, Snackbar.LENGTH_SHORT).show();
            return;
        }

        login_ = login;
        password_ = password;

        processDialog = createProcessDialog();

        login(login_, password_, "");
    }

    @NonNull
    private AlertDialog createProcessDialog() {
        return new AlertDialog.Builder(this)
                .setMessage(R.string.loading)
                .create();
    }

    public void login(final String login, final String password, String captcha) {
        if (!processDialog.isShowing())
            processDialog.show();
        final WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSaveFormData(true);
        webView.getSettings().setLoadsImagesAutomatically(false);
        webView.getSettings().setUserAgentString("Chrome/41.0.2228.0 Safari/537.36");

        webView.clearCache(true);

        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(null);
        manager.flush();
        manager.setAcceptCookie(false);

        final String url =
                "https://oauth.vk.com/token?grant_type=password&client_id=2274003&scope=notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,email,notifications,stats,ads,,market,offline&client_secret=hHbZxrka2uZ6jB1inYsH&username="
                        + login + "&password=" + password + captcha + "&v=5.68";

        webView.addJavascriptInterface(new HandlerInterface(), "HtmlHandler");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:window.HtmlHandler.handleHtml" +
                        "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
            }
        });

        webView.loadUrl(url);
    }

    public void authorize(final String jsonObject) {
        if (processDialog.isShowing())
            processDialog.dismiss();
        new LowThread(() -> {
            try {
                JSONObject response = new JSONObject(jsonObject);
                if (response.has("error")) {
                    final String error = response.optString("error");

                    switch (error) {
                        case "need_validation":
                            final String redirect_uri = response.optString("redirect_uri");
                            runOnUiThread(() -> {
                                Intent intent = new Intent(LoginActivity.this, ValidationActivity.class);
                                intent.putExtra("url", redirect_uri);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivityForResult(intent, Requests.VALIDATE_LOGIN);
                            });
                            break;
                        case "need_captcha":
                            String captcha_img = response.optString("captcha_img");
                            String captcha_sid = response.optString("captcha_sid");
                            showCaptchaDialog(captcha_sid, captcha_img);
                            break;
                        default:
                            Snackbar.make(sign, R.string.error, Snackbar.LENGTH_LONG).show();
                            break;
                    }
                } else {
                    UserConfig.userId = response.optInt("user_id", -1);
                    UserConfig.accessToken = response.optString("access_token");
                    UserConfig.save();

                    processDialog.show();

                    getCurrentUser(UserConfig.userId);
                    startMainActivity();
                }
            } catch (Exception e) {
                Log.e("Error auth void", e.toString());
            }
        }).start();
    }

    private void showCaptchaDialog(final String captcha_sid, final String captcha_img) {
        runOnUiThread(() -> {
            DisplayMetrics metrics = getResources().getDisplayMetrics();

            ImageView image = new ImageView(LoginActivity.this);
            image.setLayoutParams(new ViewGroup.LayoutParams((int) (metrics.widthPixels / 3.5), getResources().getDisplayMetrics().heightPixels / 7));

            Picasso.get().load(captcha_img).priority(Picasso.Priority.HIGH).into(image);

            final TextInputEditText input = new TextInputEditText(LoginActivity.this);

            input.setHint(getString(R.string.captcha));
            input.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);

            LinearLayout layout = new LinearLayout(LoginActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.addView(image);
            layout.addView(input);

            adb.setView(layout);
            adb.setNegativeButton(android.R.string.cancel, null);
            adb.setPositiveButton(android.R.string.ok,
                    (dialog, id) -> {
                        String captcha_code = input.getText().toString().trim();
                        login(login_, password_, "&captcha_sid=" + captcha_sid + "&captcha_key=" + captcha_code);
                    });
            adb.setTitle("Input text from picture:");
            adb.setCancelable(true);
            AlertDialog alert = adb.create();
            alert.show();
        });
    }

    private void startWebLogin() {
        startActivityForResult(new Intent(this, WebViewLoginActivity.class), Requests.WEB_LOGIN);
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArray("fields", new String[]{login.getEditText().getText().toString().trim(), password.getEditText().getText().toString().trim()});
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String[] fields = savedInstanceState.getStringArray("fields");
        if (fields == null) return;
        login.getEditText().setText(fields[0]);
        password.getEditText().setText(fields[1]);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!webLogin.isEnabled())
            webLogin.setEnabled(true);

        if ((requestCode == Requests.VALIDATE_LOGIN || requestCode == Requests.WEB_LOGIN) && resultCode == Activity.RESULT_OK) {
            String token = data.getStringExtra("token");
            int id = data.getIntExtra("id", -1);

            UserConfig.userId = id;
            UserConfig.accessToken = token;
            UserConfig.save();
            VKApi.config = UserConfig.restore();

            getCurrentUser(id);
            startMainActivity();
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
                if (processDialog != null && processDialog.isShowing())
                    processDialog.dismiss();
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(LoginActivity.this, R.string.error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleTheme() {
        ThemeManager.toggleTheme();
        applyStyles();
    }

    private class HandlerInterface {

        @JavascriptInterface
        public void handleHtml(String html) {
            Document doc = Jsoup.parse(html);
            String response_string = doc.select("pre[style=\"word-wrap: break-word; white-space: pre-wrap;\"]").first().text();
            authorize(response_string);
        }
    }
}
