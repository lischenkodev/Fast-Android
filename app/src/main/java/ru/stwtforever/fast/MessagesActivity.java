package ru.stwtforever.fast;

import android.content.DialogInterface;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ru.stwtforever.fast.adapter.MessageAdapter;
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
import ru.stwtforever.fast.db.CacheStorage;
import ru.stwtforever.fast.db.DBHelper;
import ru.stwtforever.fast.db.MemoryCache;
import ru.stwtforever.fast.fragment.FragmentSettings;
import ru.stwtforever.fast.helper.DialogHelper;
import ru.stwtforever.fast.helper.FontHelper;
import ru.stwtforever.fast.util.ArrayUtil;
import ru.stwtforever.fast.util.Utils;
import ru.stwtforever.fast.util.ViewUtils;

public class MessagesActivity extends AppCompatActivity implements TextWatcher {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ImageButton send, smiles, unpin;
    private EditText message;
    private ProgressBar progress;
    private LinearLayout chat_panel;

    private LinearLayoutManager layoutManager;
    private MessageAdapter adapter;

    private boolean loading, busy, canWrite, editing, typing;

    private int reason = -1, peerId, membersCount;

    private String text, reasonText, title, subtitle, photo;

    private TextView noMessages;

    private static final int MESSAGES_COUNT = 60;

    private Timer timer;

    private VKMessage pinned, last;
    private VKConversation conversation;

    private VKUser currentUser;

    private LinearLayout pnd;
    private TextView p_name, p_date, p_text;
    private View line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        getIntentData();
        showPinned(pinned);
        initViews();
        checkCanWrite();

        if (ThemeManager.isDark())
            if (chat_panel.getBackground() != null) {
                chat_panel.getBackground().setColorFilter(ThemeManager.getBackground(), PorterDuff.Mode.MULTIPLY);
            }

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(title);

        recyclerView.setLayoutManager(layoutManager);

        message.addTextChangedListener(this);

        subtitle = getSubtitleStatus();

        getCachedMessages();

        if (Utils.hasConnection()) {
            getMessages(0);
        }

