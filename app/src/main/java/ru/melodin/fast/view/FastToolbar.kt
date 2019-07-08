package ru.melodin.fast.view

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import androidx.core.content.ContextCompat
import ru.melodin.fast.R

class FastToolbar : FrameLayout {

    private var title: TextView? = null
    private var subtitle: TextView? = null

    lateinit var avatar: ImageView
        private set

    private var back: ImageButton? = null

    private var menuLayout: LinearLayout? = null

    var menu: Menu? = null
        private set

    private var onMenuItemClickListener: OnMenuItemClickListener? = null

    @ColorInt
    private var colorPrimary: Int = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        menu = PopupMenu(context, null).menu
        colorPrimary = getAttrColor(R.attr.colorPrimary)

        LayoutInflater.from(context).inflate(R.layout.abc_toolbar, this)

        setBackgroundColor(colorPrimary)

        title = findViewById(R.id.abc_tb_title)
        subtitle = findViewById(R.id.abc_tb_subtitle)
        avatar = findViewById(R.id.abc_user_avatar)
        back = findViewById(R.id.abc_tb_back)
        menuLayout = findViewById(R.id.abc_tb_menu)

        validateVisibility()
        initListener()
    }

    private fun initListener() {
        if (onMenuItemClickListener == null || menu!!.size() == 0) return

        for (i in 0 until menu!!.size()) {
            menuLayout!!.getChildAt(i).setOnClickListener { onMenuItemClickListener!!.onMenuItemClick(menu!!.getItem(i)) }
        }
    }

    private fun validateVisibility() {
        val title = this.title!!.text.toString().trim { it <= ' ' }
        val subtitle = this.subtitle!!.text.toString().trim { it <= ' ' }

        this.title!!.visibility = if (title.isEmpty()) View.GONE else View.VISIBLE
        this.subtitle!!.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE
    }

    fun inflateMenu(@MenuRes resId: Int) {
        val inflater = MenuInflater(context)
        inflater.inflate(resId, menu)
        initMenu()
    }

    private fun initMenu() {
        if (menu!!.size() > 2) {
            for (i in 2 until menu!!.size()) {
                menu!!.removeItem(i)
            }
        }

        for (i in 0 until menu!!.size()) {
            addMenuItem(i)
        }
    }

    fun setBackIcon(icon: Drawable) {
        back!!.setImageDrawable(icon)
    }

    fun setBackVisible(visible: Boolean) {
        back!!.visibility = if (visible) View.VISIBLE else View.GONE

        if (visible && context is Activity) {
            back!!.setOnClickListener { (context as Activity).onBackPressed() }
        } else {
            back!!.setOnClickListener(null)
        }
    }

    fun setOnBackClickListener(listener: OnClickListener) {
        back!!.setOnClickListener(listener)
    }

    private fun addMenuItem(i: Int) {
        val menuButton = menuLayout!!.getChildAt(i) as ImageButton
        menuButton.visibility = View.VISIBLE
        menuButton.setImageDrawable(menu!!.getItem(i).icon)
    }

    fun addMenuItem(item: MenuItem) {
        if (menu!!.size() == 2) return

        val menuButton = menuLayout!!.getChildAt(if (menu!!.size() == 0) 0 else menu!!.size() - 1) as ImageButton
        menuButton.visibility = View.VISIBLE
        menuButton.setImageDrawable(item.icon)
    }

    fun setItemVisible(i: Int, visible: Boolean) {
        if (i > 1) return
        menuLayout!!.getChildAt(i).visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setAvatar(drawable: Drawable?) {
        avatar!!.setImageDrawable(drawable)
        avatar!!.visibility = if (drawable == null) View.GONE else View.VISIBLE
    }

    fun setAvatar(resId: Int) {
        setAvatar(ContextCompat.getDrawable(context, resId))
    }

    fun setAvatar(bitmap: Bitmap?) {
        avatar!!.setImageBitmap(bitmap)
        avatar!!.visibility = if (bitmap == null) View.GONE else View.VISIBLE
    }

    fun setOnAvatarClickListener(listener: () -> Unit) {
        avatar!!.setOnClickListener { listener() }
    }

    fun getSubtitle(): String {
        return subtitle!!.text.toString()
    }

    fun setSubtitle(title: CharSequence?) {
        if (title == null || title.toString().trim { it <= ' ' }.isEmpty()) return
        this.subtitle!!.text = title.toString().trim { it <= ' ' }
        validateVisibility()
    }

    fun setSubtitle(resId: Int) {
        val text = context.getString(resId)
        this.subtitle!!.text = text.trim { it <= ' ' }
        validateVisibility()
    }

    fun getTitle(): String {
        return title!!.text.toString()
    }

    fun setTitle(title: CharSequence?) {
        if (title == null || title.toString().trim { it <= ' ' }.isEmpty()) return
        this.title!!.text = title.toString().trim { it <= ' ' }
        validateVisibility()
    }

    fun setTitle(resId: Int) {
        val text = context.getString(resId)
        this.title!!.text = text.trim { it <= ' ' }
        validateVisibility()
    }

    @ColorInt
    private fun getAttrColor(@AttrRes resId: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }

    fun setOnMenuItemClickListener(onMenuItemClickListener: OnMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener
        initListener()
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(item: MenuItem)
    }
}
