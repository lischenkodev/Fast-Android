package ru.melodin.fast.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;

import ru.melodin.fast.CreateChatActivity;
import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.adapter.ConversationAdapter;
import ru.melodin.fast.adapter.RecyclerAdapter;
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
import ru.melodin.fast.view.FastToolbar;

public class FragmentConversations extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private final int CONVERSATIONS_COUNT = 30;

    private SwipeRefreshLayout refreshLayout;
    private RecyclerView list;
    private ConversationAdapter adapter;

    private FastToolbar tb;
    private View empty;

    private boolean loading;

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.fragment_messages));
    }

    @Override
    public void onDestroy() {
        if (adapter != null)
            adapter.destroy();
        super.onDestroy();
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

        setToolbar(tb);
        setRecyclerView(list);

        tb.setTitle(getTitle());

        tb.inflateMenu(R.menu.fragment_dialogs_menu);
        tb.setItemVisible(0, false);
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.create_chat) {
                startActivity(new Intent(getActivity(), CreateChatActivity.class));
            }
        });

        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getPrimary());

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);

        list.setItemViewCacheSize(20);
        list.setDrawingCacheEnabled(true);
        list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        list.setLayoutManager(manager);

        getCachedConversations();
        if (savedInstanceState == null)
            getConversations(CONVERSATIONS_COUNT, 0);
    }

    private void initViews(@NonNull View v) {
        tb = v.findViewById(R.id.tb);
        list = v.findViewById(R.id.list);
        empty = v.findViewById(R.id.no_items_layout);
        refreshLayout = v.findViewById(R.id.refresh);
    }

    public void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createAdapter(ArrayList<VKConversation> conversations) {
        if (ArrayUtil.isEmpty(conversations)) return;

        if (adapter == null) {
            adapter = new ConversationAdapter(this, conversations);
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
        if (loading) return;

        if (!Util.hasConnection()) {
            refreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), R.string.connect_to_the_internet, Toast.LENGTH_SHORT).show();
            return;
        }

        loading = true;
        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            private ArrayList<VKConversation> conversations;

            @Override
            public void ready() throws Exception {
                conversations = VKApi.messages().getConversations()
                        .filter("all")
                        .extended(true)
                        .fields(VKUser.FIELDS_DEFAULT + ", " + VKGroup.FIELDS_DEFAULT)
                        .count(count)
                        .offset(offset)
                        .execute(VKConversation.class);
            }

            @Override
            public void done() {
                CacheStorage.delete(DatabaseHelper.CONVERSATIONS_TABLE);
                CacheStorage.insert(DatabaseHelper.CONVERSATIONS_TABLE, conversations);

                ArrayList<VKUser> users = VKConversation.users;
                ArrayList<VKGroup> groups = VKConversation.groups;
                ArrayList<VKMessage> messages = new ArrayList<>();

                for (VKConversation conversation : conversations)
                    messages.add(conversation.getLast());

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
                CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups);
                CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages);

                createAdapter(conversations);
                refreshLayout.setRefreshing(false);

                loading = false;
            }

            @Override
            public void error(Exception e) {
                loading = false;
                refreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void openChat(int position) {
        VKConversation conversation = adapter.getItem(position);

        int peerId = conversation.getPeerId();

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", conversation.getFullTitle());
        intent.putExtra("photo", conversation.getPhoto());
        intent.putExtra("conversation", conversation);
        intent.putExtra("peer_id", peerId);
        intent.putExtra("can_write", conversation.isCanWrite());

        if (!conversation.isCanWrite()) {
            intent.putExtra("reason", conversation.getReason());
        }

        startActivity(intent);
    }

    private void readConversation(final int peerId) {
        if (!Util.hasConnection()) return;
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.messages().markAsRead().peerId(peerId).execute(Integer.class).get(0);
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
            readConversation(conversation.getPeerId());
        }
    }

    @Override
    public void onItemLongClick(View v, int position) {
        showAlert(position);
    }

    private void showAlert(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());

        VKConversation conversation = adapter.getItem(position);

        int peerId = conversation.getPeerId();

        adb.setTitle(conversation.getFullTitle());

        ArrayList<String> list = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.conversation_functions)));
        ArrayList<String> remove = new ArrayList<>();

        if (conversation.getLast() == null || conversation.getLastMessageId() == -1 || conversation.getLast().isOut() || conversation.isRead() || conversation.getLast().isRead()) {
            remove.add(getString(R.string.read));
        }

        list.removeAll(remove);

        final String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++)
            items[i] = list.get(i);

        adb.setItems(items, (dialogInterface, i) -> {
            String title = items[i];

            if (title.equals(getString(R.string.clear_messages_history))) {
                showConfirmDeleteConversation(position);
            } else if (title.equals(getString(R.string.read))) {
                readConversation(peerId);
            }
        });
        adb.show();
    }

    private void showConfirmDeleteConversation(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, (dialogInterface, i) -> deleteConversation(position));
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void deleteConversation(final int position) {
        VKConversation conversation = adapter.getItem(position);
        final int peerId = conversation.getLast().getPeerId();

        if (!Util.hasConnection() || (peerId == -1 || peerId == 0)) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            @Override
            public void ready() throws Exception {
                VKApi.messages().deleteConversation().peerId(peerId).execute();
            }

            @Override
            public void done() {
                CacheStorage.delete(DatabaseHelper.CONVERSATIONS_TABLE, DatabaseHelper.PEER_ID, peerId);
                adapter.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void error(Exception e) {
                refreshLayout.setRefreshing(false);
                Log.e("Error delete dialog", Log.getStackTraceString(e));
                Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
