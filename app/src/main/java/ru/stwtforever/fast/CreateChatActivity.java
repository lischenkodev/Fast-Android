package ru.stwtforever.fast;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import ru.stwtforever.fast.adapter.CreateChatAdapter;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.cls.OnItemListener;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.DBHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Requests;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class CreateChatActivity extends AppCompatActivity implements OnItemListener, SwipeRefreshLayout.OnRefreshListener {

    @Override
    public void onRefresh() {
        loadFriends(0, 0);
    }

    @Override
    public void OnItemClick(View v, int position) {
        adapter.setSelected(position, !adapter.isSelected(position));
        adapter.notifyItemChanged(position);
        setTitle();

        isSelecting = adapter.getSelectedCount() > 0;
        invalidateOptionsMenu();
    }

    @Override
    public void onItemLongClick(View v, int position) {
        return;
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
        invalidateOptionsMenu();
    }

    private void getFriends() {
        getCachedFriends();
    }

    private void getCachedFriends() {
        ArrayList<VKUser> users = CacheStorage.getFriends(UserConfig.userId, false);

        if (ArrayUtil.isEmpty(users)) {
            loadFriends(0, 0);
        }

        createAdapter(users, 0);
    }

    private void createAdapter(ArrayList<VKUser> users, int offset) {
        if (ArrayUtil.isEmpty(users))
            return;

        if (offset != 0) {
            adapter.add(users);
            adapter.notifyDataSetChanged();
            return;
        }

        if (adapter != null) {
            adapter.changeItems(users);
            adapter.notifyDataSetChanged();
            return;
        }

        adapter = new CreateChatAdapter(this, users);
        list.setAdapter(adapter);
        adapter.setListener(this);
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
                    CacheStorage.delete(DBHelper.FRIENDS_TABLE);
                    CacheStorage.insert(DBHelper.FRIENDS_TABLE, users);
                }

                CacheStorage.insert(DBHelper.USERS_TABLE, users);
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
        String subtitle = "";


        int selected = adapter == null ? 0 : adapter.getSelectedCount();

        if (selected > 0) {
            subtitle = String.format(getString(R.string.selected_count), selected);
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
        manager.setOrientation(LinearLayoutManager.VERTICAL);

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
        if (!isSelecting) return;

        HashMap<Integer, VKUser> items = adapter.getSelectedPositions();

        Collection<VKUser> values = items.values();

        ArrayList<VKUser> users = new ArrayList<>();

        for (VKUser u : values) {
            users.add(u);
        }

        createChat(users);

    }

    private void createChat(ArrayList<VKUser> users) {
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
        MenuItem create = menu.getItem(0);

        int color = 0;

        color = isSelecting ? ViewUtils.mainColor : Color.LTGRAY;

        create.getIcon().setTint(color);

        create.setEnabled(isSelecting);

        return super.onPrepareOptionsMenu(menu);
    }


} 
