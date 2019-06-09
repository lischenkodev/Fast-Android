package ru.melodin.fast;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import ru.melodin.fast.api.Auth;
import ru.melodin.fast.api.UserConfig;

public class ValidationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView view = new WebView(this);
        view.setWebViewClient(new VKWebClient());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(view);

        String url = getIntent().getStringExtra("url");

        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }

        view.loadUrl(url);
    }

    private void parseUrl(String url) {
        try {
            if (url.startsWith("https://oauth.vk.com/blank.html#success=1")) {
                if (!url.contains("error=")) {
                    String[] auth = Auth.parseRedirectUrl(url);

                    UserConfig.accessToken = auth[0];
                    UserConfig.userId = Integer.parseInt(auth[1]);
                    UserConfig.save();

                    setResult(RESULT_OK);
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