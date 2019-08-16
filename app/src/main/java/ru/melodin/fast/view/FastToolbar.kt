package ru.melodin.fast.view

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import ru.melodin.fast.R


class FastToolbar : FrameLayout {

    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var back: ImageButton
    private lateinit var menuLayout: LinearLayout
    private lateinit var avatarSpace: Space
    private lateinit var menuView: ActionMenuView

    private lateinit var menuInflater: SupportMenuInflater

    lateinit var avatar: ImageView

    lateinit var menu: Menu

    private var onMenuItemClickListener: OnMenuItemClickListener? = null

    @ColorInt
    private var colorPrimary: Int = 0

    private val onMenuViewItemClickListener = ActionMenuView.OnMenuItemClickListener {
        if (onMenuItemClickListener != null) {
            return@OnMenuItemClickListener onMenuItemClickListener!!.onMenuItemClick(it)
        }

        return@OnMenuItemClickListener false
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        initMenuInflater()
        initPrimaryColor()

        LayoutInflater.from(context).inflate(R.layout.abc_toolbar, this)

        setBackgroundColor(colorPrimary)

        initViews()
        validateAvatarVisibility()

        menu = menuView.menu

        (menuView.menu as MenuBuilder).setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(m: MenuBuilder?, item: MenuItem?): Boolean {
                if (onMenuItemClickListener != null) {
                    return onMenuItemClickListener!!.onMenuItemClick(item!!)
                }

                return false
            }

            override fun onMenuModeChange(menuBuilder: MenuBuilder) {}
        })

        menuView.setOnMenuItemClickListener(onMenuViewItemClickListener)

        validateVisibility()
    }

    private fun initMenuInflater() {
        menuInflater = SupportMenuInflater(context)
    }

    private fun initPrimaryColor() {
        colorPrimary = getAttrColor(R.attr.colorPrimary)
    }

    private fun initViews() {
        avatarSpace = findViewById(R.id.abc_tb_avatar_space)
        menuView = findViewById(R.id.menuView)
        title = findViewById(R.id.abc_tb_title)
        subtitle = findViewById(R.id.abc_tb_subtitle)
        avatar = findViewById(R.id.abc_user_avatar)
        back = findViewById(R.id.abc_tb_back)
        menuLayout = findViewById(R.id.abc_tb_menu)
    }

    private fun validateVisibility() {
        val title = this.title.text.toString().trim()
        val subtitle = this.subtitle.text.toString().trim()

        this.title.visibility = if (title.isEmpty()) View.GONE else View.VISIBLE
        this.subtitle.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE
    }

    fun inflateMenu(@MenuRes resId: Int) {
        menuInflater.inflate(resId, menuView.menu as MenuBuilder)
    }

    fun setBackIcon(icon: Drawable) {
        back.setImageDrawable(icon)
    }

    fun setBackVisible(visible: Boolean) {
        back.visibility = if (visible) View.VISIBLE else View.GONE

        if (visible && context is Activity) {
            back.setOnClickListener { (context as Activity).onBackPressed() }
        } else {
            back.setOnClickListener(null)
        }
    }

    private fun validateAvatarVisibility() {
        if (avatar.visibility == View.VISIBLE) {
            avatarSpace.visibility = View.GONE
        } else if (avatar.visibility == View.GONE) {
            avatarSpace.visibility = View.VISIBLE
        }
    }

    fun setAvatar(drawable: Drawable?) {
        avatar.setImageDrawable(drawable)
        avatar.visibility = if (drawable == null) View.GONE else View.VISIBLE
        validateAvatarVisibility()
    }

    fun setAvatar(resId: Int) {
        setAvatar(ContextCompat.getDrawable(context, resId))
    }

    fun setOnAvatarClickListener(listener: () -> Unit) {
        avatar.setOnClickListener { listener() }
    }

    fun setSubtitle(title: CharSequence?) {
        this.subtitle.text = title.toString().trim()
        validateVisibility()
    }

    fun setSubtitle(resId: Int) {
        val text = context.getString(resId)
        this.subtitle.text = text.trim()
        validateVisibility()
    }

    fun setTitle(title: CharSequence?) {
        this.title.text = title.toString().trim()
        validateVisibility()
    }

    fun setTitle(resId: Int) {
        val text = context.getString(resId)
        this.title.text = text.trim()
        validateVisibility()
    }

    private fun getAttrColor(@AttrRes resId: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }

    fun setOnMenuItemClickListener(listener: OnMenuItemClickListener) {

        this.onMenuItemClickListener = listener
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(item: MenuItem): Boolean
    }
}
