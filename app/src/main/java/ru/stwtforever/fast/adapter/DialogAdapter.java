package ru.stwtforever.fast.adapter;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.support.v4.content.*;
import android.support.v7.widget.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.widget.*;
import com.squareup.picasso.*;
import java.util.*;
import org.greenrobot.eventbus.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.cls.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.db.*;
import ru.stwtforever.fast.helper.*;
import ru.stwtforever.fast.util.*;

import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.fragment.*;

public class DialogAdapter extends BaseRecyclerAdapter<VKConversation, DialogAdapter.ViewHolder> {
    
	private int position;
	private RecyclerView list;
	private LinearLayoutManager manager;

	private OnItemListener listener;

    public DialogAdapter(FragmentDialogs fr, ArrayList<VKConversation> dialogs) {
		super(fr.getActivity(), dialogs);
		list = fr.getList();
		manager =  (LinearLayoutManager) list.getLayoutManager();
		
		EventBus.getDefault().register(this);
    }
	
	@Subscribe (threadMode = ThreadMode.MAIN)
	public void onReceive(Object[] data) {
		if (ArrayUtil.isEmpty(data)) return;
		
		int type = data[0];
		
		switch(type) {
			case 3:
				readMessage((int) data[1]);
				break;
			case 4:
				addMessage((VKConversation) data[1]);
				break;
			case 5:
				editMessage((VKMessage) data[1]);
				break;
			case 8:
				setUserOnline(data[1], data[2]);
				break;
			case 9:
				setUserOffline(data[1], data[2], data[3]);
				break;
		}
	}
	
	private void setUserOnline(int user_id, int time) {
		VKUser u = new VKUser();
		u.id = user_id;
		
		int i = searchPosition(u);
		if (i == -1) return;
		
		VKMessage msg = getValues().get(i).last;
		VKUser user = CacheStorage.getUser(msg.fromId);
		if (user == null) return;
		
		user.online = true;
		user.last_seen = time;
		
		notifyItemChanged(i);
	}
	
	private void setUserOffline(int user_id, int time, int timeout) {
		VKUser us = CacheStorage.getUser(user_id);
		if (us == null) us = VKUser.EMPTY;
		
		Toast.makeText(context, "user " + us.toString() + " offline", Toast.LENGTH_SHORT).show();
		VKUser u = new VKUser();
		u.id = user_id;
		
		int i = searchPosition(u);
		Toast.makeText(context, String.valueOf(i), Toast.LENGTH_SHORT).show();
		if (i == -1) return;

		VKMessage msg = getValues().get(i).last;
		VKUser user = CacheStorage.getUser(msg.fromId);
		if (user == null) return;

		user.online = false;
		user.last_seen = time;

		notifyItemChanged(i);
	}
	
	private int searchPosition(VKUser user) {
		for (int i = 0; i < getValues().size(); i++) {
			VKConversation msg = getValues().get(i);
			if (!msg.last.isUser()) return -1;
			
			if (msg.last.fromId == user.id) return i;
		}
		
		return -1;
	}

	private void addMessage(VKConversation conversation) {
		int index = searchMessageIndex(conversation.last.peerId);
        if (index >= 0) {
			VKConversation current = getItem(index);
			
			current.last.id = conversation.last.id;
			current.last.flag = conversation.last.flag;
			current.last.peerId = conversation.last.peerId;
			current.last.date = conversation.last.date;
			current.last.text = conversation.last.text;
			current.last.read = current.read;
			current.last.out = conversation.last.out;
			current.last.fromId = conversation.last.fromId;
			
			current.read = conversation.read;
			current.type = conversation.type;
			current.unread++;
			
			if (current.last.out) {
				current.unread = 0;
				current.read = false;
			}
			
			
			int first = manager.findFirstCompletelyVisibleItemPosition();
			boolean scroll = false;
			
			if (first <= 2)
				scroll = true;
			
			getValues().remove(index);
			getValues().add(0, current);
			notifyItemRemoved(index);
			notifyItemInserted(0);
			
			if (scroll) {
				list.smoothScrollToPosition(0);
			}
        } else {
			getValues().add(0, conversation);
			notifyItemInserted(0);
			
			int first = manager.findFirstCompletelyVisibleItemPosition();
			boolean scroll = false;

			if (first <= 2)
				scroll = true;
				
			if (scroll) {
				list.smoothScrollToPosition(0);
			}
		}
	}

