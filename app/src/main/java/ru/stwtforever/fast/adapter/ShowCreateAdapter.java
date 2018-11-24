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
import ru.stwtforever.fast.api.*;
import ru.stwtforever.fast.api.model.*;
import ru.stwtforever.fast.view.*;

public class ShowCreateAdapter extends BaseRecyclerAdapter<VKUser, ShowCreateAdapter.ViewHolder> {
	
	public ShowCreateAdapter(Context context, ArrayList<VKUser> users) {
		super(context, users);
	}

	static class ViewHolder extends RecyclerView.ViewHolder {

		CircleImageView avatar;
		CircleImageView online;
		
		ImageButton remove;

		TextView name;
		TextView invited_by;

		Context context;
		ShowCreateAdapter adapter;

		public ViewHolder(Context context, ShowCreateAdapter adapter, View v) {
			super(v);
			this.context = context;
			this.adapter = adapter;
			
			remove = v.findViewById(R.id.remove);

			avatar = v.findViewById(R.id.avatar);
			online = v.findViewById(R.id.online);

			name = v.findViewById(R.id.name);
			invited_by = v.findViewById(R.id.last_seen);
		}

		public void bind(final int position) {
			VKUser user = adapter.getItem(position);

			name.setText(user.toString());

			online.setVisibility(user.online ? View.VISIBLE : View.GONE);

			String text = String.format(adapter.getString(R.string.invited_by), UserConfig.user.toString()); 
			invited_by.setText(text);
			
			if (TextUtils.isEmpty(user.photo_100)) {
				avatar.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
			} else {
				Picasso.get()
					.load(user.photo_100)
					.placeholder(new ColorDrawable(Color.TRANSPARENT))
					.into(avatar);
			}
			
			remove.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View p1) {
						if (adapter.getValues().size() >= 2) {
							adapter.getValues().remove(position);
							adapter.notifyItemRemoved(position);
							adapter.notifyItemRangeChanged(0, adapter.getValues().size());
						} else {
							((ShowCreateChatActivity) context).finish();
						}
					}
				
			});
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = inflater.inflate(R.layout.activity_create_show_list, parent, false);
		return new ViewHolder(context, this, v);
	}

	@Override
	public void onBindViewHolder(ShowCreateAdapter.ViewHolder holder, int position) {
		holder.bind(position);
	}
}
