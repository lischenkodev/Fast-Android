package ru.stwtforever.fast

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.database.CacheStorage
import ru.stwtforever.fast.database.DatabaseHelper
import ru.stwtforever.fast.helper.FontHelper
import ru.stwtforever.fast.util.Requests
import ru.stwtforever.fast.util.ViewUtils
import ru.stwtforever.fast.view.CircleImageView
import java.util.*

class LoginActivity : AppCompatActivity() {

    private var card: CardView? = null
    private var name: TextView? = null
    private var avatar: CircleImageView? = null
    private var logo: ImageView? = null

    private var loggedIn: Boolean = false

    private val logoutClick = View.OnLongClickListener {
        if (loggedIn)
            showExitDialog()
        return@OnLongClickListener true
    }

    private val closeClick = View.OnClickListener { startMainActivity() }

    private val loginClick = View.OnClickListener { login() }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login2)

        logo = findViewById(R.id.logo)
        card = findViewById(R.id.card)
        name = findViewById(R.id.name)
        avatar = findViewById(R.id.avatar)

        name!!.typeface = FontHelper.getFont(FontHelper.PS_REGULAR)


        setUserData(null)

        findViewById<ImageView>(R.id.logo).drawable.setColorFilter(ThemeManager.getAccent(), PorterDuff.Mode.MULTIPLY)

        logo!!.setOnClickListener {
            ThemeManager.update(!ThemeManager.isDark())
            ViewUtils.update()
            finish()
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        UserConfig.restore()
        UserConfig.updateUser()
        if (UserConfig.isLoggedIn()) {
            val user = UserConfig.getUser()
            setUserData(user)
        }
    }

    private fun showExitDialog() {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.warning)
        adb.setMessage(R.string.exit_message)
        adb.setPositiveButton(R.string.yes) { _, _ ->
            UserConfig.clear()
            setUserData(null)
            loggedIn = false
            card!!.setOnClickListener(loginClick)
        }
        adb.setNegativeButton(R.string.no, null)
        val alert = adb.create()
        alert.show()
    }

    private fun login() {
        startActivityForResult(Intent(this, WebViewLoginActivity::class.java), Requests.LOGIN)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Requests.LOGIN && resultCode == Activity.RESULT_OK) {
            val token = data!!.getStringExtra("token")
            val id = data.getIntExtra("id", -1)

            val config = UserConfig(token, null, id, UserConfig.FAST_ID)
            config.save()
            VKApi.config = config

            getCurrentUser(id)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getCurrentUser(id: Int) {
        ThreadExecutor.execute(object : AsyncCallback(this) {

            var user: VKUser? = null

            @Throws(Exception::class)
            override fun ready() {
                val ids = ArrayList<Int>()
                ids.add(id)
                user = VKApi.users().get().userIds(ids).fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java)[0]

                val users = ArrayList<VKUser?>()
                users.add(user)

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)

                UserConfig.updateUser()
            }

            override fun done() {
                loggedIn = true
                setUserData(user)
                card!!.setOnClickListener(closeClick)
            }

            override fun error(e: Exception) {
                setUserData(null)
                Toast.makeText(this@LoginActivity, R.string.error, Toast.LENGTH_LONG).show()
            }

        })
    }

    private fun setUserData(user: VKUser?) {
        if (user == null) {
            loggedIn = false
            name!!.setText(R.string.add_account)
            avatar!!.setImageResource(R.drawable.placeholder_user)

            card!!.setOnClickListener(loginClick)
            card!!.setOnLongClickListener(null)
            return
        }

        loggedIn = true

        card!!.setOnLongClickListener(logoutClick)
        card!!.setOnClickListener(closeClick)

        name!!.text = user.toString()

        Picasso.get()
                .load(user.photo_200)
                .priority(Picasso.Priority.HIGH)
                .into(avatar!!, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        ViewUtils.fadeView(avatar)
                    }
                })
    }
}
