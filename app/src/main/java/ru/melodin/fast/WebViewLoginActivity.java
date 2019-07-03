package ru.melodin.fast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.melodin.fast.api.AppIds;
import ru.melodin.fast.api.Auth;
import ru.melodin.fast.api.Scopes;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.FastToolbar;

public class WebViewLoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_login);

        progressBar = findViewById(R.id.progress);
        webView = findViewById(R.id.web);

        FastToolbar tb = findViewById(R.id.tb);
        tb.setBackVisible(true);
        tb.setOnBackClickListener((view) -> onBackPressed());
        tb.inflateMenu(R.menu.activity_login);

        webView.setVisibility(View.GONE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new VKWebViewClient());
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSaveFormData(true);
        webView.clearCache(true);

        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(null);
        manager.flush();
        manager.setAcceptCookie(true);

        webView.loadUrl(Auth.getUrl(AppIds.FAST_ID, Scopes.allInt()));

        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.refresh) {
                progressBar.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                webView.reload();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return super.onCreateOptionsMenu(menu);
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
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }
}
