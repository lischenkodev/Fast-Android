package ru.stwtforever.fast.fragment;

import android.content.*;
import android.os.*;
import android.support.annotation.Nullable;
import android.support.v4.widget.*;
import android.support.v7.widget.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import ru.stwtforever.fast.*;
import ru.stwtforever.fast.adapter.*;
import ru.stwtforever.fast.cls.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.concurrent.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;

import java.util.*;

import android.support.v7.widget.Toolbar;

import ru.stwtforever.fast.util.ViewUtils;

public class FragmentFriends extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, OnItemListener {

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
    public void OnItemClick(View v, int position) {
        VKUser user = adapter.getValues().get(position);
        if (user == null) return;
        Toast.makeText(getActivity(), user.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(View v, int position) {
        return;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        this.title = getString(R.string.fragment_friends);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        tb = view.findViewById(R.id.tb);
        ViewUtils.applyToolbarStyles(tb);

        tb.setTitle(title);

        list = view.findViewById(R.id.list);
        setList(list);

        refreshLayout = view.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), OrientationHelper.VERTICAL, false);
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
            adapter.add(users);
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
        adapter.setListener(this);
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
}
