package ru.stwtforever.fast.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ru.stwtforever.fast.MessagesActivity;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.adapter.FriendAdapter;
import ru.stwtforever.fast.adapter.RecyclerAdapter;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.cls.BaseFragment;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.DatabaseHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class FragmentFriends extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemLongClickListener, RecyclerAdapter.OnItemClickListener {

    public FragmentFriends() {
    }

    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;

    private FriendAdapter adapter;

    private Toolbar tb;

    private boolean loading;

    @Override
    public void onRefresh() {
        loadFriends(0, 0);
    }

    @Override
    public void onItemClick(View v, int position) {

    }

    @Override
    public void onItemLongClick(View v, int position) {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        this.title = getString(R.string.fragment_friends);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        tb = view.findViewById(R.id.tb);
        ViewUtils.applyToolbarStyles(tb);

        tb.setTitle(title);

        list = view.findViewById(R.id.list);
        setRecyclerView(list);

        refreshLayout = view.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        getFriends();
    }

    private void getFriends() {
        getCachedFriends();
        loadFriends(0, 0);
    }

    private void getCachedFriends() {
        ArrayList<VKUser> users = CacheStorage.getFriends(UserConfig.userId, false);

        if (ArrayUtil.isEmpty(users))
            return;

        createAdapter(users, 0);
    }

    private void loadFriends(final int count, final int offset) {
        if (!Utils.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

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
            }

            @Override
            public void error(Exception e) {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createAdapter(ArrayList<VKUser> users, int offset) {
        if (ArrayUtil.isEmpty(users))
            return;

        if (offset != 0) {
            adapter.changeItems(users);
            adapter.notifyDataSetChanged();
            return;
        }

        if (adapter != null) {
            adapter.changeItems(users);
            adapter.notifyDataSetChanged();
            return;
        }

        adapter = new FriendAdapter(this, users);
        list.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
    }

    public void openChat(int position) {
        VKUser user = adapter.getValues().get(position);

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", user.toString());
        intent.putExtra("photo", user.photo_200);
        intent.putExtra("peer_id", user.id);

        boolean can_write = !user.isDeactivated();

        intent.putExtra("can_write", can_write);

        if (!can_write) {
            intent.putExtra("reason", VKConversation.REASON_USER_BLOCKED_DELETED);
        }

        startActivity(intent);
    }

    public void showDialog(final int position, View v) {
        androidx.appcompat.widget.PopupMenu m = new androidx.appcompat.widget.PopupMenu(getActivity(), v);
        m.inflate(R.menu.fragment_friends_funcs);
        m.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.remove_friend:
                        showConfirmDeleteFriend(position);
                        break;
                }
                return true;
            }

        });
        m.show();
    }

    private void showConfirmDeleteFriend(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.confirm_delete_friend);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFriend(position);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.create().show();
    }

    private void deleteFriend(final int position) {
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            @Override
            public void ready() throws Exception {
                VKApi.friends().delete().userId(adapter.getItem(position).getId()).execute(Integer.class);
            }

            @Override
            public void done() {
                adapter.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(0, adapter.getItemCount(), adapter.getItem(adapter.getItemCount() - 1));
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(getActivity(), getString(R.string.error) + "!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