        smiles.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View p1) {
                String template = Utils.getPrefs().getString(FragmentSettings.KEY_MESSAGE_TEMPLATE, FragmentSettings.DEFAULT_TEMPLATE_VALUE);
                if (message.getText().length() == 0) {
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
        gd.setCornerRadius(100);

        send.setBackground(gd);
        send.setImageResource(R.drawable.md_mic);

        ViewUtils.applyToolbarStyles(toolbar);
        noMessages.setVisibility(View.GONE);
    }

    public void onItemClick(View v, int i, VKMessage item) {
        if (item.out) {
            showOutDialog(i);
        } else {
            showInDialog(i);
        }
    }

    private void checkCanWrite() {
        send.setEnabled(true);
        smiles.setEnabled(true);
        message.setEnabled(true);
        message.setHint(R.string.tap_to_type);
        message.setText("");

        if (reason <= 0) return;
        if (!canWrite) {
            send.setEnabled(false);
            smiles.setEnabled(false);
            message.setEnabled(false);
            message.setHint(VKUtils.getErrorReason(reason));
            message.setText("");
        }
    }

    private void showPinned(final VKMessage pinned) {
        pnd = findViewById(R.id.pinned_msg_container);
        p_name = pnd.findViewById(R.id.name);
        p_date = pnd.findViewById(R.id.date);
        p_text = pnd.findViewById(R.id.message);
        unpin = pnd.findViewById(R.id.unpin);
        line = findViewById(R.id.line);

        p_name.setTypeface(FontHelper.getFont(FontHelper.PS_BOLD));

        if (pinned == null) {
            line.setVisibility(View.GONE);
            pnd.setVisibility(View.GONE);
            checkPinnedExists();
            return;
        }

        checkPinnedExists();

        pnd.setVisibility(View.VISIBLE);
        line.setVisibility(View.VISIBLE);

        pnd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                showAlertPinned(pinned);
            }

        });

        VKUser user = CacheStorage.getUser(pinned.fromId);
        if (user == null) user = VKUser.EMPTY;

        p_name.setText(user.toString());
        p_date.setText(Utils.dateFormatter.format(pinned.date * 1000));

        p_text.setText(pinned.text);

        unpin.setVisibility(conversation.can_change_pin ? View.VISIBLE : View.GONE);
        unpin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showConfirmUnpinDialog();
            }
        });

        if ((pinned.attachments != null
                || !ArrayUtil.isEmpty(pinned.fwd_messages))
                && TextUtils.isEmpty(pinned.text)) {

            String body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages);

            String r = "<b>" + body + "</b>";
            SpannableString span = new SpannableString(Html.fromHtml(r));
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, body.length(), 0);

            p_text.append(span);
        }
    }

    private void showAlertPinned(VKMessage pinned) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.pinned_message);

        View v = LayoutInflater.from(this).inflate(R.layout.alert_pinned, null, false);

        LinearLayout pnd = v.findViewById(R.id.pinned_msg_container);
        TextView name = pnd.findViewById(R.id.name);
        TextView date = pnd.findViewById(R.id.date);
        TextView text = pnd.findViewById(R.id.message);

        VKUser user = CacheStorage.getUser(pinned.fromId);
        if (user == null) user = VKUser.EMPTY;

        name.setTypeface(FontHelper.getFont(FontHelper.PS_BOLD));

        name.setText(user.toString());
        date.setText(Utils.dateFormatter.format(pinned.date * 1000));

        text.setText(pinned.text);

        if ((pinned.attachments != null
                || !ArrayUtil.isEmpty(pinned.fwd_messages))
                && TextUtils.isEmpty(pinned.text)) {

            String body = VKUtils.getAttachmentBody(pinned.attachments, pinned.fwd_messages);

            String r = "<b>" + body + "</b>";
            SpannableString span = new SpannableString(Html.fromHtml(r));
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, body.length(), 0);

            text.append(span);
        }

        adb.setView(v);
        adb.setPositiveButton(android.R.string.ok, null);

        DialogHelper.create(adb).show();
    }

    private void pinMessage(final int position) {
        if (pinned != null && pinned.id == adapter.getItem(position).id) return;
        Toast.makeText(this, adapter.getItem(position).id + "", Toast.LENGTH_LONG).show();
        ThreadExecutor.execute(new AsyncCallback(this) {

            VKMessage msg;

            @Override
            public void ready() throws Exception {
                msg = adapter.getItem(position);
                VKApi.messages().pin().peerId(peerId).messageId(msg.id).execute(null);
            }

            @Override
            public void done() {
                conversation.pinned = msg;
                pinned = msg;
                showPinned(msg);
                VKMessage m = new VKMessage();
                m.fromId = UserConfig.userId;
                m.actionType = VKMessage.ACTION_CHAT_PIN_MESSAGE;

                adapter.getValues().add(m);
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, getString(R.string.error) + "\nmId = " + adapter.getItem(position).id + "\npeerId = " + peerId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unpinMessage() {
        ThreadExecutor.execute(new AsyncCallback(this) {

            @Override
            public void ready() throws Exception {
                VKApi.messages().unpin().peerId(peerId).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                conversation.pinned = null;
                pinned = null;
                showPinned(null);

                VKMessage m = new VKMessage();
                m.fromId = UserConfig.userId;
                m.actionType = VKMessage.ACTION_CHAT_UNPIN_MESSAGE;

                adapter.getValues().add(m);
                adapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }

        });
    }

    private void sendMessage() {
        if (busy || TextUtils.isEmpty(text)) return;
        busy = true;

        final VKMessage msg = new VKMessage();
        msg.text = text;
        msg.fromId = UserConfig.userId;
        msg.peerId = peerId;
        msg.id = adapter.getItem(adapter.getMessagesCount() - 1).id + 1;
        msg.date = Calendar.getInstance().getTimeInMillis();
        msg.out = true;
        msg.status = VKMessage.STATUS_SENDING;
        msg.isAdded = true;
        msg.randomId = new Random().nextLong();

        adapter.getValues().add(msg);
        adapter.notifyItemInserted(adapter.getItemCount() - 1);
        recyclerView.smoothScrollToPosition(adapter.getValues().size());

        ThreadExecutor.execute(new AsyncCallback(this) {

            @Override
            public void ready() throws Exception {
                msg.id = VKApi.messages().send().peerId(peerId).randomId(msg.randomId).text(text.trim()).execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                if (typing) {
                    typing = false;
                    if (timer != null)
                        timer.cancel();
                }

                checkMessagesCount();
                busy = false;
                msg.status = VKMessage.STATUS_SENT;
                adapter.getValues().remove(msg);
                adapter.add(msg, true);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void error(Exception e) {
                e.printStackTrace();
                busy = false;
                msg.status = VKMessage.STATUS_ERROR;
                adapter.notifyDataSetChanged();
            }
        });

    }

    public void edit(final int position) {
        final VKMessage m = adapter.getValues().get(position);
        final String oldText = m.text;

        adapter.showHover(position, true);

        applyStyles(true);
        editing = true;

        message.setText(oldText);
        message.setSelection(oldText.length());

        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View p1) {
                String newText = message.getText().toString();

                if ((!TextUtils.isEmpty(newText) && ArrayUtil.isEmpty(m.attachments) && ArrayUtil.isEmpty(m.fwd_messages)) || (!ArrayUtil.isEmpty(m.attachments) || !ArrayUtil.isEmpty(m.fwd_messages))) {
                    editMessage(position, m.id, oldText, newText);
                } else {
                    if (ArrayUtil.isEmpty(m.attachments) && ArrayUtil.isEmpty(m.fwd_messages))
                        showDeleteOutDialog(position);
                }
            }

        });
    }


    @Override
    public void onBackPressed() {
        if (editing) {
            editing = false;
            applyStyles(editing);
            message.setText("");
            checkHovered();
        } else
            super.onBackPressed();
    }

    private void checkHovered() {
        int position = -1;

        for (int i = 0; i < adapter.getValues().size(); i++) {
            VKMessage m = adapter.getValues().get(i);
            if (m.isSelected()) position = i;
        }

        if (position == -1) return;

        adapter.getValues().get(position).setSelected(false);
        adapter.notifyItemChanged(position);
    }

    private void deleteMessage(final int pos, final Boolean every, final Boolean spam) {
        if (busy) return;
        busy = true;
        ThreadExecutor.execute(new AsyncCallback(this) {

            @Override
            public void ready() throws Exception {
                int id = adapter.getValues().get(pos).id;
                VKApi.messages()
                        .delete()
                        .messageIds(id)
                        .every(every)
                        .spam(spam)
                        .execute(Integer.class);
            }

            @Override
            public void done() {
                busy = false;
                if (adapter.isHover(pos)) {
                    adapter.showHover(pos, false);
                }

                adapter.getValues().remove(pos);
                adapter.notifyDataSetChanged();
                checkMessagesCount();
            }

            @Override
            public void error(Exception e) {
                busy = false;
                Toast.makeText(MessagesActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editMessage(final int pos, final int messageId, final String oldText, final String newText) {
        if (busy) return;
        busy = true;

        adapter.getValues().get(pos).status = VKMessage.STATUS_SENDING;
        adapter.notifyItemChanged(pos);
        ThreadExecutor.execute(new AsyncCallback(this) {

            int res;

            @Override
            public void ready() throws Exception {
                res = VKApi.messages()
                        .edit()
                        .peerId(peerId)
                        .text(newText)
                        .messageId(messageId)
                        .keepForwardMessages(true)
                        .attachment(adapter.getItem(pos).attachments)
                        .keepSnippets(true)
                        .dontParseLinks(false)
                        .execute(Integer.class).get(0);
            }

            @Override
            public void done() {
                busy = false;
                adapter.getValues().get(pos).text = newText;
                adapter.getValues().get(pos).status = VKMessage.STATUS_SENT;
                adapter.getValues().get(pos).setSelected(false);
                adapter.notifyItemChanged(pos);

                editing = false;

                applyStyles(false);
                message.setText("");
            }

            @Override
            public void error(Exception e) {
                busy = false;
                adapter.getValues().get(pos).status = VKMessage.STATUS_ERROR;
                adapter.notifyItemChanged(pos);
                Toast.makeText(MessagesActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void applyStyles(final boolean isEdit) {
        if (isEdit) {
            send.setImageResource(R.drawable.md_done);
        } else {
            String s = message.getText().toString();

            applyBtnStyle(!(s != null && !s.trim().isEmpty()));
        }
    }

    private View.OnClickListener send_listener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String s = message.getText().toString();
            if (s != null && !s.trim().isEmpty()) {
                text = s;

                sendMessage();
                message.setText(null);
            }
        }

    };

    private View.OnClickListener record_listener = new View.OnClickListener() {

        @Override
        public void onClick(View p1) {

        }
    };

    @Override
    public void onTextChanged(CharSequence cs, int p2, int p3, int p4) {
        if (!typing) {
            setTyping();
        }
        if (!editing) {
            applyBtnStyle(cs.toString().trim().isEmpty());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence cs, int p2, int p3, int p4) {
    }

    @Override
    public void afterTextChanged(Editable p1) {
    }

    private void setTyping() {
        if (Utils.getPrefs().getBoolean(FragmentSettings.KEY_HIDE_TYPING, false)) return;
        typing = true;
        new LowThread(new Runnable() {

            @Override
            public void run() {
                try {
                    VKApi.messages().setActivity().peerId(peerId).execute(Integer.class);
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
                } catch (Exception e) {
                }
            }

        }).start();
    }

    private void applyBtnStyle(boolean isTextEmpty) {
        if (isTextEmpty) {
            send.setImageResource(R.drawable.md_mic);
            if (!editing)
                send.setOnClickListener(record_listener);
        } else {
            send.setImageResource(R.drawable.md_send);
            if (!editing)
                send.setOnClickListener(send_listener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adapter != null) {
            adapter.destroy();
        }
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
                finish();
                break;
            case R.id.menuUpdate:
                if (!loading && !editing) {
                    adapter.getValues().clear();
                    getMessages(0);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        chat_panel = findViewById(R.id.chat_panel);
        smiles = findViewById(R.id.smiles);
        toolbar = findViewById(R.id.tb);
        recyclerView = findViewById(R.id.list);
        send = findViewById(R.id.send);
        message = findViewById(R.id.message);
        progress = findViewById(R.id.progress);
        noMessages = findViewById(R.id.text_no_messages);
    }

    public RecyclerView getRecycler() {
        return recyclerView;
    }

    private void getIntentData() {
        Intent intent = getIntent();
        this.conversation = (VKConversation) intent.getSerializableExtra("conversation");
        this.title = intent.getStringExtra("title");
        this.photo = intent.getStringExtra("photo");
        this.peerId = intent.getIntExtra("peer_id", -1);
        this.reason = intent.getIntExtra("reason", -1);
        this.canWrite = intent.getBooleanExtra("can_write", false);
        this.reasonText = canWrite ? "" : VKUtils.getErrorReason(reason);

        if (conversation != null) {
            this.last = conversation.last;
            this.membersCount = conversation.membersCount;
            this.pinned = conversation.pinned;
        }

        checkPinnedExists();
    }

    private void checkPinnedExists() {
        Space space = findViewById(R.id.space);
        space.setVisibility(pinned == null ? View.GONE : View.VISIBLE);
    }

    private void createAdapter(ArrayList<VKMessage> messages) {
        if (adapter != null) {
            adapter.changeItems(messages);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new MessageAdapter(this, messages, peerId);
            recyclerView.setAdapter(adapter);
            recyclerView.smoothScrollToPosition(adapter.getMessagesCount());
        }
    }

    private void insertMessages(ArrayList<VKMessage> messages) {
        if (adapter != null) {
            adapter.insert(messages);
            adapter.notifyItemRangeChanged(0, messages.size());
        }
    }

    private void getCachedMessages() {
        ArrayList<VKMessage> messages = CacheStorage.getMessages(peerId);
        if (!ArrayUtil.isEmpty(messages)) {
            createAdapter(messages);
            return;
        }
    }

    private String getSubtitleStatus() {
        int type = 2; //user
        if (peerId > 2_000_000_000) type = 0; //chat
        if (peerId < -1) type = 1; // group

        if (conversation != null) {
            if (conversation.isChannel()) {
                type = 3; //channel
            }
        }

        switch (type) {
            case 0:
                if (membersCount > 0)
                    return getString(R.string.members_count, membersCount);
                else
                    return "";
            case 1:
                return getString(R.string.group);
            case 2:
                currentUser = CacheStorage.getUser(peerId);
                return getUserSubtitle(currentUser);
            case 3:
                return getString(R.string.channel) + " • " + getString(R.string.members_count, membersCount);
            default:
                return "";
        }
    }

    private String getUserSubtitle(VKUser user) {
        if (user != null) {
            if (user.online) {
                return getString(R.string.online);
            } else {
                return String.format(getString(user.sex == VKUser.Sex.MALE ? R.string.last_seen_m : R.string.last_seen_w), Utils.dateFormatter.format(user.last_seen * 1000));
            }
        }

        return "";
    }

    public void checkMessagesCount() {
        if (adapter == null) return;

        int count = adapter.getValues().size();

        if (count > 0) {
            noMessages.setVisibility(View.GONE);
        } else {
            noMessages.setVisibility(View.VISIBLE);
        }
    }

    private void getUserIds(HashSet<Integer> ids, ArrayList<VKMessage> messages) {
        for (VKMessage m : messages) {
            if (!VKGroup.isGroupId(m.fromId)) {
                ids.add(m.fromId);
            }
            if (m.peerId < 2_000_000_000) {
                ids.add(m.peerId);
            }
        }
        for (VKMessage msg : messages) {
            if (!ArrayUtil.isEmpty(msg.fwd_messages)) {
                getUserIds(ids, msg.fwd_messages);
            }
        }
    }

    private void getUsers(final ArrayList<VKMessage> messages) {
        final HashSet<Integer> ids = new HashSet<>();
        getUserIds(ids, messages);

        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = VKApi.users().get().userIds(ids).fields("photo_100,photo_50,photo_200,online,last_seen").execute(VKUser.class);
            }

            @Override
            public void done() {
                if (ArrayUtil.isEmpty(users)) {
                    return;
                }

                CacheStorage.insert(DBHelper.USERS_TABLE, users);
                MemoryCache.update(users);
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getMessages(final int offset) {
        loading = true;
        getSupportActionBar().setSubtitle(getString(R.string.loading));
        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKMessage> messages;
            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                messages = VKApi.messages().getHistory().peerId(peerId).extended(true).offset(offset).count(MESSAGES_COUNT).execute(VKMessage.class);
            }

            @Override
            public void done() {
                Collections.reverse(messages);
                if (offset == 0) {
                    CacheStorage.deleteMessages(peerId);
                    CacheStorage.insert(DBHelper.MESSAGES_TABLE, messages);
                    createAdapter(messages);
                    recyclerView.smoothScrollToPosition(adapter.getMessagesCount());
                } else {
                    insertMessages(messages);
                }

                checkMessagesCount();

                loading = messages.isEmpty();
                if (!messages.isEmpty()) {
                    getUsers(messages);
                }

                getSupportActionBar().setSubtitle(getSubtitleStatus());
            }

            @Override
            public void error(Exception e) {
                getSupportActionBar().setSubtitle(getSubtitleStatus());
                e.printStackTrace();
            }

        });

        if (offset != 0) {
            return;
        }

        if (peerId > 2_000_000_000 || VKGroup.isGroupId(peerId)) {
            return;
        }

        ThreadExecutor.execute(new AsyncCallback(this) {

            ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = new ArrayList<>();

                VKUser user = VKApi.users().get().userId(peerId).fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class).get(0);
                users.add(user);
            }

            @Override
            public void done() {
                CacheStorage.insert(DBHelper.USERS_TABLE, users);
                getSupportActionBar().setSubtitle(getSubtitleStatus());
            }

            @Override
            public void error(Exception e) {
                Toast.makeText(MessagesActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
            }

        });
    }

    /*
     *
     *      DIALOGS
     *
     */

    private void showConfirmPinDialog(final int position) {
        if (conversation != null && !conversation.can_change_pin) return;

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.confirm_pin);
        adb.setNegativeButton(R.string.no, null);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int i) {
                pinMessage(position);
            }
        });

        DialogHelper.create(adb).show();
    }

    private void showConfirmUnpinDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.confirm_unpin);
        adb.setNegativeButton(R.string.no, null);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                unpinMessage();
            }
        });

        DialogHelper.create(adb).show();
    }

    private void showInDialog(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        String[] items = new String[]{getString(R.string.pin_message), getString(R.string.delete)};

        final VKMessage msg = adapter.getValues().get(position);
        final boolean isError = msg.status == VKMessage.STATUS_ERROR;

        if (isError) {
            items = new String[]{getString(R.string.retry), getString(R.string.delete)};
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int i) {
                switch (i) {
                    case 0:
                        showConfirmPinDialog(position);
                        break;
                    case 1:
                        showDeleteInDialog(position);
                        break;
                }
            }
        };

        DialogHelper.create(adb, items, listener);
    }

    private void showOutDialog(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        String[] items = new String[]{getString(R.string.pin_message), getString(R.string.edit), getString(R.string.delete)};

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int i) {
                switch (i) {
                    case 0:
                        showConfirmPinDialog(position);
                        break;
                    case 1:
                        edit(position);
                        break;
                    case 2:
                        showDeleteOutDialog(position);
                        break;
                }
            }
        };

        if (!editing)
            DialogHelper.create(adb, items, listener);
    }

    public void showDeleteInDialog(final int position) {

        VKMessage m = adapter.getItem(position);
        if (m.status == VKMessage.STATUS_ERROR) {
            adapter.getValues().remove(position);
            adapter.notifyDataSetChanged();
            return;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.delete) + "?");

        String[] items = new String[]{getString(R.string.spam)};

        final boolean[] values = new boolean[]{false};
        final boolean[] entries = new boolean[1];

        DialogInterface.OnMultiChoiceClickListener click = new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface d, int i, boolean value) {
                switch (i) {
                    case 0:
                        entries[0] = value;
                        break;
                }
            }
        };

        adb.setNegativeButton(R.string.no, null);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                deleteMessage(position, null, entries[0]);
            }
        });

        DialogHelper.create(adb, items, values, click);
    }

    public void showDeleteOutDialog(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.delete) + "?");

        String[] every = new String[]{getString(R.string.delete_for_everyone)};

        final boolean[] values = new boolean[]{true};
        final boolean[] entries = new boolean[]{true};

        DialogInterface.OnMultiChoiceClickListener click = new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface d, int i, boolean value) {
                switch (i) {
                    case 0:
                        entries[0] = value;
                        break;
                }
            }
        };

        adb.setNegativeButton(R.string.no, null);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface p1, int p2) {
                deleteMessage(position, entries[0], null);
            }
        });

        DialogHelper.create(adb, every, values, click);
    }

}
