package ru.melodin.fast

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.appcompat.app.AppCompatActivity

import ru.melodin.fast.api.Auth
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.util.ViewUtil

class ValidationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtil.applyWindowStyles(window)
        setTheme(ThemeManager.loginTheme)
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.webViewClient = VKWebClient()
        webView.settings.domStorageEnabled = true
        webView.clearCache(true)
        webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val manager = CookieManager.getInstance()
        manager.removeAllCookies(null)
        manager.flush()
        manager.setAcceptCookie(true)

        val url = intent.getStringExtra("url")
        webView.loadUrl(url)

        setContentView(webView)
    }

    private fun parseUrl(url: String) {
        try {
            if (url.startsWith("https://oauth.vk.com/blank.html#success=1")) {
                if (!url.contains("error=")) {
                    val auth = Auth.parseRedirectUrl(url)
                    val intent = Intent()
                    intent.putExtra("token", auth[0])
                    intent.putExtra("id", Integer.parseInt(auth[1]))
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private inner class VKWebClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
            parseUrl(url)
        }
    }
}