package ru.melodin.fast.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ru.melodin.fast.R;

public class FastToolbar extends FrameLayout {

    private TextView title;
    private TextView subtitle;

    private ImageView avatar;

    private ImageButton back;

    private LinearLayout menuLayout;

    private Menu menu;

    private OnMenuItemClickListener onMenuItemClickListener;

    @ColorInt
    private int colorPrimary;

    public FastToolbar(Context context) {
        super(context);
        init();
    }

    public FastToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FastToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        menu = new PopupMenu(getContext(), null).getMenu();
        colorPrimary = getAttrColor(R.attr.colorPrimary);

        LayoutInflater.from(getContext()).inflate(R.layout.abc_toolbar, this);

        setBackgroundColor(colorPrimary);

        title = findViewById(R.id.abc_tb_title);
        subtitle = findViewById(R.id.abc_tb_subtitle);
        avatar = findViewById(R.id.abc_user_avatar);
        back = findViewById(R.id.abc_tb_back);
        menuLayout = findViewById(R.id.abc_tb_menu);

        validateVisibility();
        initListener();
    }

    private void initListener() {
        if (onMenuItemClickListener == null || menu.size() == 0) return;

        for (int i = 0; i < menu.size(); i++) {
            final int finalI = i;
            menuLayout.getChildAt(i).setOnClickListener(view -> onMenuItemClickListener.onMenuItemClick(menu.getItem(finalI)));
        }
    }

    private void validateVisibility() {
        String title = this.title.getText().toString().trim();
        String subtitle = this.subtitle.getText().toString().trim();

        this.title.setVisibility(title.isEmpty() ? GONE : VISIBLE);
        this.subtitle.setVisibility(subtitle.isEmpty() ? GONE : VISIBLE);
    }

    public void inflateMenu(@MenuRes int resId) {
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(resId, menu);
        initMenu();
    }

    private void initMenu() {
        if (menu.size() > 2) {
            for (int i = 2; i < menu.size(); i++) {
                menu.removeItem(i);
            }
        }

        for (int i = 0; i < menu.size(); i++) {
            addMenuItem(i);
        }
    }

    public void setBackIcon(Drawable icon) {
        back.setImageDrawable(icon);
    }

    public void setBackVisible(boolean visible) {
        back.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnBackClickListener(View.OnClickListener listener) {
        back.setOnClickListener(listener);
    }

    private void addMenuItem(int i) {
        ImageButton menuButton = (ImageButton) menuLayout.getChildAt(i);
        menuButton.setVisibility(VISIBLE);
        menuButton.setImageDrawable(menu.getItem(i).getIcon());
    }

    public void addMenuItem(MenuItem item) {
        if (menu.size() == 2) return;

        ImageButton menuButton = (ImageButton) menuLayout.getChildAt(menu.size() == 0 ? 0 : menu.size() - 1);
        menuButton.setVisibility(VISIBLE);
        menuButton.setImageDrawable(item.getIcon());
    }

    public void setItemVisible(int i, boolean visible) {
        if (i > 1) return;
        menuLayout.getChildAt(i).setVisibility(visible ? VISIBLE : GONE);
    }

    public Menu getMenu() {
        return menu;
    }

    public void setAvatar(Drawable drawable) {
        avatar.setImageDrawable(drawable);
        avatar.setVisibility(drawable == null ? GONE : VISIBLE);
    }

    public void setAvatar(int resId) {
        setAvatar(ContextCompat.getDrawable(getContext(), resId));
    }

    public void setAvatar(Bitmap bitmap) {
        avatar.setImageBitmap(bitmap);
        avatar.setVisibility(bitmap == null ? GONE : VISIBLE);
    }

    public ImageView getAvatar() {
        return avatar;
    }

    public String getSubtitle() {
        return subtitle.getText().toString();
    }

    public void setSubtitle(CharSequence title) {
        if (title == null || title.toString().trim().isEmpty()) return;
        this.subtitle.setText(title.toString().trim());
        validateVisibility();
    }

    public void setSubtitle(int resId) {
        String text = getContext().getString(resId);
        this.subtitle.setText(text.trim());
        validateVisibility();
    }

    public String getTitle() {
        return title.getText().toString();
    }

    public void setTitle(CharSequence title) {
        if (title == null || title.toString().trim().isEmpty()) return;
        this.title.setText(title.toString().trim());
        validateVisibility();
    }

    public void setTitle(int resId) {
        String text = getContext().getString(resId);
        this.title.setText(text.trim());
        validateVisibility();
    }

    @ColorInt
    private int getAttrColor(@AttrRes int resId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
        this.onMenuItemClickListener = onMenuItemClickListener;
        initListener();
    }

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItem item);
    }
}
