package ru.stwtforever.fast;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ru.stwtforever.fast.adapter.MessageAdapter;
import ru.stwtforever.fast.adapter.RecyclerAdapter;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.api.VKApi;
import ru.stwtforever.fast.api.VKUtils;
import ru.stwtforever.fast.api.model.VKConversation;
import ru.stwtforever.fast.api.model.VKGroup;
import ru.stwtforever.fast.api.model.VKMessage;
import ru.stwtforever.fast.api.model.VKUser;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.LowThread;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.database.CacheStorage;
import ru.stwtforever.fast.database.DatabaseHelper;
import ru.stwtforever.fast.fragment.FragmentSettings;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class MessagesActivity extends AppCompatActivity implements RecyclerAdapter.OnItemClickListener, TextWatcher {

    private static final int MESSAGES_COUNT = 60;

    private Toolbar toolbar;
    private RecyclerView recycler;
    private LinearLayoutManager layoutManager;

    private ImageButton smiles, send, unpin;
    private EditText message;
    private ProgressBar bar;
    private LinearLayout chatPanel, pinnedContainer;
    private View noItems, pLine;
    private TextView pName, pDate, pText;

    private MessageAdapter adapter;

    private boolean loading, canWrite, editing, typing;
    private int cantWriteReason = -1, peerId, membersCount;
    private String messageText, reasonText, title, photo;

    private Timer timer;

    private VKMessage pinned, last;
    private VKConversation conversation;
    private VKUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        initViews();
        getIntentData();
        showPinned(pinned);
        checkCanWrite();

        if (ThemeManager.isDark())
            if (chatPanel.getBackground() != null) {
                chatPanel.getBackground().setColorFilter(ThemeManager.getBackground(), PorterDuff.Mode.MULTIPLY);
            }

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recycler.setLayoutManager(layoutManager);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(title);

        message.addTextChangedListener(this);


        smiles.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String template = Utils.getPrefs().getString(FragmentSettings.KEY_MESSAGE_TEMPLATE, FragmentSettings.DEFAULT_TEMPLATE_VALUE);
                if (message.getText().toString().trim().isEmpty()) {
                    message.setText(template);
                } else {
                    message.append(template);
                }
                message.setSelection(message.getText().length());
                return true;
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ThemeManager.getAccent());
        gd.setCornerRadius(100f);

        send.setBackground(gd);
        send.setImageResource(R.drawable.md_mic);

        getCachedMessages();
    }

    private void initViews() {
        chatPanel = findViewById(R.id.chat_panel);
        smiles = findViewById(R.id.smiles);
        toolbar = findViewById(R.id.tb);
        recycler = findViewById(R.id.list);
        send = findViewById(R.id.send);
        message = findViewById(R.id.message);
        noItems = findViewById(R.id.no_items_layout);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        this.conversation = (VKConversation) intent.getSerializableExtra("conversation");
        this.title = intent.getStringExtra("title");
        this.photo = intent.getStringExtra("photo");
        this.peerId = intent.getIntExtra("peer_id", -1);
        this.cantWriteReason = intent.getIntExtra("reason", -1);
        this.canWrite = intent.getBooleanExtra("can_write", false);
        this.reasonText = canWrite ? "" : VKUtils.getErrorReason(cantWriteReason);

        if (conversation != null) {
            this.last = conversation.last;
            this.membersCount = conversation.membersCount;
            this.pinned = conversation.pinned;
        }

        checkPinnedExists();
    }

    private void checkCanWrite() {
        send.setEnabled(true);
        smiles.setEnabled(true);
        message.setEnabled(true);
        message.setHint(R.string.tap_to_type);
        message.setText("");

        if (cantWriteReason <= 0) return;
        if (!canWrite) {
            send.setEnabled(false);
            smiles.setEnabled(false);
            message.setEnabled(false);
            message.setHint(VKUtils.getErrorReason(cantWriteReason));
            message.setText("");
        }
    }

    private void showPinned(final VKMessage pinned) {
        pinnedContainer = findViewById(R.id.pinned_msg_container);
        pName = pinnedContainer.findViewById(R.id.name);
        pDate = pinnedContainer.findViewById(R.id.date);
        pText = pinnedContainer.findViewById(R.id.message);
        unpin = pinnedContainer.findViewById(R.id.unpin);
        pLine = findViewById(R.id.line);

        if (pinned == null) {
            pLine.setVisibility(View.GONE);
            pinnedContainer.setVisibility(View.GONE);
            checkPinnedExists();
            return;
        }

        checkPinnedExists();

        pLine.setVisibility(View.VISIBLE);
        pinnedContainer.setVisibility(View.VISIBLE);

        pinnedContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter == null) return;
                if (adapter.contains(pinned.id)) {
                    recycler.scrollToPosition(adapter.findPosition(pinned.id));
                } else {
                    return;
                }
            }
        });

        VKUser user = CacheStorage.getUser(pinned.fromId);
        if (user == null) user = VKUser.EMPTY;

        pName.setText(user.toString().trim());
        pDate.setText(Utils.dateFormatter.format(pinned.date * 1000));

        pText.setText(pinned.text);

        unpin.setVisibility(conversation.can_change_pin ? View.VISIBLE : View.GONE);
        unpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                return;
            }
        });

        if ((pinned.attachments != null || !ArrayUtil.isEmpty(pinned.fwd_messages)) && TextUtils.isEmpty(pinned.text)) {

            String body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages);

            String r = "<b>" + body + "</b>";
            SpannableString span = new SpannableString(Html.fromHtml(r));
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, body.length(), 0);

            pText.append(span);
        }
    }


    private void setTyping() {
        if (Utils.getPrefs().getBoolean(FragmentSettings.KEY_HIDE_TYPING, false)) return;

        typing = true;
        new LowThread(new Runnable() {
            @Override
            public void run() {
                try {
                    VKApi.messages().setActivity().peerId(peerId).execute();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timer = new Timer();
                            timer.schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    typing = false;
                                }


                            }, 10000);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    private void applyStyles(boolean isEdit) {
        if (isEdit) {
            send.setImageResource(R.drawable.md_done);
        } else {
            String s = message.getText().toString();

            applyBtnStyle(s.trim().isEmpty());
        }
    }

    private void applyBtnStyle(boolean isTextEmpty) {
        if (isTextEmpty) {
            send.setImageResource(R.drawable.md_mic);
            if (!editing)
                send.setOnClickListener(recordClick);
        } else {
            send.setImageResource(R.drawable.md_send);
            if (!editing)
                send.setOnClickListener(recordClick);
        }
    }

    private void checkPinnedExists() {
        findViewById(R.id.space).setVisibility(pinned == null ? View.GONE : View.VISIBLE);
    }

    private void checkCount() {
        noItems.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setNotEditing() {
        editing = false;
        applyStyles(editing);
        message.setText("");
        checkHovered();
    }

    private void checkHovered() {
        int position = -1;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            VKMessage m = adapter.getItem(i);
            if (m.isSelected()) position = i;
        }

        if (position == -1) return;

        adapter.getItem(position).setSelected(false);
        adapter.notifyItemChanged(position, null);
    }

    private void getCachedMessages() {
        
    }

    private View.OnClickListener sendClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s = message.getText().toString();
            if (!s.trim().isEmpty()) {
                messageText = s;

                sendMessage();
                message.setText("");
            }
        }
    };

    private View.OnClickListener recordClick = null;

    private void sendMessage() {
        if (messageText.trim().isEmpty()) return;

        final VKMessage msg = new VKMessage();
        msg.text = messageText.trim();
        msg.fromId = UserConfig.userId;
        msg.peerId = peerId;
        msg.date = Calendar.getInstance().getTimeInMillis();
        msg.out = true;
        msg.status = VKMessage.STATUS_SENDING;
        msg.randomId = new Random().nextInt();

        adapter.add(msg);
        adapter.notifyItemInserted(adapter.getItemCount() - 1);
        recycler.smoothScrollToPosition(adapter.getItemCount() - 1);

        final int size = adapter.getItemCount();

        ThreadExecutor.execute(new AsyncCallback(this) {

            int id = -1;

            @Override
            public void ready() throws Exception {
                id = VKApi.messages().send().peerId(peerId).randomId(msg.randomId).text(messageText.trim()).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (typing) {
                    typing = false;
                    if (timer != null)
                        timer.cancel();
                }

                checkCount();

                adapter.getItem(adapter.getPosition(msg)).id = id;

                msg.status = VKMessage.STATUS_SENT;

                if (adapter.getItemCount() > size) {
                    int i = adapter.getPosition(msg);
                    adapter.remove(i);
                    adapter.add(msg);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void error(Exception e) {
                msg.status = VKMessage.STATUS_ERROR;
                adapter.notifyItemChanged(adapter.getPosition(msg), null);
            }
        });

    }

    private void createAdapter(ArrayList<VKMessage> messages) {
        if (ArrayUtil.isEmpty(messages)) {
            checkCount();
            return;
        }

        if (adapter == null) {
            adapter = new MessageAdapter(this, messages, peerId);
            recycler.setAdapter(adapter);
            recycler.scrollToPosition(adapter.getItemCount());
            adapter.setOnItemClickListener(this);

            checkCount();
            return;
        }

        adapter.changeItems(messages);
        adapter.notifyDataSetChanged();

        checkCount();
    }

    public void handleNewMessage() {
        checkCount();
        recycler.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    private void getMessages() {
        ArrayList<VKMessage> messages = CacheStorage.getMessages(peerId);
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages);
        }

        if (Utils.hasConnection()) {
            getHistory(0, MESSAGES_COUNT);
        }
    }

    private void getHistory(final int offset, final int count) {
        loading = true;
        getSupportActionBar().setSubtitle(getString(R.string.loading));

        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKMessage> messages = new ArrayList<>();

            @Override
            public void ready() throws Exception {
                messages = VKApi.messages().getHistory()
                        .peerId(peerId)
                        .extended(true)
                        .fields(VKUser.FIELDS_DEFAULT)
                        .offset(offset)
                        .count(count).execute(VKMessage.class);

                Collections.reverse(messages);

                ArrayList<VKUser> users = messages.get(0).history_users;
                ArrayList<VKGroup> groups = messages.get(0).history_groups;

                if (!ArrayUtil.isEmpty(users)) {
                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
                }

                if (!ArrayUtil.isEmpty(groups)) {
                    CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, groups);
                }

                if (!ArrayUtil.isEmpty(messages)) {
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, messages);
                }

                loading = messages.isEmpty();
            }

            @Override
            public void done() {
                createAdapter(messages);

                getSupportActionBar().setSubtitle(getSubtitle());
            }

            @Override
            public void error(Exception e) {
                getSupportActionBar().setSubtitle(getSubtitle());
            }
        });
    }

    private String getSubtitle() {
        int type = 2;
        if (peerId > 2000000000) type = 0;
        if (peerId < -1) type = 1;

        if (conversation != null) {
            if (conversation.isChannel()) {
                type = 3;
            }
        }

        switch (type) {
            case 0:
                return membersCount > 0 ? getString(R.string.members_count, membersCount) : "";
            case 1:
                return getString(R.string.group);
            case 2:
                currentUser = CacheStorage.getUser(peerId);
                return getUserSubtitle(currentUser);
            case 3:
                return getString(R.string.channel) + " • " + getString(R.string.members_count, membersCount);

        }

        return "";
    }

    private String getUserSubtitle(VKUser user) {
        if (user == null) return "";
        if (user.online) return getString(R.string.online);

        return getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w, Utils.dateFormatter.format(user.last_seen * 1000));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat_history, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (editing) setNotEditing();
                else onBackPressed();
                break;
            case R.id.menuUpdate:
                if (adapter == null) return false;
                if (!loading && !editing) {
                    adapter.clear();
                    getHistory(0, MESSAGES_COUNT);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (editing) {
            setNotEditing();
        } else
            super.onBackPressed();
    }

    @Override
    public void onItemClick(View v, int position) {
        VKMessage item = adapter.getItem(position);

        if (!TextUtils.isEmpty(item.actionType)) return;

        showAlertDialog(position);
    }

    private void showAlertDialog(int position) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!typing) {
            setTyping();
        }
        if (!editing) {
            applyBtnStyle(s.toString().trim().isEmpty());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
