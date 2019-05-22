package ru.melodin.fast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ru.melodin.fast.api.Auth;
import ru.melodin.fast.api.Scopes;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.util.ViewUtil;

public class WebViewLoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar bar;

    private AlertDialog dialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_login);

        bar = findViewById(R.id.progress);
        webView = findViewById(R.id.web);

        setSupportActionBar((Toolbar) findViewById(R.id.tb));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        webView.setVisibility(View.GONE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new VKWebViewClient());

        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        webView.loadUrl(Auth.getUrl(UserConfig.VK_DESKTOP_ID, Scopes.allInt()));
        showWarningDialog();
    }

    private void showWarningDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.auth_vkdesktop_text)
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.token_login:
                showTokenLoginDialog();
                break;
            case R.id.refresh:
                webView.reload();
                break;
            case R.id.back:
                if (webView.canGoBack()) webView.goBack();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showTokenLoginDialog() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);

        View v = LayoutInflater.from(this).inflate(R.layout.token_login, null, false);
        adb.setView(v);
        adb.setMessage(R.string.token_login_message);

        final EditText etToken = v.findViewById(R.id.token);
        final EditText etUserId = v.findViewById(R.id.user_id);

        adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String token = etToken.getText().toString();
                final String uId = etUserId.getText().toString();

                if (!token.trim().isEmpty() && !uId.trim().isEmpty()) {
                    ThreadExecutor.execute(new AsyncCallback(WebViewLoginActivity.this) {

                        int id;

                        @Override
                        public void ready() {
                            id = Integer.parseInt(uId);
                        }

                        @Override
                        public void done() {
                            Intent intent = new Intent();
                            intent.putExtra("token", token);
                            intent.putExtra("id", id);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        }

                        @Override
                        public void error(Exception e) {
                            Toast.makeText(WebViewLoginActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        adb.setNegativeButton(android.R.string.cancel, null);
        adb.create().show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    private void parseUrl(String url) {
        if (TextUtils.isEmpty(url)) return;

        try {
            if (url.startsWith(Auth.REDIRECT_URL) && !url.contains("error=")) {
                String[] auth = Auth.parseRedirectUrl(url);
                Intent intent = new Intent();
                intent.putExtra("token", auth[0]);
                intent.putExtra("id", Integer.parseInt(auth[1]));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (webView != null) {
            webView.removeAllViews();
            webView.clearCache(true);
            webView.destroy();
            webView = null;
        }

    }

    private class VKWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            bar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }
}
