package ru.stwtforever.fast.adapter;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.support.v7.widget.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.squareup.picasso.*;
import java.util.*;
import ru.stwtforever.fast.*;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.cls.*;
import ru.stwtforever.fast.util.*;
import ru.stwtforever.fast.view.*;

import ru.stwtforever.fast.util.Utils;

public class CreateChatAdapter extends BaseRecyclerAdapter<VKUser, CreateChatAdapter.ViewHolder> {
	
	private OnItemListener listener;
	
	public int position;
	
	public CreateChatAdapter(Context context, ArrayList<VKUser> users) {
		super(context, users);
	}
	
	public void add(ArrayList<VKUser> friends) {
        this.getValues().addAll(friends);
    }

    public void remove(int position) {
        getValues().remove(position);
    }

	public void changeItems(ArrayList<VKUser> friends) {
        if (!ArrayUtil.isEmpty(friends)) {
            this.getValues().clear();
            this.getValues().addAll(friends);
        }
    }
	
	public boolean isSelected(int position) {
		if (ArrayUtil.isEmpty(getValues())) return false;
		return getValues().get(position).isSelected();
	}
	
	public void setSelected(int i, boolean b) {
		if (ArrayUtil.isEmpty(getValues())) return;
		getValues().get(i).setSelected(b);
	}
	
	public HashMap<Integer, VKUser> getSelectedPositions() {
		HashMap<Integer, VKUser> sels = new HashMap<>();
		
		for (int i = 0; i < getValues().size(); i++) {
			VKUser user = getValues().get(i);
			if (user.isSelected()) {
				sels.put(i, user);
			}
		}
		
		return sels;
	}
	
	public void clearSelect() {
		for (int i = 0; i < getValues().size(); i++) {
			VKUser u = getValues().get(i);
			if (u.isSelected()) {
				u.setSelected(false);
				notifyItemChanged(i);
			}
		}
	}
	
	public int getSelectedCount() {
		int count = 0;
		
		for (VKUser u : getValues()) {
			if (u.isSelected()) count++;
		}
		
		return count;
	}
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		
		CircleImageView avatar;
		CircleImageView online;
		
		TextView name;
		TextView lastSeen;
		
		LinearLayout root;
		
		CheckBox selected;
		
		Drawable placeholder;
		
		CreateChatAdapter adapter;
		
		public ViewHolder(CreateChatAdapter adapter, View v) {
			super(v);
			this.adapter = adapter;
			
			selected = v.findViewById(R.id.selected);
			placeholder = adapter.getDrawable(R.drawable.placeholder_user);
			root = v.findViewById(R.id.root);
			
			avatar = v.findViewById(R.id.avatar);
			online = v.findViewById(R.id.online);
			
			name = v.findViewById(R.id.name);
			lastSeen = v.findViewById(R.id.last_seen);
		}
		
		public void bind(int position) {
			VKUser user = adapter.getItem(position);
		
			name.setText(user.toString());
			
			if (user.online) {
				lastSeen.setVisibility(View.GONE);
				online.setVisibility(View.VISIBLE);
			} else {
				lastSeen.setVisibility(View.VISIBLE);
				online.setVisibility(View.GONE);
			}
			
			selected.setChecked(user.isSelected());
			
			String seen_text = adapter.context.getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w);
			
			String seen = String.format(seen_text, Utils.dateFormatter.format(user.last_seen * 1000));
			
			if (lastSeen.getVisibility() == View.VISIBLE) {
				lastSeen.setText(seen);
			} else {
				lastSeen.setText("");
			}
			
			if (TextUtils.isEmpty(user.photo_100)) {
				avatar.setImageDrawable(placeholder);
			} else {
				Picasso.get()
				.load(user.photo_100)
				.priority(Picasso.Priority.HIGH)
				.placeholder(placeholder)
				.into(avatar);
			}
		}
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = inflater.inflate(R.layout.activity_create_chat_list, parent, false);
		return new ViewHolder(this, v);
	}

	@Override
	public void onBindViewHolder(CreateChatAdapter.ViewHolder holder, int position) {
		this.position = position;
		initListener(holder.itemView, position);
		holder.bind(position);
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
	
	public int getPosition() {
		return position;
	}

	@Override
	public int getItemCount() {
		return super.getItemCount();
	}
	
	@Override
	public long getItemId(int position) {
		return super.getItemId(position);
	}

	public void setListener(OnItemListener listener) {
		this.listener = listener;
	}
}
