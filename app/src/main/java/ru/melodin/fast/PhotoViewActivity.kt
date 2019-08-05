package ru.melodin.fast

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_photo_view.*
import ru.melodin.fast.adapter.PhotoViewAdapter
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.fragment.FragmentPhotoView
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import java.util.*

class PhotoViewActivity : AppCompatActivity() {

    private var animState = AnimState.SHOWED
    private var likeState = LikeState.UNLIKED

    private var adapter: PhotoViewAdapter? = null
    private var source: VKPhoto? = null

    private val url: String
        get() {
            if (pager == null) return ""
            if (pager.adapter == null) return ""

            val position = pager.currentItem

            val fragments = adapter!!.fragments
            return (fragments!![position] as FragmentPhotoView).url!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        window.statusBarColor = Color.TRANSPARENT

        changeTbVisibility()

        val photos = intent.getSerializableExtra("photo") as ArrayList<VKPhoto>
        source = intent.getSerializableExtra("selected") as VKPhoto

        if (ArrayUtil.isEmpty(photos)) {
            finish()
            return
        }

        var selectedPosition = 0

        if (source != null)
            for (i in photos.indices) {
                val photo = photos[i]
                if (photo.id == source!!.id)
                    selectedPosition = i
            }

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                setTitle(position + 1, photos.size)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        if (Util.hasConnection()) {
            createAdapter(photos)
        }

        if (selectedPosition > 0)
            pager.currentItem = selectedPosition

        setTitle(selectedPosition + 1, photos.size)

        like.setOnClickListener {
            like.drawable.setTint(if (likeState == LikeState.UNLIKED) Color.RED else Color.WHITE)
            likeState = if (likeState == LikeState.UNLIKED) LikeState.LIKED else LikeState.UNLIKED
        }
    }

    fun changeTbVisibility() {
        if (animState == AnimState.SHOWED) {
            animState = AnimState.HIDDEN
            toolbar.animate().alpha(0f).setDuration(300).withEndAction {
                if (toolbar.visibility != View.INVISIBLE) {
                    toolbar.visibility = View.INVISIBLE
                }

                if (items.visibility != View.INVISIBLE) {
                    items.visibility = View.INVISIBLE
                }
            }.start()
            items.animate().alpha(0f).setDuration(300).start()
        } else {
            animState = AnimState.SHOWED
            toolbar.animate().alpha(1f).setDuration(300).withStartAction {
                if (toolbar.visibility != View.VISIBLE) {
                    toolbar.visibility = View.VISIBLE
                }

                if (items.visibility != View.VISIBLE) {
                    items.visibility = View.VISIBLE
                }
            }.start()
            items.animate().alpha(1f).setDuration(300).start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS) {
            savePhoto()
        }
    }

    private fun savePhoto() {
        TaskManager.execute {
            try {
                val path = Util.saveFileByUrl(url)
                runOnUiThread {
                    Toast.makeText(
                        this@PhotoViewActivity,
                        getString(R.string.saved_into, path),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
            }
        }
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
        pager.offscreenPageLimit = if (photos.size == 1) 1 else photos.size - 1

        adapter = PhotoViewAdapter(supportFragmentManager, photos)
        pager.adapter = adapter
    }

    private fun setTitle(current: Int, max: Int) {
        supportActionBar!!.title = getString(R.string.photo_of_photo, current, max)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.save -> savePhoto()
            R.id.copy_link -> copyUrl()
        }
        return super.onOptionsItemSelected(item)
    }

    private enum class LikeState {
        LIKED, UNLIKED
    }

    private enum class AnimState {
        SHOWED, HIDDEN
    }

    companion object {

        private const val REQUEST_PERMISSIONS = 1
    }
}
