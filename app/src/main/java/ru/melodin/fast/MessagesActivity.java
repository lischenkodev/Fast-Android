package ru.melodin.fast;

import android.app.AlarmManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.checkbox.MaterialCheckBox;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import ru.melodin.fast.adapter.MessageAdapter;
import ru.melodin.fast.adapter.RecyclerAdapter;
import ru.melodin.fast.api.LongPollEvents;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.VKUtil;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKGroup;
import ru.melodin.fast.api.model.VKMessage;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.AppGlobal;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.LowThread;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.Util;
import ru.melodin.fast.util.ViewUtil;

public class MessagesActivity extends AppCompatActivity implements RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener, TextWatcher {

    private static final int MESSAGES_COUNT = 30;

    private Random random = new Random();

    private Drawable iconSend;
    private Drawable iconMic;
    private Drawable iconDone;
    private Drawable iconTrash;

    private Toolbar tb;
    private AppBarLayout appBar;
    private RecyclerView list;

    private AppCompatImageButton smiles, send, unpin;
    private AppCompatEditText message;
    private LinearLayout chatPanel;
    private FrameLayout pinnedContainer;
    private View empty, pinnedShadow;
    private TextView pName, pDate, pText;

    private MessageAdapter adapter;

    private boolean loading, canWrite, typing;
    private int cantWriteReason = -1, peerId, membersCount;
    private VKConversation.Reason reason;
    private String messageText;
    private String title;

    private Timer timer;

    private VKMessage pinned, edited;
    private int editingPosition;
    private VKConversation conversation;

    private boolean resumed, editing;

    private VKMessage notRead;

    private LinearLayoutManager layoutManager;

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
    private View.OnClickListener doneClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ViewUtil.hideKeyboard(message);
            editMessage(edited);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        ViewUtil.applyWindowStyles(getWindow());

        iconSend = ContextCompat.getDrawable(this, R.drawable.md_send);
        iconMic = ContextCompat.getDrawable(this, R.drawable.md_mic);
        iconDone = ContextCompat.getDrawable(this, R.drawable.md_done);
        iconTrash = ContextCompat.getDrawable(this, R.drawable.ic_trash);

        initViews();
        getIntentData();
        showPinned(pinned);
        getConversation(peerId);
        checkCanWrite();

        if (ThemeManager.isDark())
            if (chatPanel.getBackground() != null) {
                chatPanel.getBackground().setColorFilter(ColorUtil.lightenColor(ThemeManager.getBackground()), PorterDuff.Mode.MULTIPLY);
            }

        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        layoutManager.setStackFromEnd(true);

        list.setItemViewCacheSize(20);
        list.setDrawingCacheEnabled(true);
        list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        list.setLayoutManager(layoutManager);

        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(title);

        updateToolbar();

        message.addTextChangedListener(this);

        smiles.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String template = AppGlobal.preferences.getString(FragmentSettings.KEY_MESSAGE_TEMPLATE, FragmentSettings.DEFAULT_TEMPLATE_VALUE);
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
        gd.setCornerRadius(200f);

        send.setBackground(gd);

        getCachedHistory();

