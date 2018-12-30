package ru.stwtforever.fast

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
import android.widget.Button
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
import ru.stwtforever.fast.helper.FontHelper
import ru.stwtforever.fast.util.ViewUtils

class WebViewLoginActivity : AppCompatActivity() {

    private var webView: WebView? = null
    private var bar: ProgressBar? = null

    private var tb: Toolbar? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.applyWindowStyles(this)
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_login)



        bar = findViewById(R.id.progress)
        webView = findViewById(R.id.web)

        tb = findViewById(R.id.tb)
        setSupportActionBar(tb)

        webView!!.visibility = View.GONE

        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView!!.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView!!.webViewClient = VKWebViewClient()

        webView!!.loadUrl(Auth.getUrl(UserConfig.FAST_ID, Scopes.all()))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_login, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.token_login -> showTokenLoginDialog()
            R.id.refresh -> {
                webView!!.reload()
            }
            R.id.back -> {
                if (webView!!.canGoBack()) webView!!.goBack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showTokenLoginDialog() {
        val adb = AlertDialog.Builder(this)

        val v = LayoutInflater.from(this).inflate(R.layout.token_login, null, false)
        adb.setView(v)
        adb.setMessage(R.string.token_login_message)

        val ok = v.findViewById<Button>(R.id.ok)
        val cancel = v.findViewById<Button>(R.id.cancel)

        val etToken = v.findViewById<EditText>(R.id.token)
        val etUserId = v.findViewById<EditText>(R.id.user_id)

        FontHelper.setFont(arrayOf(etToken, etUserId), FontHelper.PS_REGULAR)

        val dialog = adb.create()
        dialog.show()

        ok.setOnClickListener {
            val token = etToken.text.toString()
            val uId = etUserId.text.toString()

            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(uId)) {
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
                        dialog.dismiss()
                    }

                    override fun error(e: Exception) {
                        Toast.makeText(this@WebViewLoginActivity, getString(R.string.error), Toast.LENGTH_LONG).show()
                    }
                })
            }
        }

        cancel.setOnClickListener { dialog.dismiss() }
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
