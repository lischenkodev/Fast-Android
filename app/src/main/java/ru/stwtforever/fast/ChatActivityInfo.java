package ru.stwtforever.fast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import ru.stwtforever.fast.api.model.VKUser;

public class ChatActivityInfo extends AppCompatActivity {

    private long cid;

    private ListView lv;
    private EditText et;
    private ImageView ivAva;

    private String title;

    // private VKChat chat;
    private VKUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);


        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cid = getIntent().getExtras().getLong("cid");
        title = getIntent().getExtras().getString("title");
        lv = (ListView) findViewById(R.id.lv);
        et = (EditText) findViewById(R.id.et);
        ivAva = (ImageView) findViewById(R.id.ivAva);

        et.setText(title);
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