	private void readMessage(int id) {
		int position = searchPosition(id);

		if (position == -1) return;

		VKConversation current = getItem(position);
		current.read = true;
		current.unread = 0;

		notifyItemChanged(position);
	}

	private void editMessage(VKMessage edited) {
		int position = searchPosition(edited.id);
		if (position == -1) return;
		
		VKConversation current = getValues().get(position);
		VKMessage last = current.last;
		last.mask = edited.mask;
		last.text = edited.text;
		last.update_time = edited.update_time;
		last.attachments = edited.attachments;
		
		notifyItemChanged(position);
	}
	
	private int searchPosition(int mId) {
		for (int i = 0; i < getValues().size(); i++) {
			VKMessage m = getValues().get(i).last;
			if (m.id == mId) {
				return i;
			}
		}
		
		return -1;
	}

	private int searchPosition(VKConversation m) {
		for (int i = 0; i < getValues().size(); i++) {
			VKConversation msg = getValues().get(i);
			if (msg.last.id == m.last.id) {
				return i;
			}
		}

		return -1;
	}

	private void initListener(View v, final int position) {
		v.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					listener.OnItemClick(v, position);
				}
			});
		v.setOnLongClickListener(new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					listener.onItemLongClick(v, position);
					return true;
				}
			});
	}

	public void setListener(OnItemListener listener) {
		this.listener = listener;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		ImageView avatar;
		ImageView avatar_small;
		ImageView online;
		ImageView out;

		TextView title;
		TextView body;
		TextView date;
		TextView counter;

		LinearLayout container;

		DialogAdapter adapter;
		Context context;
		
		Drawable p_user, p_users;
		
		int c_pushes_enabled;
		int c_pushes_disabled;

		ViewHolder(Context context, DialogAdapter adapter, View v) {
			super(v);

			this.context = context;
			this.adapter = adapter;
			
			UserConfig.updateUser();

			c_pushes_enabled = ThemeManager.getAccent();
			c_pushes_disabled = ThemeManager.isDark() ? 0xff454545 : 0xffcccccc;
			
			p_user = adapter.getDrawable(R.drawable.placeholder_user);
			p_users = adapter.getDrawable(R.drawable.placeholder_users);

			avatar = v.findViewById(R.id.avatar);
			avatar_small = v.findViewById(R.id.avatar_small);
			online = v.findViewById(R.id.online);
			out = v.findViewById(R.id.icon_out_message);

			title = v.findViewById(R.id.title);
			body = v.findViewById(R.id.body);
			date = v.findViewById(R.id.date);
			counter = v.findViewById(R.id.counter);

			container = v.findViewById(R.id.container);

			GradientDrawable gd = new GradientDrawable();
			gd.setColor(ThemeManager.getAccent());
			gd.setCornerRadius(60);

			counter.setBackground(gd);
		}

		public void bind(final int position) {
			VKConversation item = adapter.getItem(position);
			VKMessage last = item.last;
			VKGroup group = adapter.searchGroup(last.fromId);
			VKGroup peerGroup = adapter.searchGroup(last.peerId);

			VKUser user = adapter.searchUser(last.fromId);
			VKUser peerUser = adapter.searchUser(last.peerId);
			
			FontHelper.setFont(title, FontHelper.PS_REGULAR);

			counter.setText(item.unread > 0 ? String.valueOf(item.unread) : "");
			date.setText(Utils.dateFormatter.format(last.date * 1000));
			
			counter.getBackground().setTint(item.isNotificationsDisabled() && item.no_sound ? c_pushes_disabled : c_pushes_enabled);
			
			body.setText(last.text);

			title.setText(adapter.getTitle(item, peerUser, peerGroup));
			
			avatar_small.setVisibility((!item.isChat() && !last.out) ? View.GONE : View.VISIBLE);

			String peerAvatar = "";

			if (item.isGroup()) {
				peerAvatar = peerGroup.photo_100;
			} else if (item.isUser()) {
				peerAvatar = peerUser.photo_100;
			} else {
				peerAvatar = item.photo_100;
			}

			String fromAvatar = "";
			
			if (last.out && !item.isChat()) {
				fromAvatar = UserConfig.user.photo_100;
			} else
				fromAvatar = item.isFromUser() ? user.photo_100 : group.photo_100;

			if (TextUtils.isEmpty(fromAvatar)) {
				avatar_small.setImageDrawable(p_user);
			} else {
				Picasso.get()
				.load(fromAvatar)
				.priority(Picasso.Priority.HIGH)
				.placeholder(p_user)
				.into(avatar_small);
			}

			if (TextUtils.isEmpty(peerAvatar)) {
				avatar.setImageDrawable(item.isChat() ? p_users : p_user);
			} else {
				Picasso.get()
				.load(peerAvatar)
				.priority(Picasso.Priority.HIGH)
				.placeholder(item.isChat() ? p_users : p_user)
				.into(avatar);
			}

			body.setTextColor(!ThemeManager.isDark() ? 0x90000000 : 0x90ffffff);

			if (TextUtils.isEmpty(last.actionType)) {
				if ((last.attachments != null
					|| !ArrayUtil.isEmpty(last.fwd_messages))
					&& TextUtils.isEmpty(last.text)) {
					String body_ = VKUtils.getAttachmentBody(item.last.attachments, item.last.fwd_messages);
					
					String r = "<b>" + body_ + "</b>";
					SpannableString span = new SpannableString(Html.fromHtml(r));
					span.setSpan(new ForegroundColorSpan(ThemeManager.getAccent()), 0, body_.length(), 0);

					body.append(span);
				}
			} else {
				String body_ = VKUtils.getActionBody(last);

				body.setTextColor(ThemeManager.getAccent());
				body.append(Html.fromHtml(body_));
			}
			
			counter.setVisibility(!last.out && item.unread > 0 ? View.VISIBLE : View.GONE);
			out.setVisibility(last.out && !item.read ? View.VISIBLE : View.GONE);

			if (item.isGroup() || item.isChat()) {
				online.setVisibility(View.GONE);
			} else {
				if (peerUser.online) {
					online.setVisibility(View.VISIBLE);
				} else {
					online.setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = inflater.inflate(R.layout.fragment_dialogs_list, parent, false);
		return new ViewHolder(context, this, v);
	}

	@Override
	public void onBindViewHolder(DialogAdapter.ViewHolder holder, int position) {
		this.position = position;
		initListener(holder.itemView, position);
		holder.bind(position);
	}
	
	public int getCurrentPosition() {
        return position;
    }

	public int getMessagesCount() {
		return getValues().size();
	}

    public void add(ArrayList<VKConversation> messages) {
        this.getValues().addAll(messages);
    }

    public void remove(int position) {
        getValues().remove(position);
    }


    public String getTitle(VKConversation item, VKUser user, VKGroup group) {
		if (item == null) return null;

		if (item.isGroup()) {
			if (group != null)
				return TextUtils.isEmpty(group.name) ? "" : group.name;
		} else if (item.isUser()) {
			if (user != null)
				return TextUtils.isEmpty(user.toString()) ? "" : user.toString();
		} else {
			return TextUtils.isEmpty(item.title) ? "" : item.title;
		}

		return "";
    }

    public String getPhoto(VKConversation item, VKUser user, VKGroup group) {
		if (item == null) return null;

		if (item.isUser()) {
			if (user != null)
				return user.photo_100;
		} else if (item.isGroup()) {
			if (group != null)
				return group.photo_100;
		} else {
			return item.photo_100;
		}

		return "";
    }

    public void changeItems(ArrayList<VKConversation> messages) {
        if (!ArrayUtil.isEmpty(messages)) {
            this.getValues().clear();
            this.getValues().addAll(messages);
        }
    }

    public VKUser searchUser(int id) {
        VKUser user = MemoryCache.getUser(id);
        if (user == null) {
            user = VKUser.EMPTY;
        }
        return user;
    }

    public VKGroup searchGroup(int id) {
		VKGroup group = MemoryCache.getGroup(VKGroup.toGroupId(id));
        if (group == null) {
            group = VKGroup.EMPTY;
        }
        return group;
    }

    public int searchMessageIndex(int peerId) {
        for (int i = 0; i < getValues().size(); i++) {
            VKConversation conv = getItem(i);
            if (conv.last.peerId == peerId) {
				return i;
			}
        }
        return -1;
    }

    public VKConversation searchMessage(int id) {
		for (VKConversation c : getValues()) {
			if (c.last.id == id) {
				return c;
			}
		}
        return null;
    }

    public void destroy() {
        getValues().clear();
		EventBus.getDefault().unregister(this);
    }

    public static Drawable getOnlineIndicator(Context context, VKUser user) {
        return ContextCompat.getDrawable(context, R.drawable.ic_online_circle);
    }
}
