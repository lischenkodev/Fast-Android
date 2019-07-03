package ru.melodin.fast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ru.melodin.fast.adapter.ShowCreateAdapter;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.FastToolbar;

public class ShowCreateChatActivity extends AppCompatActivity {

    private ShowCreateAdapter adapter;

    private AppCompatEditText title;
    private FastToolbar tb;
    private RecyclerView list;

    private ArrayList<VKUser> users;

    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);

        users = (ArrayList<VKUser>) getIntent().getSerializableExtra("users");

        setContentView(R.layout.activity_show_create);
        initViews();

        tb.inflateMenu(R.menu.activity_create_chat);
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.create && adapter != null)
                createChat();
        });
        tb.setBackIcon(ContextCompat.getDrawable(this, R.drawable.md_clear));
        tb.setBackVisible(true);
        tb.setOnBackClickListener(view -> onBackPressed());

        tb.setTitle(R.string.create_chat);

        findViewById(R.id.refresh).setEnabled(false);

        title.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                invalidateOptionsMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        createAdapter();
    }

    private void initViews() {
        title = findViewById(R.id.title);
        tb = findViewById(R.id.tb);
        list = findViewById(R.id.list);
        empty = findViewById(R.id.no_items_layout);
    }

    private void createAdapter() {
        adapter = new ShowCreateAdapter(this, users);
        list.setAdapter(adapter);

        tb.setOnClickListener(v -> list.smoothScrollToPosition(0));

        checkCount();
    }

    private void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createChat() {
        ThreadExecutor.execute(new AsyncCallback(this) {

            int peerId;

            StringBuilder title_;

            @Override
            public void ready() throws Exception {
                ArrayList<Integer> ids = new ArrayList<>();
                for (VKUser user : adapter.getValues()) {
                    ids.add(user.getId());
                }

                title_ = new StringBuilder(title.getText().toString().trim());

                if (TextUtils.isEmpty(title_.toString())) {
                    if (users.size() == 1) {
                        title_.append(users.get(0).getName());
                    } else
                        for (int i = 0; i < users.size(); i++) {
                            VKUser user = adapter.getItem(i);
                            title_.append(user.getName()).append(i == users.size() ? "" : ", ");
                        }
                }

                peerId = 2_000_000_000 + VKApi.messages().createChat().title(title_.toString()).userIds(ids).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                Intent intent = new Intent();
                intent.putExtra("title", title_.toString());
                intent.putExtra("peer_id", peerId);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

            @Override
            public void error(Exception e) {
                Log.e("Error create chat", Log.getStackTraceString(e));
                Toast.makeText(ShowCreateChatActivity.this, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
