package ru.melodin.fast.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment

import com.squareup.picasso.Picasso

import org.jetbrains.annotations.Contract

import ru.melodin.fast.BuildConfig
import ru.melodin.fast.PhotoViewActivity
import ru.melodin.fast.R
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.util.Util

class FragmentPhotoView : Fragment() {

    private var photo: VKPhoto? = null

    var url: String? = null
        private set

    private var yDelta: Int = 0

    private val onTouchListener: View.OnTouchListener
        @Contract(pure = true)
        @SuppressLint("ClickableViewAccessibility")
        get() = View.OnTouchListener { view, event ->
            val y = event.rawY.toInt()

            when (event.action and MotionEvent.ACTION_MASK) {

                MotionEvent.ACTION_DOWN -> {
                    val lParams = view.layoutParams as FrameLayout.LayoutParams
                    yDelta = y - lParams.topMargin
                }

                MotionEvent.ACTION_UP -> {
                    var top = view.top
                    if (top < 0) top *= -1

                    val max = resources.displayMetrics.heightPixels / 6

                    if (BuildConfig.DEBUG) {
                        val swipeInfo = " \ntop: " + top + "\nheight: " + resources.displayMetrics.heightPixels + "\nmax: " + max
                        Log.d("swipeInfo", swipeInfo)
                    }

                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.topMargin = 0
                    view.layoutParams = params

                    if (top > max) {
                        if (view.top > 0) {
                            params.topMargin = view.height * 2
                        } else {
                            params.topMargin = Math.abs(view.height * 2)
                        }

                        activity!!.finish()
                        activity!!.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        return@OnTouchListener true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val layoutParams = view.layoutParams as FrameLayout.LayoutParams
                    layoutParams.topMargin = y - yDelta
                    view.layoutParams = layoutParams
                }
            }

            getView()!!.invalidate()
            true
        }

    override fun getView(): FrameLayout? {
        return super.getView() as FrameLayout?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = FrameLayout(context!!)
        layout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layout.fitsSystemWindows = true

        val image = ImageView(context)
        image.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        image.adjustViewBounds = true

        layout.addView(image)
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (photo == null) return
        val maxSize = photo!!.maxSize
        url = maxSize

        if (TextUtils.isEmpty(maxSize) || !Util.hasConnection()) return

        loadPhoto(maxSize)

        view.setOnClickListener { (activity as PhotoViewActivity).changeTbVisibility() }
        getView()!!.getChildAt(0).setOnTouchListener(onTouchListener)
    }

    private fun loadPhoto(url: String?) {
        try {
            Picasso.get().load(url).placeholder(ColorDrawable(Color.GRAY)).into(view!!.getChildAt(0) as ImageView)
        } catch (e: Exception) {
            Log.e("Error load photo", Log.getStackTraceString(e))
        }

    }

    companion object {

        fun newInstance(photo: VKPhoto): FragmentPhotoView {
            val fragment = FragmentPhotoView()
            fragment.photo = photo
            return fragment
        }
    }

}
