package ru.stwtforever.fast

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.greenrobot.eventbus.EventBus
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.fragment.FragmentSettings

class SettingsActivity : AppCompatActivity() {

    private var tb: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getCurrentTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tb = findViewById(R.id.tb)
        setSupportActionBar(tb)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, FragmentSettings()).commit()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
