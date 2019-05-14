package ru.stwtforever.fast

import android.Manifest
import android.animation.Animator
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import org.greenrobot.eventbus.EventBus
import ru.stwtforever.fast.adapter.PhotoViewAdapter
import ru.stwtforever.fast.api.model.VKPhoto
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.fragment.FragmentPhotoView
import ru.stwtforever.fast.common.PermissionManager
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Util
import java.util.*

class PhotoViewActivity : AppCompatActivity() {

    private var tb: Toolbar? = null
    private var items: LinearLayout? = null
    private var pager: ViewPager? = null

    private var state = 0
    private var like_state = 0

    private var adapter: PhotoViewAdapter? = null

    private var like: ImageButton? = null
    private var comment: ImageButton? = null
    private var repost: ImageButton? = null

    private val goneListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            if (tb!!.visibility != View.INVISIBLE) {
                tb!!.visibility = View.INVISIBLE
            }

            if (items!!.visibility != View.INVISIBLE) {
                items!!.visibility = View.INVISIBLE
            }
        }

        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {

        }
    }

    private val showListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {
            if (tb!!.visibility != View.VISIBLE) {
                tb!!.visibility = View.VISIBLE
            }

            if (items!!.visibility != View.VISIBLE) {
                items!!.visibility = View.VISIBLE
            }
        }

        override fun onAnimationEnd(animation: Animator) {}
        override fun onAnimationCancel(animation: Animator) {

        }

        override fun onAnimationRepeat(animation: Animator) {

        }
    }

    private val url: String?
        get() {
            if (pager == null) return null
            if (pager!!.adapter == null) return null

            val selected_position = pager!!.currentItem

            val fragments = (pager!!.adapter as PhotoViewAdapter).fragments

            return (fragments[selected_position] as FragmentPhotoView).url
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        PermissionManager.setActivity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        tb = findViewById(R.id.toolbar)
        items = findViewById(R.id.items)
        like = findViewById(R.id.like)
        comment = findViewById(R.id.comment)
        repost = findViewById(R.id.repost)

        setSupportActionBar(tb)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val photos = intent.getSerializableExtra("photo") as ArrayList<VKPhoto>

        supportActionBar!!.title = getString(R.string.photo_of_photo, "1", photos.size.toString())

        if (!ArrayUtil.isEmpty(photos) && Util.hasConnection()) {
            createAdapter(photos)
        }

        like!!.setOnClickListener {
            like!!.drawable.setTint(if (like_state == 0) Color.RED else Color.WHITE)
            like_state = if (like_state == 0) 1 else 0
        }
    }

    fun changeTbVisibility() {
        if (state == 0) {
            state = 1
            tb!!.animate().alpha(0f).setDuration(300).setListener(goneListener).start()
            items!!.animate().alpha(0f).setDuration(300).start()
        } else {
            state = 0
            tb!!.animate().alpha(1f).setDuration(300).setListener(showListener).start()
            items!!.animate().alpha(1f).setDuration(300).start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.save -> checkPermissions()
            R.id.copy_link -> copyUrl()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun checkPermissions() {
        if (PermissionManager.isGrantedPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            savePhoto()
        } else {
            PermissionManager.requestPermissions(23, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 23) {
            if (PermissionManager.isGranted(grantResults[0])) {
                savePhoto()
            } else {
                Toast.makeText(this, R.string.fast_rqrs_prmsn_for_save_photo, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePhoto() {
        ThreadExecutor.execute(object : AsyncCallback(this) {
            @Throws(Exception::class)
            override fun ready() {
                Util.saveFileByUrl(url)
            }

            override fun done() {
                Toast.makeText(this@PhotoViewActivity, R.string.photo_saved_in_downloads_directory, Toast.LENGTH_LONG).show()
            }

            override fun error(e: Exception) {
                Toast.makeText(this@PhotoViewActivity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun copyUrl() {
        val url = url
        Util.copyText(url)
        Toast.makeText(this, R.string.url_copied_text, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_photo_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun createAdapter(photos: ArrayList<VKPhoto>) {
        pager = findViewById(R.id.pager)

        pager!!.offscreenPageLimit = 10

        adapter = PhotoViewAdapter(supportFragmentManager, photos)
        pager!!.adapter = adapter
        pager!!.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = getString(R.string.photo_of_photo, (position + 1).toString(), photos.size.toString())
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(arrayOf<Any>(-1))
        super.onDestroy()
    }
}
