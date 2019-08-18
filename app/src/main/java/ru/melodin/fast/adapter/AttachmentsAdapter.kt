package ru.melodin.fast.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import ru.melodin.fast.R
import ru.melodin.fast.api.model.VKModel
import ru.melodin.fast.api.model.attachment.*
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.AttachmentInflater
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.CircleImageView
import java.util.*

class AttachmentsAdapter(context: Context, values: ArrayList<VKModel>) :
    RecyclerAdapter<VKModel, RecyclerHolder>(context, -1, values) {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).modelType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        return when (viewType) {
            VKModel.TYPE_PHOTO -> PhotoHolder(
                inflateView(
                    R.layout.activity_messages_attach_photo,
                    parent
                )
            )
            VKModel.TYPE_VIDEO -> VideoHolder(
                inflateView(
                    R.layout.activity_messages_attach_video,
                    parent
                )
            )
            VKModel.TYPE_AUDIO -> AudioHolder(
                inflateView(
                    R.layout.activity_messages_attach_audio,
                    parent
                )
            )
            VKModel.TYPE_DOC -> DocHolder(
                inflateView(
                    R.layout.activity_messages_attach_doc,
                    parent
                )
            )
            VKModel.TYPE_LINK -> LinkHolder(
                inflateView(
                    R.layout.activity_messages_attach_link,
                    parent
                )
            )
            else -> createEmptyHolder()
        }
    }

    inner class PhotoHolder(v: View) : RecyclerHolder(v) {

        override fun bind(position: Int) {
            super.bind(position)

            val photo = getItem(position) as VKPhoto

            Picasso.get().load(photo.maxSize).placeholder(ColorDrawable(Color.DKGRAY))
                .into(itemView as ImageView)
        }
    }

    inner class VideoHolder(v: View) : RecyclerHolder(v) {

        private val image: ImageView = v.findViewById(R.id.video_image)
        private val duration: TextView = v.findViewById(R.id.video_duration)

        override fun bind(position: Int) {
            super.bind(position)

            val item = getItem(position) as VKVideo

            val duration =
                String.format(
                    AppGlobal.locale,
                    "%d:%02d",
                    item.duration / 60,
                    item.duration % 60
                )

            this.duration.text = duration

            if (item.duration == 0) {
                this.duration.visibility = View.GONE
            } else {
                this.duration.visibility = View.VISIBLE
            }

            AttachmentInflater.loadImage(
                image,
                item.sizes[0]!!.src,
                if (item.sizes[1] != null) item.sizes[1]!!.src else null
            )
        }
    }

    inner class AudioHolder(v: View) : RecyclerHolder(v) {

        private val title: TextView = v.findViewById(R.id.title)
        private val body: TextView = v.findViewById(R.id.body)
        private val duration: TextView = v.findViewById(R.id.audio_duration)

        override fun bind(position: Int) {
            super.bind(position)

            itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

            val item = getItem(position) as VKAudio

            val duration =
                String.format(AppGlobal.locale, "%d:%02d", item.duration / 60, item.duration % 60)

            this.title.text = item.title
            this.body.text = item.artist
            this.duration.text = duration

        }
    }

    inner class DocHolder(v: View) : RecyclerHolder(v) {

        private val title: TextView = v.findViewById(R.id.title)
        private val size: TextView = v.findViewById(R.id.body)

        override fun bind(position: Int) {
            super.bind(position)

            itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

            val item = getItem(position) as VKDoc

            title.text = item.title

            val size =
                Util.parseSize(item.size.toLong()) + " • " + item.ext.toUpperCase(Locale.getDefault())

            this.size.text = size
        }

    }

    inner class LinkHolder(v: View) : RecyclerHolder(v) {

        private val title: TextView = v.findViewById(R.id.title)
        private val description: TextView = v.findViewById(R.id.description)
        private val icon: ImageView = v.findViewById(R.id.icon)
        private val photo: CircleImageView = v.findViewById(R.id.photo)

        override fun bind(position: Int) {
            super.bind(position)

            itemView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

            val item = getItem(position) as VKLink

            title.text = item.title

            val body =
                if (TextUtils.isEmpty(item.caption)) if (TextUtils.isEmpty(item.description)) "" else item.description else item.caption

            if (body.isEmpty()) {
                description.visibility = View.GONE
                description.text = ""
            } else {
                description.visibility = View.VISIBLE
                description.text = body
            }

            if (item.photo == null || TextUtils.isEmpty(item.photo!!.maxSize)) {
                photo.visibility = View.GONE
                icon.visibility = View.VISIBLE
            } else {
                photo.visibility = View.VISIBLE
                icon.visibility = View.GONE

                Picasso.get()
                    .load(item.photo!!.maxSize)
                    .into(photo)
            }
        }

    }


}
