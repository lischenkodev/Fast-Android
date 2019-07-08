package ru.melodin.fast

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_web_login.*
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.api.AppIds
import ru.melodin.fast.api.Auth
import ru.melodin.fast.api.Scopes
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.util.ViewUtil
import ru.melodin.fast.view.FastToolbar

class WebViewLoginActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.currentTheme)
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_login)

        tb.setBackVisible(true)
        tb.setOnBackClickListener (View.OnClickListener { onBackPressed() })
        tb.inflateMenu(R.menu.activity_login)

        web.visibility = View.GONE

        web.settings.javaScriptEnabled = true
        web.webViewClient = VKWebViewClient()
        web.settings.domStorageEnabled = true
        web.clearCache(true)

        val manager = CookieManager.getInstance()
        manager.removeAllCookies(null)
        manager.flush()
        manager.setAcceptCookie(true)

        try {
            web!!.loadUrl(Auth.getUrl(AppIds.FAST_ID, Scopes.allInt()))
        } catch (ignored: Exception) {
        }

        tb.setOnMenuItemClickListener(object : FastToolbar.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem) {
                if (item.itemId == R.id.refresh) {
                    progress.visibility = View.VISIBLE
                    web.visibility = View.GONE
                    web.reload()
                }
            }

        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_login, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun parseUrl(url: String?) {
        if (TextUtils.isEmpty(url)) return
        url?: return

        try {
            if (url.startsWith(Auth.REDIRECT_URL) && !url.contains("error=")) {
                val auth = Auth.parseRedirectUrl(url)
                val intent = Intent()
                intent.putExtra("token", auth[0])
                intent.putExtra("id", Integer.parseInt(auth[1]))
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    public override fun onDestroy() {
        super.onDestroy()

        if (web != null) {
            web.removeAllViews()
            web.clearCache(true)
            web.destroy()
        }

    }

    private inner class VKWebViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            progress.visibility = View.GONE
            web.visibility = View.VISIBLE
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            parseUrl(url)
        }
    }
}
