package ru.stwtforever.fast;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ru.stwtforever.fast.adapter.CreateChatAdapter;
import ru.stwtforever.fast.adapter.RecyclerAdapter;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.database.CacheStorage;
import ru.stwtforever.fast.database.DatabaseHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Requests;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class CreateChatActivity extends AppCompatActivity implements RecyclerAdapter.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    @Override
    public void onRefresh() {
        loadFriends(0, 0);
    }

    @Override
    public void onItemClick(View v, int position) {
        adapter.setSelected(position, !adapter.isSelected(position));
        adapter.notifyItemChanged(position);
        setTitle();

        isSelecting = adapter.getSelectedCount() > 0;
    }

    private boolean loading;

    private Toolbar tb;
    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;

    private CreateChatAdapter adapter;

    private boolean isSelecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);
        initViews();

        getFriends();
    }

    @Override
    public void onBackPressed() {
        if (isSelecting) {
            adapter.clearSelect();
            isSelecting = false;
        } else
            super.onBackPressed();

        setTitle();
    }

    private void getFriends() {
        getCachedFriends();
    }

    private void getCachedFriends() {
        ArrayList<VKUser> users = CacheStorage.getFriends(UserConfig.userId, false);

        if (ArrayUtil.isEmpty(users)) {
            loadFriends(0, 0);
            return;
        }

        createAdapter(users, 0);
    }

    private void createAdapter(ArrayList<VKUser> users, int offset) {
        if (ArrayUtil.isEmpty(users))
            return;

        if (offset != 0) {
            adapter.changeItems(users);
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), null);
            return;
        }

        if (adapter != null) {
            adapter.changeItems(users);
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), null);
            return;
        }

        adapter = new CreateChatAdapter(this, users);
        list.setAdapter(adapter);
        adapter.setOnItemClickListener(this);

        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.scrollToPosition(0);
            }
        });
    }

    private void loadFriends(final int offset, final int count) {
        if (!Utils.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        setTitle();

        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class);

                if (users.isEmpty()) {
                    loading = true;
                }

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE);
                    CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, users);
                }

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
            }

            @Override
            public void done() {
                createAdapter(users, offset);
                refreshLayout.setRefreshing(false);

                if (!users.isEmpty()) {
                    loading = false;
                }

                setTitle();
            }

            @Override
            public void error(Exception e) {
                setTitle();
                refreshLayout.setRefreshing(false);
                Toast.makeText(CreateChatActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setTitle() {
        String title = getString(R.string.select_friends);
        String subtitle;


        int selected = adapter == null ? 0 : adapter.getSelectedCount();

        if (selected > 0) {
            subtitle = String.format(getString(R.string.selected_count), String.valueOf(selected));
        } else {
            subtitle = "";
        }

        tb.setTitle(title);
        tb.setSubtitle(subtitle);
    }

    private void initViews() {
        tb = findViewById(R.id.tb);
        setTitle();

        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewUtils.applyToolbarStyles(tb);

        refreshLayout = findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        list = findViewById(R.id.list);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.create:
                getUsers();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getUsers() {
        HashMap<Integer, VKUser> items = adapter.getSelectedPositions();

        Collection<VKUser> values = items.values();

        ArrayList<VKUser> users = new ArrayList<>(values);

        createChat(users);
    }

    private void createChat(ArrayList<VKUser> users) {
        VKUser user = UserConfig.getUser();
        users.add(0, user);

        Bundle b = new Bundle();
        b.putSerializable("users", users);

        startActivityForResult(new Intent(this, ShowCreateChatActivity.class).putExtras(b), Requests.CREATE_CHAT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Requests.CREATE_CHAT) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
