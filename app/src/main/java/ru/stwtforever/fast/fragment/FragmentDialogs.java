package ru.stwtforever.fast.fragment;

import android.content.*;
import android.os.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
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
import org.greenrobot.eventbus.*;

import android.support.v7.widget.Toolbar;
import ru.stwtforever.fast.util.ViewUtils;

public class FragmentDialogs extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, OnItemListener {
	
	public FragmentDialogs() {}
	
	@Override
	public void OnItemClick(View v, int position) {
		openChat(position, false);
	}

	@Override
	public void onItemLongClick(View v, int position) {
		showDialog(position);
	}
	
	private static final int DIALOGS_COUNT = 60;
	
	private RecyclerView list;
	private SwipeRefreshLayout refreshLayout;

	private Toolbar tb;
	
    private DialogAdapter adapter;
	
	private boolean loading;
	
	public FragmentDialogs(CharSequence title) {
		this.title = title;
	}

    @Override
    public void onRefresh() {
		getDialogs(0, DIALOGS_COUNT);
    }

	@Override
	public void onDestroy() {
		if (adapter != null) {
			adapter.destroy();
		}
		super.onDestroy();
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_dialogs, container, false);
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		tb = view.findViewById(R.id.tb);
		
		ViewUtils.applyToolbarStyles(tb);
		
		tb.setTitle(title);
		
		tb.inflateMenu(R.menu.activity_main_menu);
		tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (item.getItemId() == R.id.create_chat) {
						startActivity(new Intent(getActivity(), CreateChatActivity.class));
					}
					return true;
				}
			
		});
		
		tb.getMenu().getItem(0).getIcon().setTint(ViewUtils.mainColor);
		
		list = view.findViewById(R.id.list);
		setList(list);

		refreshLayout = view.findViewById(R.id.refresh);
		refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
		refreshLayout.setOnRefreshListener(this);
		refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

		LinearLayoutManager manager = new LinearLayoutManager(getActivity(), OrientationHelper.VERTICAL, false);
		list.setHasFixedSize(true);
		list.setLayoutManager(manager);
		
		getMessages();
	}
	
	private void showDialog(final int position) {
		String[] features = new String[] {
			getString(R.string.clean_history)
		};
		
		VKConversation item = adapter.getValues().get(position);
		VKGroup group = CacheStorage.getGroup(VKGroup.toGroupId(item.last.peerId));
		VKUser user = CacheStorage.getUser(item.last.peerId);
		
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		adb.setTitle(adapter.getTitle(item, user, group));
		adb.setItems(features, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
						case 0:
							showDeleteConfirmDialog();
							break;
					}
				}

				private void showDeleteConfirmDialog() {
					AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
					adb.setTitle(R.string.warning);
					adb.setMessage(R.string.confirm_delete_dialog_message);
					adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteDialog();
							}

							private void deleteDialog() {
								ThreadExecutor.execute(new AsyncCallback(getActivity()) {
										int response = -1;
										@Override
										public void ready() throws Exception {
											VKConversation m = adapter.getValues().get(position);
											response = VKApi.messages().deleteConversation().peerId(m.last.peerId).execute(Integer.class).get(0);
										}

										@Override
										public void done() {
											adapter.getValues().remove(position);
											adapter.notifyDataSetChanged();
										}

										@Override
										public void error(Exception e) {
											Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_LONG).show();
										}
									});
							}

						});
					adb.setNegativeButton(R.string.no, null);
					adb.show();
				}
			});
		adb.show();
	}

	void getMessages() {
		getCachedDialogs();
		getDialogs(0, DIALOGS_COUNT);
	}

	private void createAdapter(ArrayList<VKConversation> messages, int offset) {
        if (ArrayUtil.isEmpty(messages)) {
            return;
        }
        if (offset != 0) {
            adapter.add(messages);
            adapter.notifyDataSetChanged();
            return;
        }

        if (adapter != null) {
            adapter.changeItems(messages);
            adapter.notifyDataSetChanged();
            return;
        }
        adapter = new DialogAdapter(this, messages);
        list.setAdapter(adapter);
		adapter.setListener(this);
    }

	private void getCachedDialogs() {
        ArrayList<VKConversation> dialogs = CacheStorage.getDialogs();
        if (ArrayUtil.isEmpty(dialogs)) {
            return;
        }

        createAdapter(dialogs, 0);
    }

    private void getDialogs(final int offset, final int count) {
        if (!Utils.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);
        ThreadExecutor.execute(new AsyncCallback(getActivity()) {
				private ArrayList<VKConversation> messages;

				@Override
				public void ready() throws Exception {
					messages = VKApi.messages().getConversations().filter("all").extended(true).offset(offset).count(count).execute(VKConversation.class);

					if (messages.isEmpty()) {
						loading = true;
					}

					if (offset == 0) {
						CacheStorage.delete(DBHelper.DIALOGS_TABLE);
						CacheStorage.insert(DBHelper.DIALOGS_TABLE, messages);
					}
					
					ArrayList<VKUser> users = messages.get(0).profiles;
					ArrayList<VKGroup> groups = messages.get(0).groups;
					ArrayList<VKMessage> last_messages = new ArrayList<>();
					
					for (int i = 0; i < messages.size(); i++) {
						VKMessage last = messages.get(i).last;
						last_messages.add(last);
					}
					
					if (!ArrayUtil.isEmpty(last_messages)) 
						CacheStorage.insert(DBHelper.MESSAGES_TABLE, last_messages);
					
					if (!ArrayUtil.isEmpty(users))
						CacheStorage.insert(DBHelper.USERS_TABLE, users);
					
					if (!ArrayUtil.isEmpty(groups))
						CacheStorage.insert(DBHelper.GROUPS_TABLE, messages.get(0).groups);
				}

				@Override
				public void done() {
					EventBus.getDefault().postSticky(MemoryCache.getUser(UserConfig.userId));
					createAdapter(messages, offset);
					refreshLayout.setRefreshing(false);

					if (!messages.isEmpty()) {
						loading = false;
					}
				}

				@Override
				public void error(Exception e) {
					refreshLayout.setRefreshing(false);
				}
			});
    }

	private void openChat(int position, boolean fromStart) {
        VKConversation c = adapter.getValues().get(position);
		VKUser user = CacheStorage.getUser(c.last.peerId);
		VKGroup g = CacheStorage.getGroup(VKGroup.toGroupId(c.last.peerId));
        
        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", adapter.getTitle(c, user, g));
        intent.putExtra("photo", adapter.getPhoto(c, user, g));
		intent.putExtra("conversation", c);
		intent.putExtra("peer_id", c.last.peerId);
		intent.putExtra("can_write", c.can_write);
		
		if (!c.can_write) {
			intent.putExtra("reason", c.reason);
		}
		
        startActivity(intent);
    }
}


