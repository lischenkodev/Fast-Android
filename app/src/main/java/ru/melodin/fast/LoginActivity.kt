package ru.melodin.fast

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import ru.melodin.fast.api.OnResponseListener
import ru.melodin.fast.api.Scopes
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.net.HttpRequest
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*

class LoginActivity : BaseActivity() {

    private var login: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.LOGIN_THEME)
        ViewUtil.applyWindowStyles(window, ThemeManager.BACKGROUND)
        setContentView(R.layout.activity_login)

        progress.visibility = View.INVISIBLE
        progress.indeterminateTintList =
            ColorStateList.valueOf(ColorUtil.saturateColor(ThemeManager.ACCENT, 2f))

        buttonLogin.shrink()
        buttonLogin.extend()

        buttonLogin.setOnClickListener {
            if (!buttonLogin.isExtended) {
                toggleButton()
            } else {
                login(false)
            }
        }

        logoText.setOnClickListener { toggleTheme() }

        if (ThemeManager.IS_DARK) {
            logoText.setTextColor(Color.WHITE)
            val stateList = ColorStateList.valueOf(ThemeManager.ACCENT)
            iconEmail.imageTintList = stateList
            iconKey.imageTintList = stateList
        } else {
            val boxColor = ColorUtil.darkenColor(ThemeManager.BACKGROUND, 0.98f)
            inputLogin.boxBackgroundColor = boxColor
            inputPassword.boxBackgroundColor = boxColor
        }

        val anim = intent.getBooleanExtra("show_anim", true)

        if (anim)
            card.animate().translationY(200f).setDuration(0).withEndAction {
                card.animate().translationY(0f).setDuration(500).start()
            }.start()

        val bundle = intent.getBundleExtra("data")
        if (bundle != null)
            onRestoreInstanceState(bundle)

        inputPassword.editText!!.setOnEditorActionListener { _, _, _ ->
            ViewUtil.hideKeyboard(inputPassword.editText!!)
            login(true)
            true
        }

        webLogin!!.setOnClickListener {
            webLogin!!.isEnabled = false
            startWebLogin()
        }
    }

    private fun login(fromKeyboard: Boolean) {
        val login = inputLogin.editText!!.text.toString().trim()
        val password = inputPassword.editText!!.text.toString().trim()

        if (login.isEmpty() || password.isEmpty()) {
            if (!fromKeyboard)
                ViewUtil.snackbar(buttonLogin, R.string.all_necessary_data).show()
            return
        }

        this.login = login
        this.password = password

        login(this.login, this.password, "")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login(login: String?, password: String?, captcha: String) {
        if (!Util.hasConnection()) {
            ViewUtil.snackbar(buttonLogin, R.string.connect_to_the_internet).show()
            return
        }

        toggleButton()

        TaskManager.execute {
            val url =
                "https://oauth.vk.com/token?grant_type=password&client_id=2274003&scope=${Scopes.all()}&client_secret=hHbZxrka2uZ6jB1inYsH&username=$login&password=$password$captcha&v=5.68"

            try {
                val response = JSONObject(HttpRequest[url].asString())
                runOnUiThread {
                    parseResponse(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showErrorSnackbar(e.toString())
                }
            }
        }
    }

    private fun parseResponse(response: JSONObject) {
        toggleButton()

        if (response.has("error")) {
            val errorDescription = response.optString("error_description")

            when (response.optString("error", getString(R.string.error))) {
                "need_validation" -> {
                    val redirectUri = response.optString("redirect_uri")
                    val intent =
                        Intent(this@LoginActivity, ValidationActivity::class.java).apply {
                            putExtra("url", redirectUri)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    startActivityForResult(intent, REQUEST_VALIDATE)
                }
                "need_captcha" -> {
                    val captchaImg = response.optString("captcha_img")
                    val captchaSid = response.optString("captcha_sid")
                    showCaptchaDialog(captchaSid, captchaImg)
                }
                else ->
                    runOnUiThread {
                        showErrorSnackbar(errorDescription)
                    }
            }
        } else {
            UserConfig.userId = response.optInt("user_id", -1)
            UserConfig.accessToken = response.optString("access_token")
            UserConfig.save()

            getCurrentUser()
            startMainActivity()
        }
    }

    private fun showErrorSnackbar(text: String) {
        Snackbar.make(
            buttonLogin,
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showCaptchaDialog(captcha_sid: String, captcha_img: String) {
        val metrics = resources.displayMetrics

        val image = ImageView(this@LoginActivity)
        image.layoutParams = ViewGroup.LayoutParams(
            (metrics.widthPixels / 3.5).toInt(),
            resources.displayMetrics.heightPixels / 7
        )

        Picasso.get().load(captcha_img).priority(Picasso.Priority.HIGH).into(image)

        val input = TextInputEditText(this@LoginActivity)

        input.hint = getString(R.string.captcha)
        input.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val adb = AlertDialog.Builder(this@LoginActivity)

        val layout = LinearLayout(this@LoginActivity)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.addView(image)
        layout.addView(input)

        adb.setView(layout)
        adb.setNegativeButton(android.R.string.cancel, null)
        adb.setPositiveButton(android.R.string.ok) { _, _ ->
            val captchaCode = input.text!!.toString().trim()
            login(login, password, "&captcha_sid=$captcha_sid&captcha_key=$captchaCode")
        }
        adb.setTitle(R.string.input_text_from_picture)
        adb.setCancelable(true)
        val alert = adb.create()
        alert.show()
    }

    private fun startWebLogin() {
        startActivityForResult(Intent(this, WebViewLoginActivity::class.java), REQUEST_WEB_LOGIN)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(createBundle(outState))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val fields = savedInstanceState.getStringArray("fields") ?: return
        inputLogin.editText!!.setText(fields[0])
        inputPassword.editText!!.setText(fields[1])
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!webLogin!!.isEnabled)
            webLogin!!.isEnabled = true

        if ((requestCode == REQUEST_VALIDATE || requestCode == REQUEST_WEB_LOGIN) && (resultCode == Activity.RESULT_OK)) {
            data ?: return
            val token = data.getStringExtra("token")
            val id = data.getIntExtra("id", -1)

            UserConfig.userId = id
            UserConfig.accessToken = token
            UserConfig.save()

            VKApi.config = UserConfig.restore()

            getCurrentUser()
            startMainActivity()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getCurrentUser() {
        TaskManager.execute {
            VKApi.users().get().fields(VKUser.FIELDS_DEFAULT)
                .execute(VKUser::class.java, object : OnResponseListener {
                    override fun onComplete(models: ArrayList<*>?) {
                        if (ArrayUtil.isEmpty(models)) return
                        models ?: return

                        val user = models[0] as VKUser

                        CacheStorage.insert(DatabaseHelper.USERS_TABLE, user)
                        UserConfig.getUser()
                    }

                    override fun onError(e: Exception) {
                        Toast.makeText(this@LoginActivity, R.string.error, Toast.LENGTH_LONG).show()
                    }

                })
        }
    }

    private fun createBundle(savedInstanceState: Bundle): Bundle {
        savedInstanceState.putStringArray(
            "fields",
            arrayOf(
                inputLogin.editText!!.text.toString().trim(),
                inputPassword.editText!!.text.toString().trim()
            )
        )
        return savedInstanceState
    }

    private fun toggleTheme() {
        ThemeManager.toggleTheme()
        applyStyles()
    }

    override fun applyStyles() {
        finish()
        startActivity(intent.putExtra("data", createBundle(Bundle())).putExtra("show_anim", false))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun toggleButton() {
        if (buttonLogin.isExtended) {
            progress.visibility = View.VISIBLE
            buttonLogin.shrink(true)
            buttonLogin.icon = drawable(R.drawable.ic_refresh)
        } else {
            progress.visibility = View.INVISIBLE
            buttonLogin.extend(true)
            buttonLogin.icon = drawable(R.drawable.md_done)
        }
    }

    companion object {
        const val REQUEST_WEB_LOGIN = 1
        const val REQUEST_VALIDATE = 2
    }
}
