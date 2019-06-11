package ru.melodin.fast.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;

import ru.melodin.fast.CreateChatActivity;
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
import ru.melodin.fast.database.MemoryCache;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

public class FragmentDialogs extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private final int CONVERSATIONS_COUNT = 30;

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

        for (int i = 0; i < tb.getMenu().size(); i++) {
            MenuItem item = tb.getMenu().getItem(i);
            item.getIcon().setTint(ThemeManager.getMain());
        }

        tb.getMenu().getItem(0).setVisible(false);

        tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.create_chat:
                        startActivity(new Intent(getActivity(), CreateChatActivity.class));
                        break;
                    case R.id.clear_messages_cache:
                        DatabaseHelper.getInstance().dropMessagesTable(AppGlobal.database);

                        if (adapter != null) {
                            adapter.clear();
                            adapter.notifyDataSetChanged();
                        }
                        checkCount();
                        getConversations(CONVERSATIONS_COUNT, 0);
                        break;
                }
                return true;
            }
        });

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);

        list.setItemViewCacheSize(20);
        list.setDrawingCacheEnabled(true);
        list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        list.setLayoutManager(manager);

        getCachedConversations();
        if (Util.hasConnection())
            getConversations(CONVERSATIONS_COUNT, 0);
    }

    private void initViews(View v) {
        list = v.findViewById(R.id.list);
        empty = v.findViewById(R.id.no_items_layout);
        tb = v.findViewById(R.id.tb);
        refreshLayout = v.findViewById(R.id.refresh);
    }

    public void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createAdapter(ArrayList<VKConversation> conversations) {
        if (ArrayUtil.isEmpty(conversations)) return;

        if (adapter == null) {
            adapter = new DialogAdapter(this, conversations);
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            list.setAdapter(adapter);
            list.scrollToPosition(0);

            checkCount();
            return;
        }

        adapter.changeItems(conversations);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);

        checkCount();
    }

    private void getCachedConversations() {
        ArrayList<VKConversation> conversations = CacheStorage.getConversations();

        if (!ArrayUtil.isEmpty(conversations)) {
            createAdapter(conversations);
        }
    }

    private void getConversations(final int count, final int offset) {
        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            private ArrayList<VKConversation> conversations;

            @Override
            public void ready() throws Exception {
                conversations = VKApi.messages().getConversations()
                        .filter("all")
                        .extended(true)
                        .fields(VKUser.FIELDS_DEFAULT)
                        .count(count)
                        .execute(VKConversation.class);

                CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE);
                CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, conversations);

                ArrayList<VKUser> users = conversations.get(0).getConversationUsers();
                ArrayList<VKGroup> groups = conversations.get(0).getConversationGroups();
                ArrayList<VKMessage> messages = new ArrayList<>();

                for (VKConversation conversation : conversations)
                    messages.add(conversation.getLast());

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
                CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups);
                CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages);
            }

            @Override
            public void done() {
                EventBus.getDefault().post(CacheStorage.getUser(UserConfig.userId));
                createAdapter(conversations);
                refreshLayout.setRefreshing(false);

                tb.setTitle(title);
            }

            @Override
            public void error(Exception e) {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();
                Log.e("Load conversations", Log.getStackTraceString(e));
            }
        });
    }

    private void openChat(int position) {
        VKConversation conversation = adapter.getItem(position);

        int peerId = conversation.getLast().getPeerId();

        VKUser user = MemoryCache.getUser(peerId);
        VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(peerId));

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", adapter.getTitle(conversation, user, group));
        intent.putExtra("photo", adapter.getPhoto(conversation, user, group));
        intent.putExtra("conversation", conversation);
        intent.putExtra("peer_id", peerId);
        intent.putExtra("can_write", conversation.isCanWrite());

        if (!conversation.isCanWrite()) {
            intent.putExtra("reason", conversation.getReason());
        }

        startActivity(intent);
    }

    private void readMessage(final int position) {
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
            @Override
            public void ready() throws Exception {
                VKApi.messages().markAsRead().peerId(adapter.getItem(position).getLast().getPeerId()).execute();
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
        getConversations(CONVERSATIONS_COUNT, 0);
    }

    @Override
    public void onItemClick(View v, int position) {
        openChat(position);

        VKConversation conversation = adapter.getItem(position);

        if (!conversation.isRead() && !AppGlobal.preferences.getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
            readMessage(position);
        }
    }

    @Override
    public void onItemLongClick(View v, int position) {
        showAlert(position);
    }

    private void showAlert(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());

        VKConversation conversation = adapter.getItem(position);

        int peerId = conversation.getLast().getPeerId();

        VKUser user = MemoryCache.getUser(peerId);
        VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(peerId));

        adb.setTitle(adapter.getTitle(conversation, user, group));

        ArrayList<String> list = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.conversation_functions)));
        ArrayList<String> remove = new ArrayList<>();
        list.removeAll(remove);

        final String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++)
            items[i] = list.get(i);

        adb.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = items[i];

                if (title.equals(getString(R.string.clear))) {
                    showConfirmDeleteConversation(position);
                }
            }
        });
        adb.show();
    }

    private void showConfirmDeleteConversation(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteConversation(position);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void deleteConversation(final int position) {
        if (!Util.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        VKConversation conversation = adapter.getItem(position);
        final int peerId = conversation.getLast().getPeerId();

        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.messages().deleteConversation().peerId(peerId).offset(0).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE, DatabaseHelper.PEER_ID, peerId);
                adapter.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
            }

            @Override
            public void error(Exception e) {
                Log.e("Error delete dialog", Log.getStackTraceString(e));
                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
