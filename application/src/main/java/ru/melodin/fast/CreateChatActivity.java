package ru.melodin.fast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;

import ru.melodin.fast.adapter.CreateChatAdapter;
import ru.melodin.fast.adapter.RecyclerAdapter;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Requests;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.Toolbar;

public class CreateChatActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener {

    private View empty;

    private Toolbar tb;
    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;

    private CreateChatAdapter adapter;

    private boolean selecting;

    @Override
    public void onRefresh() {
        loadFriends();
    }

    @Override
    public void onItemClick(View v, int position) {
        adapter.toggleSelected(position);
        adapter.notifyItemChanged(position, -1);
        setTitle();

        selecting = adapter.getSelectedCount() > 0;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chat);

        initViews();

        setTitle();

        tb.setBackVisible(true);
        tb.setOnBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        tb.inflateMenu(R.menu.activity_create_chat);
        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.create)
                    getUsers();
            }
        });

        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        getCachedFriends();
    }

    @Override
    public void onBackPressed() {
        if (selecting) {
            adapter.clearSelect();
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
            selecting = false;

            setTitle();
        } else
            super.onBackPressed();
    }

    private void getCachedFriends() {
        ArrayList<VKUser> users = CacheStorage.getFriends(UserConfig.userId, false);

        if (ArrayUtil.isEmpty(users)) {
            loadFriends();
            return;
        }

        createAdapter(users);
    }

    private void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createAdapter(ArrayList<VKUser> users) {
        if (ArrayUtil.isEmpty(users)) return;

        if (adapter == null) {
            adapter = new CreateChatAdapter(this, users);
            adapter.setOnItemClickListener(this);
            list.setAdapter(adapter);

            checkCount();
            return;
        }

        adapter.changeItems(users);
        adapter.notifyDataSetChanged();

        checkCount();
    }

    private void loadFriends() {
        if (!Util.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        setTitle();

        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> friends;

            @Override
            public void ready() throws Exception {
                friends = VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class);

                CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE);
                CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, friends);
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, friends);
            }

            @Override
            public void done() {
                createAdapter(friends);
                refreshLayout.setRefreshing(false);

                setTitle();
            }

            @Override
            public void error(Exception e) {
                setTitle();
                refreshLayout.setRefreshing(false);
                Toast.makeText(CreateChatActivity.this, R.string.error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setTitle() {
        int selected = adapter == null ? 0 : adapter.getSelectedCount();

        String title = getString(R.string.select_friends);
        String subtitle = selected > 0 ? String.format(getString(R.string.selected_count), String.valueOf(selected)) : "";

        tb.setTitle(title);
        tb.setSubtitle(subtitle);
    }

    private void initViews() {
        empty = findViewById(R.id.no_items_layout);
        tb = findViewById(R.id.tb);
        refreshLayout = findViewById(R.id.refresh);
        list = findViewById(R.id.list);
    }

    private void getUsers() {
        SparseArray<VKUser> items = adapter.getSelectedPositions();

        ArrayList<VKUser> users = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            users.add(items.valueAt(i));
        }

        createChat(users);
    }

    private void createChat(ArrayList<VKUser> users) {
        VKUser user = UserConfig.getUser();
        users.add(0, user);

        Intent intent = new Intent(this, ShowCreateChatActivity.class);
        intent.putExtra("users", users);

        startActivityForResult(intent, Requests.CREATE_CHAT);

        adapter.clearSelect();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
        setTitle();
    }

    private void loadChat(final String title, final int peerId) {
        ThreadExecutor.execute(new AsyncCallback(this) {
            VKConversation conversation;

            @Override
            public void ready() throws Exception {
                conversation = VKApi.messages().getConversationsById().peerIds(peerId).extended(true).fields(VKUser.FIELDS_DEFAULT).execute(VKConversation.class).get(0);
            }

            @Override
            public void done() {
                openChat(title, peerId, conversation);
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load chat", Log.getStackTraceString(e));
                Toast.makeText(CreateChatActivity.this, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChat(String title, int peerId, VKConversation conversation) {
        Intent intent = new Intent(this, MessagesActivity.class);

        intent.putExtra("title", title);
        intent.putExtra("conversation", conversation);
        intent.putExtra("peer_id", peerId);
        intent.putExtra("can_write", true);

        finish();
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Requests.CREATE_CHAT) {
            if (resultCode == Activity.RESULT_OK) {
                String title = data.getStringExtra("title");
                int peerId = data.getIntExtra("peer_id", -1);

                loadChat(title, peerId);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
