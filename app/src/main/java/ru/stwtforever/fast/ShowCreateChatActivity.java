package ru.stwtforever.fast;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
        users = (ArrayList<VKUser>) getIntent().getSerializableExtra("users");
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
        manager.setOrientation(RecyclerView.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        createAdapter();
    }

    private void createAdapter() {
        adapter = new ShowCreateAdapter(this, users);
        list.setAdapter(adapter);

        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.scrollToPosition(0);
            }
        });
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
        ThreadExecutor.execute(new AsyncCallback(this) {

            int res;

            @Override
            public void ready() throws Exception {
                ArrayList<Integer> ids = new ArrayList<>();
                for (VKUser u : adapter.getValues()) {
                    ids.add(u.id);
                }

                StringBuilder title_ = new StringBuilder(title.getText().toString().trim());
                if (TextUtils.isEmpty(title_.toString())) {
                    if (users.size() == 1) {
                        title_.append(users.get(0).name);
                    } else for (int i = 0; i < users.size(); i++) {
                        VKUser user = adapter.getItem(i);
                        title_.append(user.name).append(i == users.size() ? "" : ", ");
                    }
                }

                res = VKApi.messages().createChat().title(title_.toString()).userIds(ids).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void error(Exception e) {
                Log.e("Error create chat", Log.getStackTraceString(e));
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
        menu.findItem(R.id.create).getIcon().setTint(ViewUtils.mainColor);
        return super.onPrepareOptionsMenu(menu);
    }

}
