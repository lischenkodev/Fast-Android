package ru.melodin.fast.common

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.greenrobot.eventbus.EventBus
import org.jetbrains.annotations.Contract
import ru.melodin.fast.PhotoViewActivity
import ru.melodin.fast.R
import ru.melodin.fast.adapter.MessageAdapter
import ru.melodin.fast.api.VKUtil
import ru.melodin.fast.api.model.*
import ru.melodin.fast.api.model.attachment.VKAudio
import ru.melodin.fast.api.model.attachment.VKDoc
import ru.melodin.fast.api.model.attachment.VKPhoto
import ru.melodin.fast.api.model.attachment.VKVideo
import ru.melodin.fast.database.MemoryCache
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.view.CircleImageView
import java.util.*
import kotlin.math.max
import kotlin.math.min

class AttachmentInflater(private val adapter: MessageAdapter?, private val context: Context) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val metrics: DisplayMetrics = context.resources.displayMetrics

    private val params: LinearLayout.LayoutParams
        get() = getParams(-1)

    fun showForwardedMessages(item: VKMessage, parent: ViewGroup, withStyles: Boolean) {
        for (i in 0 until item.fwdMessages.size) {
            message(item, parent, item.fwdMessages[i], false, withStyles)
        }
    }

    private fun inflateAttachments(
        item: VKMessage,
        parent: ViewGroup,
        images: ViewGroup,
        maxWidth: Int,
        forwarded: Boolean
    ) {
        val attachments = item.attachments
        for (i in attachments.indices) {
            when (val attachment = attachments[i]) {
                is VKAudio -> audio(item, parent, attachment)
                is VKPhoto -> photo(item, images, attachment, if (forwarded) maxWidth else -1)
                is VKSticker -> sticker(item, images, attachment)
                is VKDoc -> doc(item, parent, attachment)
                is VKLink -> link(item, parent, attachment)
                is VKVideo -> video(item, parent, attachment, if (forwarded) maxWidth else -1)
                is VKGraffiti -> graffiti(item, parent, attachment)
                is VKVoice -> voice(item, parent, attachment)
                is VKWall -> wall(item, parent, attachment)
                else -> empty(parent, context.getString(R.string.unknown))
            }
        }
    }

    private fun loadImage(
        image: ImageView,
        smallSrc: String?,
        normalSrc: String?,
        placeholder: Drawable? = null
    ) {
        if (TextUtils.isEmpty(smallSrc)) return
        try {
            val request = Picasso.get()
                .load(smallSrc)
                .priority(Picasso.Priority.HIGH)
                .placeholder(placeholder ?: ColorDrawable(0))

            if (!TextUtils.isEmpty(normalSrc)) {
                request.into(image, object : Callback.EmptyCallback() {
                    override fun onSuccess() {
                        if (TextUtils.isEmpty(normalSrc))
                            return

                        Picasso.get()
                            .load(normalSrc)
                            .placeholder(image.drawable)
                            .priority(Picasso.Priority.HIGH)
                            .into(image)
                    }
                })
            } else {
                request.into(image)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getHeight(layoutMaxWidth: Int): Int {
        val scale = max(320, layoutMaxWidth) / min(320, layoutMaxWidth)
        return (if (320 < layoutMaxWidth) 240 * scale else 240 / scale)
    }

    private fun getParams(width: Int, height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, height)
    }

    private fun getParams(layoutWidth: Int): LinearLayout.LayoutParams {
        return if (layoutWidth == -1)
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        else
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getHeight(layoutWidth)
            )
    }

    private fun getFrameParams(width: Int, height: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(width, height)
    }

    private fun getFrameParams(layoutWidth: Int): FrameLayout.LayoutParams {
        return if (layoutWidth == -1) {
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            getHeight(layoutWidth)
        )
    }

    fun empty(parent: ViewGroup, s: String) {
        val textView = TextView(context)

        val string = "[$s]"
        textView.text = string
        textView.textSize = 16f

        textView.isClickable = false
        textView.isFocusable = false

        parent.addView(textView)

    }

    fun wall(item: VKMessage, parent: ViewGroup, wall: VKWall) {
        val v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false)
        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        v.setOnClickListener { isSelected(item) }

        val title = v.findViewById<TextView>(R.id.abc_tb_title)
        val body = v.findViewById<TextView>(R.id.body)
        val icon = v.findViewById<ImageView>(R.id.icon)

        title.text = context.getString(R.string.wall_post)
        body.text = context.getString(R.string.link)

        title.maxWidth = metrics.widthPixels - metrics.widthPixels / 2
        body.maxWidth = metrics.widthPixels - metrics.widthPixels / 2

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_newspaper_black_24dp)
        icon.setImageDrawable(drawable)

        parent.addView(v)
    }

    fun graffiti(item: VKMessage, parent: ViewGroup, source: VKGraffiti) {
        val image =
            inflater.inflate(R.layout.activity_messages_attach_photo, parent, false) as ImageView
        image.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        image.setOnClickListener { isSelected(item) }

        image.layoutParams = params
        loadImage(image, source.url, null)

        image.isClickable = false
        image.isFocusable = false

        parent.addView(image)
    }

    fun sticker(item: VKMessage, parent: ViewGroup, source: VKSticker) {
        val image =
            inflater.inflate(R.layout.activity_messages_attach_photo, parent, false) as ImageView
        image.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        image.setOnClickListener { isSelected(item) }

        image.layoutParams = getParams(400, 400)
        loadImage(
            image,
            if (ThemeManager.IS_DARK) source.maxBackgroundSize else source.maxSize,
            null
        )

        image.isClickable = false
        image.isFocusable = false

        parent.addView(image)
    }

    fun service(item: VKMessage, parent: ViewGroup) {
        val text = inflater.inflate(R.layout.activity_messages_service, parent, false) as TextView

        text.text = Html.fromHtml(VKUtil.getActionBody(item, false))
        parent.addView(text)
    }

    fun video(item: VKMessage, parent: ViewGroup, source: VKVideo, maxWidth: Int) {
        val v = inflater.inflate(R.layout.activity_messages_attach_video, parent, false)
        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        v.setOnClickListener { isSelected(item) }

        val image = v.findViewById<ImageView>(R.id.image)
        val time = v.findViewById<TextView>(R.id.time)

        val duration =
            String.format(AppGlobal.locale, "%d:%02d", source.duration / 60, source.duration % 60)
        time.text = duration

        image.layoutParams = if (maxWidth == -1) getFrameParams(
            320,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ) else getFrameParams(maxWidth)

        loadImage(image, source.sizes[0]!!.src, if (source.sizes[1] != null) source.sizes[1]!!.src else null)
        parent.addView(v)
    }

    fun photo(item: VKMessage, parent: ViewGroup, source: VKPhoto, maxWidth: Int) {
        val image =
            inflater.inflate(R.layout.activity_messages_attach_photo, parent, false) as ImageView

        image.layoutParams = if (maxWidth == -1) getParams(
            source.maxWidth,
            source.maxHeight
        ) else getParams(maxWidth)
        image.setOnClickListener {
            if (isSelected(item)) return@setOnClickListener

            val intent = Intent(context, PhotoViewActivity::class.java)

            val photos = ArrayList<VKPhoto>()

            for (m in item.attachments) {
                if (m is VKPhoto) {
                    photos.add(m)
                }
            }

            intent.putExtra("selected", source)
            intent.putExtra("photo", photos)
            context.startActivity(intent)
        }

        image.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        loadImage(image, source.maxSize, null)
        parent.addView(image)
    }


    fun reply(item: VKMessage, parent: ViewGroup, withStyles: Boolean) {
        val v = message(item, null, item.reply!!.asMessage(), true, withStyles)
        v.setOnClickListener { adapter!!.getFragment().selectMessage(item.reply!!.asMessage()) }
        parent.addView(v)
    }

    fun message(
        item: VKMessage?,
        parent: ViewGroup?,
        source: VKMessage,
        isReply: Boolean,
        withStyles: Boolean
    ): View {
        val v = inflater.inflate(R.layout.activity_messages_attach_message, parent, false)
        v.isClickable = false
        v.isFocusable = false

        if (item != null) {
            v.setOnLongClickListener {
                simulateLongClick(item)
                true
            }

            v.setOnClickListener { isSelected(item) }
        }

        val name = v.findViewById<TextView>(R.id.userName)
        val message = v.findViewById<TextView>(R.id.userMessage)
        val avatar = v.findViewById<ImageView>(R.id.userAvatar)
        val line = v.findViewById<View>(R.id.message_line)

        message.isSingleLine = isReply

        var user = MemoryCache.getUser(source.fromId)
        if (user == null) {
            user = VKUser.EMPTY
        }

        if (TextUtils.isEmpty(user.photo100) || isReply) {
            avatar.visibility = View.GONE
        } else {
            avatar.visibility = View.VISIBLE
            Picasso.get()
                .load(user.photo100)
                .priority(Picasso.Priority.HIGH)
                .placeholder(R.drawable.avatar_placeholder)
                .into(avatar)
        }

        line.setBackgroundColor(if (withStyles) ThemeManager.ACCENT else Color.TRANSPARENT)

        @ColorInt val lineColor = if (item != null) ThemeManager.ACCENT else Color.TRANSPARENT
        line.setBackgroundColor(lineColor)

        name.maxWidth = metrics.widthPixels - metrics.widthPixels / 3
        message.maxWidth = metrics.widthPixels - metrics.widthPixels / 3

        name.text = user.toString()

        if (TextUtils.isEmpty(source.text)) {
            message.visibility = View.GONE
        } else {
            message.text = source.text
        }

        if (!ArrayUtil.isEmpty(source.attachments) && !isReply) {
            val container = v.findViewById<LinearLayout>(R.id.attachments)
            inflateAttachments(source, container, container, -1, true)
        }

        if (!ArrayUtil.isEmpty(source.fwdMessages) && !isReply) {
            val container = v.findViewById<LinearLayout>(R.id.forwarded)
            showForwardedMessages(source, container, withStyles)
        }

        parent?.addView(v)
        return v
    }

    @SuppressLint("DefaultLocale")
    fun doc(item: VKMessage, parent: ViewGroup, source: VKDoc) {
        val v = inflater.inflate(R.layout.activity_messages_attach_doc, parent, false)
        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        val title = v.findViewById<TextView>(R.id.abc_tb_title)
        val size = v.findViewById<TextView>(R.id.body)

        title.text = source.title

        val s = Util.parseSize(source.size.toLong()) + " • " + source.ext.toUpperCase()
        size.text = s

        title.maxWidth = metrics.widthPixels - metrics.widthPixels / 2
        size.maxWidth = metrics.widthPixels - metrics.widthPixels / 2

        parent.addView(v)
        v.setOnClickListener {
            if (isSelected(item)) return@setOnClickListener

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun voice(item: VKMessage, parent: ViewGroup, source: VKVoice) {
        val v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false)
        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        v.setOnClickListener { isSelected(item) }

        val title = v.findViewById<TextView>(R.id.abc_tb_title)
        val body = v.findViewById<TextView>(R.id.body)
        val time = v.findViewById<TextView>(R.id.duration)

        val play = v.findViewById<ImageButton>(R.id.play)

        val duration =
            String.format(AppGlobal.locale, "%d:%02d", source.duration / 60, source.duration % 60)
        title.text = context.getString(R.string.voice_message)
        body.text = duration

        val start = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_black_24dp)
        val stop = ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_black_24dp)

        val playing = item.isPlaying
        play.setImageDrawable(if (playing) stop else start)

        play.setOnClickListener {
            EventBus.getDefault().postSticky(
                arrayOf<Any>(
                    if (playing) KEY_PAUSE_AUDIO else KEY_PLAY_AUDIO,
                    item.id,
                    if (!playing) source.linkMp3 else ""
                )
            )
        }

        title.maxWidth = metrics.widthPixels / 2
        body.maxWidth = metrics.widthPixels / 2
        time.maxWidth = metrics.widthPixels / 2

        parent.addView(v)
    }

    fun audio(item: VKMessage, parent: ViewGroup, source: VKAudio) {
        val v = inflater.inflate(R.layout.activity_messages_attach_audio, parent, false)

        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        v.setOnClickListener { isSelected(item) }

        val title = v.findViewById<TextView>(R.id.abc_tb_title)
        val body = v.findViewById<TextView>(R.id.body)
        val time = v.findViewById<TextView>(R.id.duration)

        val play = v.findViewById<ImageButton>(R.id.play)
        val start = ContextCompat.getDrawable(context, R.drawable.ic_play_circle_filled_black_24dp)
        val stop = ContextCompat.getDrawable(context, R.drawable.ic_pause_circle_filled_black_24dp)

        val playing = item.isPlaying
        play.setImageDrawable(if (playing) stop else start)

        play.setOnClickListener {
            EventBus.getDefault().postSticky(
                arrayOf<Any>(
                    if (playing) KEY_PAUSE_AUDIO else KEY_PLAY_AUDIO,
                    item.id,
                    if (!playing) source.url else ""
                )
            )
        }

        val duration =
            String.format(AppGlobal.locale, "%d:%02d", source.duration / 60, source.duration % 60)
        title.text = source.title
        body.text = source.artist
        time.text = duration

        title.maxWidth = metrics.widthPixels / 2
        body.maxWidth = metrics.widthPixels / 2
        time.maxWidth = metrics.widthPixels / 2

        parent.addView(v)
    }

    fun link(item: VKMessage, parent: ViewGroup, source: VKLink) {
        val v = inflater.inflate(R.layout.activity_messages_attach_link, parent, false)
        v.setOnLongClickListener {
            simulateLongClick(item)
            true
        }

        val title = v.findViewById<TextView>(R.id.abc_tb_title)
        val description = v.findViewById<TextView>(R.id.description)
        val icon = v.findViewById<ImageView>(R.id.icon)
        val photo = v.findViewById<CircleImageView>(R.id.photo)

        title.text = source.title

        val body =
            if (TextUtils.isEmpty(source.caption)) if (TextUtils.isEmpty(source.description)) "" else source.description else source.caption

        if (body.isEmpty()) {
            description.visibility = View.GONE
            description.text = ""
        } else {
            description.visibility = View.VISIBLE
            description.text = body
        }

        if (source.photo == null || TextUtils.isEmpty(source.photo!!.maxSize)) {
            photo.visibility = View.GONE
            icon.visibility = View.VISIBLE
        } else {
            photo.visibility = View.VISIBLE
            icon.visibility = View.GONE

            Picasso.get()
                .load(source.photo!!.maxSize)
                .into(photo)
        }

        title.maxWidth = metrics.widthPixels - metrics.widthPixels / 2
        description.maxWidth = metrics.widthPixels - metrics.widthPixels / 2

        parent.addView(v)

        v.setOnClickListener {
            if (isSelected(item)) return@setOnClickListener

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage("com.android.chrome")
            try {
                context.startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                intent.setPackage(null)
                context.startActivity(intent)
            }
        }
    }

    @Contract(pure = true)
    private fun isSelected(item: VKMessage): Boolean {
        if (adapter == null) return false
        if (adapter.isSelected) {
            simulateClick(item)
            return true
        }

        return false
    }

    private fun simulateClick(item: VKMessage) {
        if (adapter == null) return

        val position = adapter.searchPosition(item.id)
        if (position == -1) return

        adapter.getFragment().onItemClick(position)
    }

    private fun simulateLongClick(item: VKMessage?) {
        if (adapter == null) return

        val position = adapter.searchPosition(item!!.id)
        if (position == -1) return

        adapter.getFragment().onItemLongClick(position)
    }

    companion object {

        const val KEY_PLAY_AUDIO = "play_audio"
        const val KEY_PAUSE_AUDIO = "pause_audio"
        const val KEY_STOP_AUDIO = "stop_audio"

        @Synchronized
        fun getInstance(adapter: MessageAdapter?, context: Context): AttachmentInflater {
            return AttachmentInflater(adapter, context)
        }
    }
}