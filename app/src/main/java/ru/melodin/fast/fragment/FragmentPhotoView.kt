package ru.melodin.fast.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import ru.melodin.fast.PhotoViewActivity
import ru.melodin.fast.api.model.VKPhoto
import ru.melodin.fast.util.Util

class FragmentPhotoView : Fragment() {

    private var photo: VKPhoto? = null

    val url: String
        get() = photo!!.maxSize

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photo = arguments!!.getSerializable("photo") as VKPhoto
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val image = ImageView(context)
        image.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        image.adjustViewBounds = true
        return image
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val maxSize = photo!!.maxSize

        if (!TextUtils.isEmpty(maxSize) && Util.hasConnection()) {
            loadImage(maxSize)
        }

        getView()!!.setOnClickListener { (activity as PhotoViewActivity).changeTbVisibility() }
    }

    private fun loadImage(url: String) {
        try {
            Picasso.get().load(url).placeholder(ColorDrawable(Color.GRAY)).into(view as ImageView?)
        } catch (ignored: Exception) {
        }

    }

    companion object {

        fun newInstance(photo: VKPhoto): FragmentPhotoView {
            val fragment = FragmentPhotoView()
            val b = Bundle()
            b.putSerializable("photo", photo)
            fragment.arguments = b
            return fragment
        }
    }
}
