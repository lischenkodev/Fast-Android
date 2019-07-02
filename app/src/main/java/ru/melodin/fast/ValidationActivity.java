package ru.melodin.fast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import ru.melodin.fast.api.Auth;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.util.ViewUtil;

public class ValidationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtil.applyWindowStyles(getWindow());
        setTheme(ThemeManager.getLoginTheme());
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        webView.setWebViewClient(new VKWebClient());
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSaveFormData(true);
        webView.clearCache(true);
        webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        CookieManager manager = CookieManager.getInstance();
        manager.removeAllCookies(null);
        manager.flush();
        manager.setAcceptCookie(true);

        String url = getIntent().getStringExtra("url");
        webView.loadUrl(url);
    }

    private void parseUrl(String url) {
        try {
            if (url.startsWith("https://oauth.vk.com/blank.html#success=1")) {
                if (!url.contains("error=")) {
                    String[] auth = Auth.parseRedirectUrl(url);
                    Intent intent = new Intent();
                    intent.putExtra("token", auth[0]);
                    intent.putExtra("id", Integer.parseInt(auth[1]));
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class VKWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }
}