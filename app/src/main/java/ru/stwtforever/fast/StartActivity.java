package ru.stwtforever.fast;

import android.support.v7.app.*;
import android.os.*;
import android.widget.*;
import ru.stwtforever.fast.helper.*;
import ru.stwtforever.fast.util.*;

public class StartActivity extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		
		Button web = findViewById(R.id.login_web);
		Button vk = findViewById(R.id.login_by_vk);
		Button token = findViewById(R.id.login_token);
		
		TextView title = findViewById(R.id.title);
		TextView loginBy = findViewById(R.id.login_by);
		
		FontHelper.setFont(new Button[]{web, vk, token}, FontHelper.PS_REGULAR);
		FontHelper.setFont(new TextView[]{title, loginBy}, FontHelper.PS_MEDIUM);
	}
	
}
