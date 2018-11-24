package ru.stwtforever.fast;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import ru.stwtforever.fast.adapter.ShowCreateAdapter;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.util.ViewUtils;

public class ShowCreateChatActivity extends AppCompatActivity {

    private ShowCreateAdapter adapter;

    private EditText title;
    private Toolbar tb;
    private RecyclerView list;

    private ArrayList<VKUser> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());

        super.onCreate(savedInstanceState);
        getIntentData();

        setContentView(R.layout.activity_show_create);
        initViews();
    }

    private void getIntentData() {
        Bundle b = getIntent().getExtras();

        users = (ArrayList<VKUser>) b.getSerializable("users");
    }

    private void initViews() {
        title = findViewById(R.id.title);
        title.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
                invalidateOptionsMenu();
            }
        });

        tb = findViewById(R.id.tb);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.md_clear);

        getSupportActionBar().setTitle(getString(R.string.create_chat));

        ViewUtils.applyToolbarStyles(tb);

        SwipeRefreshLayout refresh = findViewById(R.id.refresh);
        refresh.setEnabled(false);

        list = findViewById(R.id.list);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        createAdapter();
    }

    private void createAdapter() {
        adapter = new ShowCreateAdapter(this, users);
        list.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.create:
                if (adapter != null)
                    createChat();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createChat() {
        if (users == null) return;

        ThreadExecutor.execute(new AsyncCallback(this) {

            int res;

            @Override
            public void ready() throws Exception {
                ArrayList<Integer> ids = new ArrayList<>();
                for (VKUser u : adapter.getValues()) {
                    ids.add(u.id);
                }

                res = VKApi.messages().createChat().title(title.getText().toString()).userIds(ids).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(ShowCreateChatActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (title == null) return false;

        MenuItem create = menu.getItem(0);
        int color = 0;
        String s = title.getText().toString();
        boolean haveText = s != null && !s.trim().isEmpty();

        color = haveText ? ViewUtils.mainColor : Color.LTGRAY;

        create.getIcon().setTint(color);
        create.setEnabled(haveText);

        return super.onPrepareOptionsMenu(menu);
    }

}
