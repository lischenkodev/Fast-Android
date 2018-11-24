package ru.stwtforever.fast;

import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.support.v7.app.*;
import android.widget.*;
import ru.stwtforever.fast.common.*;

public class TestActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeManager.getCurrentTheme());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		Button b = findViewById(R.id.btn);
		
		b.setTextColor(ThemeManager.isDark() ? Color.WHITE : Color.BLACK);
		
		Drawable search = getResources().getDrawable(R.drawable.ic_search);
		search.setTint(ThemeManager.isDark() ? Color.WHITE : 0xff404040);
		search.setBounds(0,0,60,60);
		
		b.setCompoundDrawablesWithIntrinsicBounds(search, null, null, null);
		
		if (b.getBackground() == null) return;
		
		if (ThemeManager.isDark()) {
			b.getBackground().setTint(0xff404040);
		} else {
			b.getBackground().setTint(Color.WHITE);
		}
	}
}
