package ru.melodin.fast.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.adapter.DialogAdapter;
import ru.melodin.fast.adapter.RecyclerAdapter;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.current.BaseFragment;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

public class FragmentDialogs extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private final int CONVERSATIONS_COUNT = 60;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView list;
    private Toolbar tb;
    private DialogAdapter adapter;

    private View empty;

    @Override
    public void onDestroy() {
        if (adapter != null) adapter.destroy();
        super.onDestroy();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.title = getString(R.string.fragment_messages);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        setRecyclerView(list);

        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        tb.setTitle(title);
        tb.inflateMenu(R.menu.fragment_dialogs_menu);
        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.create_chat:
                        break;
                    case R.id.clear_messages_cache:
                        DatabaseHelper.getInstance().dropMessagesTable(AppGlobal.database);

                        if (adapter != null) {
                            adapter.clear();
                            adapter.notifyDataSetChanged();
                        }
                        checkCount();
                        getConversations(0, CONVERSATIONS_COUNT);
                        break;
                }
                return true;
            }
        });

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        getCachedConversations();
        //getConversations(0, CONVERSATIONS_COUNT);
    }

    private void initViews(View v) {
        list = v.findViewById(R.id.list);
        empty = v.findViewById(R.id.no_items_layout);
        tb = v.findViewById(R.id.tb);
        refreshLayout = v.findViewById(R.id.refresh);
    }

    private void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createAdapter(ArrayList<VKConversation> conversations, int offset) {
        if (ArrayUtil.isEmpty(conversations)) return;

        checkCount();

        if (adapter == null) {
            adapter = new DialogAdapter(getActivity(), conversations);
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            list.setAdapter(adapter);

            checkCount();
            return;
        }

        if (offset != 0) {
            adapter.changeItems(conversations);
            adapter.notifyDataSetChanged();

            checkCount();
            return;
        }

        adapter.changeItems(conversations);
        adapter.notifyDataSetChanged();

        checkCount();
    }

    private void getCachedConversations() {
        ArrayList<VKConversation> conversations = CacheStorage.getConversations();

        if (ArrayUtil.isEmpty(conversations)) return;

        createAdapter(conversations, 0);
    }

    private void getConversations(final int offset, final int count) {
        if (!Util.hasConnection()) {
            if (refreshLayout.isRefreshing())
                refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            private ArrayList<VKConversation> conversations;

            @Override
            public void ready() throws Exception {
                conversations = VKApi.messages().getConversations()
                        .filter("all")
                        .extended(true)
                        .fields(VKUser.FIELDS_DEFAULT)
                        .offset(offset)
                        .count(count)
                        .execute(VKConversation.class);

                //if (conversations.isEmpty()) loading = true;

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE);
                    CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, conversations);
                }

                ArrayList<VKUser> users = conversations.get(0).conversation_users;
                ArrayList<VKGroup> groups = conversations.get(0).conversation_groups;
                ArrayList<VKMessage> messages = new ArrayList<>();

                for (VKConversation conversation : conversations)
                    messages.add(conversation.last);

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
                CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups);
                CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages);
            }

            @Override
            public void done() {
                EventBus.getDefault().post(CacheStorage.getUser(UserConfig.userId));
                createAdapter(conversations, offset);
                refreshLayout.setRefreshing(false);

                //if (!conversations.isEmpty()) loading = false;

                tb.setTitle(title);
            }

            @Override
            public void error(Exception e) {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                Log.e("Load conversations", Log.getStackTraceString(e));
            }
        });
    }

    private void openChat(int position) {
        VKConversation conversation = adapter.getItem(position);
        VKUser user = CacheStorage.getUser(conversation.last.peerId);
        VKGroup group = CacheStorage.getGroup(VKGroup.toGroupId(conversation.last.peerId));

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", adapter.getTitle(conversation, user, group));
        intent.putExtra("photo", adapter.getPhoto(conversation, user, group));
        intent.putExtra("conversation", conversation);
        intent.putExtra("peer_id", conversation.last.peerId);
        intent.putExtra("can_write", conversation.can_write);

        if (!conversation.can_write) {
            intent.putExtra("reason", conversation.reason);
        }

        startActivity(intent);
    }

    private void readMessage(final int position) {
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
            @Override
            public void ready() throws Exception {
                VKApi.messages().markAsRead().peerId(adapter.getItem(position).last.peerId).execute();
            }

            @Override
            public void done() {

            }

            @Override
            public void error(Exception e) {

            }
        });
    }

    @Override
    public void onRefresh() {
        getConversations(0, CONVERSATIONS_COUNT);
    }

    @Override
    public void onItemClick(View v, int position) {
        openChat(position);

        VKConversation conversation = adapter.getItem(position);

        if (!conversation.read && !AppGlobal.preferences.getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
            readMessage(position);
        }
    }

    @Override
    public void onItemLongClick(View v, int position) {

    }
}
