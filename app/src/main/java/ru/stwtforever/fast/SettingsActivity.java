package ru.stwtforever.fast;

import android.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.view.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.fragment.*;
import ru.stwtforever.fast.util.*;

import ru.stwtforever.fast.util.ViewUtils;
import org.greenrobot.eventbus.*;
import android.graphics.*;
import android.graphics.drawable.*;

public class SettingsActivity extends AppCompatActivity {
	
	private Toolbar tb;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		ViewUtils.applyWindowStyles(this);
		setTheme(ThemeManager.getCurrentTheme());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		tb = findViewById(R.id.tb);
		setSupportActionBar(tb);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ViewUtils.applyToolbarStyles(tb);
		
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentSettings()).commit();
	}
	
	@Override
	protected void onDestroy() {
		EventBus.getDefault().unregister(this);
		super.onDestroy();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) finish();
		return super.onOptionsItemSelected(item);
	}
}
