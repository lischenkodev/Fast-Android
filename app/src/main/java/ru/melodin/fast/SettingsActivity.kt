package ru.melodin.fast

import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar.*
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.ViewUtil

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.currentTheme)
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tb.setTitle(R.string.settings)
        tb.setBackVisible(true)

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, FragmentSettings())
            .commit()
    }

    override fun applyStyles() {
        finish()
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