        if (Util.hasConnection())
            getHistory(0, MESSAGES_COUNT);
    }

    private void getConversation(final int peerId) {
        ThreadExecutor.execute(new AsyncCallback(this) {
            VKMessage last;

            @Override
            public void ready() throws Exception {
                conversation =
                        VKApi.messages()
                                .getConversationsById()
                                .peerIds(peerId)
                                .extended(true)
                                .fields(VKUser.FIELDS_DEFAULT)
                                .execute(VKConversation.class)
                                .get(0);
                if (conversation.getLastMessageId() != 0)
                    last =
                            VKApi.messages()
                                    .getById()
                                    .messageIds(conversation.getLastMessageId())
                                    .extended(false)
                                    .execute(VKMessage.class)
                                    .get(0);

                conversation.setLast(last);
            }

            @Override
            public void done() {
                invalidateOptionsMenu();
                if (conversation.getLastMessageId() != 0)
                    CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load dialog", Log.getStackTraceString(e));
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (notRead != null) {
            adapter.readNewMessage(notRead);
            notRead = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onReceive(Object[] data) {
        String key = (String) data[0];

        switch (key) {
            case LongPollEvents.KEY_USER_OFFLINE:
            case LongPollEvents.KEY_USER_ONLINE:
                setUserOnline((int) data[1]);
                break;
            case LongPollEvents.KEY_MESSAGE_CLEAR_FLAGS:
                if (adapter != null)
                    adapter.handleClearFlags(data);
                break;
            case LongPollEvents.KEY_MESSAGE_SET_FLAGS:
                if (adapter != null)
                    adapter.handleSetFlags(data);
                break;
            case LongPollEvents.KEY_MESSAGE_NEW:
                VKConversation conversation = (VKConversation) data[1];

                if (adapter == null) return;

                adapter.addMessage(conversation.getLast(), true);

                int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisibleItem >= adapter.getItemCount() - 4) {
                    list.scrollToPosition(adapter.getItemCount() - 1);
                }

                if (!conversation.getLast().isOut() && conversation.getLast().getPeerId() == peerId && !AppGlobal.preferences.getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
                    if (!resumed) {
                        notRead = conversation.getLast();
                    } else {
                        adapter.readNewMessage(conversation.getLast());
                    }
                }

                break;
            case LongPollEvents.KEY_MESSAGE_EDIT:
                if (adapter != null)
                    adapter.editMessage((VKMessage) data[1]);
                break;
            case LongPollEvents.KEY_MESSAGE_UPDATE:
                adapter.updateMessage((VKMessage) data[1]);
                break;
        }
    }

    private void setUserOnline(int userId) {
        if (peerId == userId)
            getSupportActionBar().setSubtitle(getSubtitle());
    }

    private void initViews() {
        chatPanel = findViewById(R.id.chat_panel);
        smiles = findViewById(R.id.smiles);
        tb = findViewById(R.id.tb);
        appBar = (AppBarLayout) tb.getParent();
        list = findViewById(R.id.list);
        send = findViewById(R.id.send);
        message = findViewById(R.id.message_edit_text);
        empty = findViewById(R.id.no_items_layout);

        pinnedContainer = findViewById(R.id.pinned_msg_container);
        pinnedShadow = findViewById(R.id.pinned_shadow);
        pName = pinnedContainer.findViewById(R.id.name);
        pDate = pinnedContainer.findViewById(R.id.date);
        pText = pinnedContainer.findViewById(R.id.message);
        unpin = pinnedContainer.findViewById(R.id.unpin);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        conversation = (VKConversation) intent.getSerializableExtra("conversation");
        title = intent.getStringExtra("title");
        peerId = intent.getIntExtra("peer_id", -1);
        cantWriteReason = intent.getIntExtra("reason", -1);
        canWrite = intent.getBooleanExtra("can_write", false);
        reason = VKConversation.getReason(cantWriteReason);

        if (conversation != null) {
            //last = conversation.last;
            membersCount = conversation.getMembersCount();
            pinned = conversation.getPinned();
        }
    }

    private void checkCanWrite() {
        send.setEnabled(true);
        smiles.setEnabled(true);
        message.setEnabled(true);
        message.setText("");
        if (cantWriteReason <= 0) return;
        if (!canWrite) {
            chatPanel.setEnabled(false);
            send.setEnabled(false);
            smiles.setEnabled(false);
            message.setEnabled(false);
            message.setText(VKUtil.getErrorReason(reason));
        }
    }

    private void showPinned(final VKMessage pinned) {
        if (pinned == null) {
            pinnedContainer.setVisibility(View.GONE);
            pinnedShadow.setVisibility(View.GONE);
            appBar.setElevation(0);
            return;
        }

        appBar.setElevation(8);
        pinnedContainer.setVisibility(View.VISIBLE);
        pinnedShadow.setVisibility(View.VISIBLE);

        pinnedContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter == null) return;
                if (adapter.contains(pinned.getId())) {
                    list.scrollToPosition(adapter.searchPosition(pinned.getId()));
                } else {
                    Toast.makeText(MessagesActivity.this, "Сообщения нет в списке, скоро запилю его отображение", Toast.LENGTH_SHORT).show();
                }
            }
        });

        VKUser user = CacheStorage.getUser(pinned.getFromId());
        if (user == null) user = VKUser.EMPTY;

        pName.setText(user.toString().trim());
        pDate.setText(Util.dateFormatter.format(pinned.getDate() * 1000));

        pText.setText(pinned.getText());

        unpin.setVisibility(conversation.isCanChangePin() ? View.VISIBLE : View.GONE);
        unpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmUnpinMessage();
            }
        });

        if ((pinned.getAttachments() != null || !ArrayUtil.isEmpty(pinned.getFwdMessages())) && TextUtils.isEmpty(pinned.getText())) {

            String body = VKUtil.getAttachmentBody(pinned.getAttachments(), pinned.getFwdMessages());

            String r = "<b>" + body + "</b>";
            SpannableString span = new SpannableString(Html.fromHtml(r));
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.accent)), 0, body.length(), 0);

            pText.append(span);
        }
    }

    private void showConfirmUnpinMessage() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                unpinMessage();
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void unpinMessage() {
        ThreadExecutor.execute(new AsyncCallback(this) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.messages().unpin().peerId(peerId).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (response == 1) {
                    pinned = null;
                    conversation.setPinned(null);
                    showPinned(null);

                    CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
                }
            }

            @Override
            public void error(Exception e) {
                Log.e("Error unpin", Log.getStackTraceString(e));
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setTyping() {
        if (AppGlobal.preferences.getBoolean(FragmentSettings.KEY_HIDE_TYPING, false)) return;

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

    public void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void sendMessage() {
        if (messageText.trim().isEmpty()) return;

        final VKMessage msg = new VKMessage();
        msg.setText(messageText.trim());
        msg.setFromId(UserConfig.userId);
        msg.setPeerId(peerId);
        msg.setAdded(true);
        msg.setDate(Calendar.getInstance().getTimeInMillis());
        msg.setOut(true);
        msg.setRandomId(random.nextInt());

        adapter.addMessage(msg, true);
        list.smoothScrollToPosition(adapter.getItemCount() - 1);

        final int position = adapter.getItemCount() - 1;

        final int size = adapter.getItemCount();

        ThreadExecutor.execute(new AsyncCallback(this) {

            int id = -1;

            @Override
            public void ready() throws Exception {
                id = VKApi.messages().send().randomId(msg.getRandomId()).text(messageText.trim()).peerId(peerId).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (typing) {
                    typing = false;
                    if (timer != null)
                        timer.cancel();
                }

                checkCount();

                adapter.getItem(position).setId(id);

                if (adapter.getItemCount() > size) {
                    int i = adapter.searchPosition(id);
                    adapter.remove(i);
                    adapter.add(msg);
                    adapter.notifyItemMoved(i, adapter.getItemCount() - 1);
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
                }
            }

            @Override
            public void error(Exception e) {
                Log.e("Error send message", Log.getStackTraceString(e));
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                //adapter.notifyItemChanged(position, -1);
            }
        });
    }

    public RecyclerView getRecyclerView() {
        return list;
    }

    private void createAdapter(ArrayList<VKMessage> messages, int offset) {
        if (ArrayUtil.isEmpty(messages)) {
            return;
        }

        if (adapter == null) {
            adapter = new MessageAdapter(this, messages, peerId);
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            list.setAdapter(adapter);

            if (adapter.getItemCount() > 0 && !list.isComputingLayout())
                list.scrollToPosition(adapter.getItemCount() - 1);

            checkCount();
            return;
        }

        if (offset > 0) {
            adapter.getValues().addAll(messages);
            adapter.notifyDataSetChanged();
            checkCount();
            return;
        }

        adapter.changeItems(messages);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);

        if (adapter.getItemCount() > 0 && !list.isComputingLayout())
            list.scrollToPosition(adapter.getItemCount() - 1);

        checkCount();
    }

    private void getCachedHistory() {
        ArrayList<VKMessage> messages = CacheStorage.getMessages(peerId);
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages, 0);
        }
    }

    private void getHistory(final int offset, final int count) {
        if (!Util.hasConnection()) return;
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
                        .count(count)
                        .execute(VKMessage.class);

                Collections.reverse(messages);

                ArrayList<VKUser> users = VKMessage.users;
                ArrayList<VKGroup> groups = VKMessage.groups;

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
                createAdapter(messages, offset);

                getSupportActionBar().setSubtitle(getSubtitle());
            }

            @Override
            public void error(Exception e) {
                loading = false;
                checkCount();
                getSupportActionBar().setSubtitle(getSubtitle());
                Toast.makeText(MessagesActivity.this, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateToolbar() {
        invalidateOptionsMenu();
        getSupportActionBar().setSubtitle(getSubtitle());
    }

    private String getSubtitle() {
        if (conversation == null) return null;

        if (editing) {
            return getString(R.string.editing);
        } else if (adapter != null && adapter.isSelected()) {
            return getString(R.string.selected_count, adapter.getSelectedCount() + "");
        } else if (conversation.getLast() != null && (conversation.isChat() || conversation.isGroupChannel()) && conversation.getState() != VKConversation.State.IN) {
            boolean kicked = conversation.getState() == VKConversation.State.KICKED;

            return getString(kicked ? R.string.kicked_out_text : R.string.leave_from_chat_text);
        } else
            switch (conversation.getType()) {
                case GROUP: {
                    return getString(R.string.group);
                }
                case USER: {
                    VKUser currentUser = CacheStorage.getUser(peerId);
                    if (currentUser == null)
                        loadUser(peerId);
                    return getUserSubtitle(currentUser);
                }
                case CHAT: {
                    if (conversation.isGroupChannel()) {
                        return getString(R.string.channel) + " • " + getString(R.string.members_count, membersCount);
                    } else {
                        return membersCount > 0 ? getString(R.string.members_count, membersCount) : "";
                    }
                }
            }


        return "Unknown";
    }

    private void loadUser(final int peerId) {
        ThreadExecutor.execute(new AsyncCallback(this) {
            VKUser user;

            @Override
            public void ready() throws Exception {
                user = VKApi.users().get().userId(peerId).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.insert(DatabaseHelper.USERS_TABLE, user);
                getSupportActionBar().setSubtitle(getSubtitle());
            }

            @Override
            public void error(Exception e) {
                Log.e("Error load user", Log.getStackTraceString(e));
            }
        });
    }

    private String getUserSubtitle(VKUser user) {
        if (user == null) return "";
        if (user.online) return getString(R.string.online);

        return getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w, Util.dateFormatter.format(user.last_seen * 1000));
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
                onBackPressed();
                break;
            case R.id.delete:
                showConfirmDeleteMessages(adapter.getSelectedMessages());
                break;
            case R.id.clear:
                showConfirmDeleteConversation();
                break;
            case R.id.notifications:
                toggleNotifications();
                invalidateOptionsMenu();
                break;
            case R.id.leave:
                toggleChatState();
                invalidateOptionsMenu();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleChatState() {
        if (conversation == null) return;
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean leave = conversation.getState() == VKConversation.State.IN;
                int chatId = conversation.getLast().getPeerId() - 2_000_000_000;
                setChatState(chatId, leave);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void setChatState(final int chatId, final boolean leave) {
        ThreadExecutor.execute(new AsyncCallback(this) {

            int response;

            @Override
            public void ready() throws Exception {
                response = leave ?
                        VKApi.messages()
                                .removeChatUser()
                                .chatId(chatId)
                                .userId(UserConfig.userId)
                                .execute(Integer.class).get(0) :

                        VKApi.messages()
                                .addChatUser()
                                .chatId(chatId)
                                .userId(UserConfig.userId)
                                .execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (response == 1) {
                    conversation.setState(leave ? VKConversation.State.LEFT : VKConversation.State.IN);
                    CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
                    invalidateOptionsMenu();
                    getSupportActionBar().setSubtitle(getSubtitle());
                }
            }

            @Override
            public void error(Exception e) {
                Log.e("Error toggle state", Log.getStackTraceString(e));
            }
        });
    }

    private void toggleNotifications() {
        if (conversation == null) return;
        if (conversation.isNotificationsDisabled()) {
            conversation.setNoSound(false);
            conversation.setDisabledForever(false);
            conversation.setDisabledUntil(0);
        } else {
            conversation.setNoSound(true);
            conversation.setDisabledUntil(-1);
            conversation.setDisabledForever(true);
        }

        setNotifications(!conversation.isNotificationsDisabled());
    }

    private void setNotifications(final boolean on) {
        ThreadExecutor.execute(new AsyncCallback(this) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.account()
                        .setSilenceMode()
                        .peerId(peerId)
                        .time(on ? 0 : -1)
                        .sound(on)
                        .execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                conversation.setDisabledForever(!on);
                conversation.setDisabledUntil(on ? 0 : -1);
                conversation.setNoSound(!on);

                CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
            }

            @Override
            public void error(Exception e) {

            }
        });
    }

    private void showConfirmDeleteMessages(final ArrayList<VKMessage> items) {
        final int[] mIds = new int[items.size()];

        boolean self = true;
        boolean can = true;

        for (int i = 0; i < items.size(); i++) {
            VKMessage message = items.get(i);
            mIds[i] = message.getId();

            if (message.getDate() * 1000L < System.currentTimeMillis() - AlarmManager.INTERVAL_DAY)
                can = false;

            if (message.getFromId() != UserConfig.userId)
                self = false;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.are_you_sure);

        View v = LayoutInflater.from(this).inflate(R.layout.activity_messages_dialog_checkbox, null, false);

        final MaterialCheckBox checkBox = v.findViewById(R.id.checkBox);
        checkBox.setText(R.string.for_everyone);
        checkBox.setEnabled(peerId != UserConfig.userId && can);
        checkBox.setChecked(true);

        if (self)
            adb.setView(v);

        final Boolean forAll = self ? checkBox.isEnabled() ? checkBox.isChecked() : null : null;

        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (editing) {
                    editing = false;
                    updateStyles();

                    editingPosition = -1;

                    adapter.clearSelected();
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
                }

                deleteMessages(items, forAll, mIds);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void deleteMessages(final ArrayList<VKMessage> messages, final Boolean forAll, final int... mIds) {
        ThreadExecutor.execute(new AsyncCallback(this) {

            @Override
            public void ready() throws Exception {
                VKApi.messages().delete().messageIds(mIds).every(forAll).execute();
            }

            @Override
            public void done() {
                adapter.getValues().removeAll(messages);
                adapter.clearSelected();
                adapter.notifyDataSetChanged();

                invalidateOptionsMenu();
                getSupportActionBar().setSubtitle(getSubtitle());

                for (VKMessage message : messages) {
                    CacheStorage.delete(DatabaseHelper.MESSAGES_TABLE, DatabaseHelper.MESSAGE_ID, message.getId());
                }
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void showAlert(final int position) {
        final VKMessage item = adapter.getItem(position);

        ArrayList<String> list = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.message_functions)));
        ArrayList<String> remove = new ArrayList<>();

        if (!conversation.isCanChangePin()) {
            remove.add(getString(R.string.pin_message));
        }

        if (conversation.getLast().getDate() * 1000L < System.currentTimeMillis() - AlarmManager.INTERVAL_DAY || !item.isOut()) {
            remove.add(getString(R.string.edit));
        }

        list.removeAll(remove);

        final String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++)
            items[i] = list.get(i);

        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        adb.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = items[i];

                if (title.equals(getString(R.string.delete))) {
                    showConfirmDeleteMessages(new ArrayList<>(Collections.singletonList(item)));
                } else if (title.equals(getString(R.string.pin_message))) {
                    showConfirmPinDialog(item);
                } else if (title.equals(getString(R.string.edit))) {
                    edited = item;

                    adapter.setSelected(position, true);
                    adapter.notifyItemChanged(position, -1);

                    editingPosition = position;

                    showEdit();
                }
            }
        });
        adb.show();
    }

    private void showEdit() {
        editing = true;

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        message.requestFocus();
        ViewUtil.showKeyboard(message);

        messageText = message.getText().toString();
        updateStyles();
    }

    private void updateStyles() {
        if (editing) {
            message.setText(edited.getText());
            setDoneStyle();
        } else {
            adapter.clearSelected();
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);

            if (messageText.trim().isEmpty()) {
                message.setText("");
                setMicStyle();
            } else {
                message.setText(messageText);
                setSendStyle();
            }

            messageText = null;
        }

        getSupportActionBar().setSubtitle(getSubtitle());
        message.setSelection(message.getText().length());
    }

    private void editMessage(final VKMessage edited) {
        edited.setText(message.getText().toString().trim());
        if (edited.getText().trim().isEmpty() && ArrayUtil.isEmpty(edited.getAttachments()) && ArrayUtil.isEmpty(edited.getFwdMessages())) {
            showConfirmDeleteMessages(new ArrayList<>(Collections.singletonList(edited)));
        } else
            ThreadExecutor.execute(new AsyncCallback(this) {
                int response;

                @Override
                public void ready() throws Exception {
                    response = VKApi.messages().edit()
                            .text(edited.getText())
                            .messageId(edited.getId())
                            .attachment(edited.getAttachments())
                            .keepForwardMessages(true)
                            .keepSnippets(true)
                            .peerId(peerId)
                            .execute(Integer.class)
                            .get(0);
                }

                @Override
                public void done() {
                    if (response != 1) return;

                    MessagesActivity.this.edited = null;
                    editing = false;
                    updateStyles();

                    VKMessage message = adapter.getItem(editingPosition);
                    message.setText(edited.getText());
                    message.setUpdateTime(System.currentTimeMillis());

                    adapter.notifyItemChanged(editingPosition, -1);
                    editingPosition = -1;
                }

                @Override
                public void error(Exception e) {
                    Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showConfirmPinDialog(final VKMessage message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                pinMessage(message);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void pinMessage(final VKMessage message) {
        ThreadExecutor.execute(new AsyncCallback(this) {

            @Override
            public void ready() throws Exception {
                VKApi.messages().pin().messageId(message.getId()).peerId(peerId).execute();
            }

            @Override
            public void done() {
                pinned = message;
                showPinned(pinned);

                conversation.setPinned(pinned);

                CacheStorage.update(DatabaseHelper.DIALOGS_TABLE, conversation, DatabaseHelper.PEER_ID, peerId);
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
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
            if (s.toString().trim().isEmpty())
                setMicStyle();
            else
                setSendStyle();
        } else {
            if (s.toString().trim().isEmpty()) {
                setTrashStyle();
                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showConfirmDeleteMessages(new ArrayList<>(Collections.singletonList(edited)));
                    }
                });
            } else {
                setDoneStyle();
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private void setSendStyle() {
        send.setImageDrawable(iconSend);
        send.setOnClickListener(sendClick);
    }

    private void setMicStyle() {
        send.setImageDrawable(iconMic);
        send.setOnClickListener(recordClick);
    }

    private void setDoneStyle() {
        send.setImageDrawable(iconDone);
        send.setOnClickListener(doneClick);
    }

    private void setTrashStyle() {
        send.setImageDrawable(iconTrash);
    }

    @Override
    public void onBackPressed() {
        if (editing) {
            editing = false;
            updateStyles();
        } else if (adapter != null && adapter.isSelected()) {
            adapter.clearSelected();
            invalidateOptionsMenu();
            getSupportActionBar().setSubtitle(getSubtitle());
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
        } else
            super.onBackPressed();
    }

    @Override
    public void onItemClick(View v, int position) {
        VKMessage item = adapter.getItem(position);

        if (item.getAction() != null) return;

        if (adapter.isSelected()) {
            adapter.toggleSelected(position);
            adapter.notifyItemChanged(position, -1);
            getSupportActionBar().setSubtitle(getSubtitle());
            invalidateOptionsMenu();
        } else
            showAlert(position);
    }

    @Override
    public void onItemLongClick(View v, int position) {
        VKMessage item = adapter.getItem(position);
        if (item.getAction() != null) return;

        if (adapter.isSelected()) {
            adapter.clearSelected();
            adapter.notifyItemRangeChanged(0, adapter.getItemCount(), -1);
        } else {
            adapter.setSelected(position, true);
            adapter.notifyItemChanged(position, -1);
        }

        getSupportActionBar().setSubtitle(getSubtitle());
        invalidateOptionsMenu();
    }

    private void showConfirmDeleteConversation() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.are_you_sure);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteConversation();
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.show();
    }

    private void deleteConversation() {
        if (!Util.hasConnection()) {
            return;
        }

        ThreadExecutor.execute(new AsyncCallback(this) {
            int response;

            @Override
            public void ready() throws Exception {
                response = VKApi.messages().deleteConversation().peerId(peerId).offset(0).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE, DatabaseHelper.PEER_ID, peerId);
                adapter.getValues().clear();
                adapter.notifyDataSetChanged();
                checkCount();
            }

            @Override
            public void error(Exception e) {
                Log.e("Error delete dialog", Log.getStackTraceString(e));
                Toast.makeText(MessagesActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem delete = menu.findItem(R.id.delete);
        MenuItem clear = menu.findItem(R.id.clear);
        MenuItem notifications = menu.findItem(R.id.notifications);
        MenuItem leave = menu.findItem(R.id.leave);

        if (conversation != null) {
            if (conversation.getLast() != null && (conversation.isChat() || conversation.isGroupChannel()) && conversation.getState() != VKConversation.State.KICKED) {
                leave.setVisible(true);
                String title = getString(conversation.getState() == VKConversation.State.IN ? R.string.leave_from : R.string.return_to);
                String s = getString(conversation.isGroupChannel() ? R.string.channel : R.string.chat).toLowerCase();

                leave.setTitle(String.format(title, s));
            } else {
                leave.setVisible(false);
            }
            if (conversation.isNotificationsDisabled()) {
                notifications.setTitle(R.string.enable_notifications);
                notifications.setIcon(R.drawable.ic_volume_full_black_24dp);
            } else {
                notifications.setTitle(R.string.disable_notifications);
                notifications.setIcon(R.drawable.ic_volume_off_black_24dp);
            }
        }

        delete.getIcon().setTint(ThemeManager.getMain());

        boolean selecting = adapter != null && adapter.isSelected();

        delete.setVisible(selecting);
        clear.setVisible(!selecting);
        notifications.setVisible(!selecting);

        return super.onPrepareOptionsMenu(menu);
    }
}