package ru.stwtforever.fast

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import ru.stwtforever.fast.api.Auth
import ru.stwtforever.fast.api.Scopes
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor

class WebViewLoginActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var bar: ProgressBar? = null

    private var tb: Toolbar? = null

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_login)

        bar = findViewById(R.id.progress)
        webView = findViewById(R.id.web)

        tb = findViewById(R.id.tb)
        setSupportActionBar(tb)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        webView!!.visibility = View.GONE

        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView!!.webViewClient = VKWebViewClient()

        webView!!.loadUrl(Auth.getUrl(UserConfig.FAST_ID, Scopes.all()))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_login, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.token_login -> showTokenLoginDialog()
            R.id.refresh -> webView!!.reload()
            R.id.back -> if (webView!!.canGoBack()) webView!!.goBack()

        }
        return super.onOptionsItemSelected(item)
    }

    private fun showTokenLoginDialog() {
        val adb = AlertDialog.Builder(this)

        val v = LayoutInflater.from(this).inflate(R.layout.token_login, null, false)
        adb.setView(v)
        adb.setMessage(R.string.token_login_message)

        val etToken = v.findViewById<EditText>(R.id.token)
        val etUserId = v.findViewById<EditText>(R.id.user_id)

        adb.setPositiveButton(android.R.string.ok, ({ _, _ ->
            val token = etToken.text.toString()
            val uId = etUserId.text.toString()

            if (!token.trim().isEmpty() && !uId.trim().isEmpty()) {
                ThreadExecutor.execute(object : AsyncCallback(this@WebViewLoginActivity) {
                    var id: Int = 0

                    override fun ready() {
                        id = Integer.parseInt(uId)
                    }

                    override fun done() {
                        val intent = Intent()
                        intent.putExtra("token", token)
                        intent.putExtra("id", id)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }

                    override fun error(e: Exception) {
                        Toast.makeText(this@WebViewLoginActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
                    }
                })
            }
        }))
        adb.setNegativeButton(android.R.string.cancel, null)
        adb.create().show()
    }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }


    private fun parseUrl(url: String) {
        if (TextUtils.isEmpty(url)) return

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

    override fun onDestroy() {
        super.onDestroy()

        if (webView != null) {
            webView!!.removeAllViews()
            webView!!.clearCache(true)
            webView!!.destroy()
            webView = null
        }

    }

    private inner class VKWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            bar!!.visibility = View.GONE
            webView!!.visibility = View.VISIBLE
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            parseUrl(url)
        }
    }
}
