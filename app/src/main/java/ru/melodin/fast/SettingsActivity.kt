package ru.melodin.fast

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_messages.*
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.fragment.FragmentSettings
import ru.melodin.fast.util.ViewUtil

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        ViewUtil.applyWindowStyles(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tb.setTitle(R.string.settings)
        tb.setBackVisible(true)
        tb.setOnBackClickListener { onBackPressed() }

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, FragmentSettings()).commit()
    }
}
