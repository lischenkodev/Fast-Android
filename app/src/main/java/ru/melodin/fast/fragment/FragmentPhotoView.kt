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
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import org.jetbrains.annotations.Contract
import ru.melodin.fast.BuildConfig
import ru.melodin.fast.PhotoViewActivity
import ru.melodin.fast.R
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.util.Util
import kotlin.math.abs

class FragmentPhotoView(private var photo: VKPhoto?) : Fragment() {

    constructor() : this(null)

    var url: String? = null
        private set

    private var yDelta: Int = 0

    private val onTouchListener: View.OnTouchListener
        @Contract(pure = true)
        @SuppressLint("ClickableViewAccessibility")
        get() = View.OnTouchListener { view, event ->
            val y = event.rawY.toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val lParams = view.layoutParams as CoordinatorLayout.LayoutParams
                    yDelta = y - lParams.topMargin

                    getView()!!.invalidate()
                    true
                }

                MotionEvent.ACTION_UP -> {
                    var top = view.top
                    if (top < 0) top *= -1

                    val max = resources.displayMetrics.heightPixels / 6

                    if (BuildConfig.DEBUG) {
                        val swipeInfo = " \ntop: " + top + "\nheight: " + resources.displayMetrics.heightPixels + "\nmax: " + max
                        Log.d("swipeInfo", swipeInfo)
                    }

                    val params = view.layoutParams as CoordinatorLayout.LayoutParams
                    params.topMargin = 0
                    view.layoutParams = params

                    if (top > max) {
                        if (view.top > 0) {
                            params.topMargin = view.height * 2
                        } else {
                            params.topMargin = abs(view.height * 2)
                        }

                        activity!!.finish()
                        activity!!.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        return@OnTouchListener true
                    } else {
                        (activity as PhotoViewActivity).changeTbVisibility()
                    }

                    getView()!!.invalidate()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val layoutParams = view.layoutParams as CoordinatorLayout.LayoutParams
                    layoutParams.topMargin = y - yDelta
                    view.layoutParams = layoutParams

                    getView()!!.invalidate()
                    true
                }
                else -> false
            }
        }

    override fun getView(): CoordinatorLayout? {
        return super.getView() as CoordinatorLayout?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = CoordinatorLayout(context!!)
        layout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layout.fitsSystemWindows = true

        val image = ImageView(context)
        image.layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT)
        image.adjustViewBounds = true
        image.fitsSystemWindows = true

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

        getView()!!.getChildAt(0).setOnTouchListener(onTouchListener)
    }

    private fun loadPhoto(url: String?) {
        try {
            Picasso.get().load(url).placeholder(ColorDrawable(Color.GRAY)).into(view!!.getChildAt(0) as ImageView)
        } catch (e: Exception) {
            Log.e("Error load photo", Log.getStackTraceString(e))
        }

    }
}
