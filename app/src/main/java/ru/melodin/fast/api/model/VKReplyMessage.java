package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.util.ArrayUtil;

public class VKReplyMessage extends VKModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int fromId;
    private int date;
    private int peerId;
    private int conversationMessageId;
    private ArrayList<VKModel> attachments;
    private String text;

    public VKReplyMessage(JSONObject o) {
        this.id = o.optInt("id", -1);
        this.fromId = o.optInt("from_id", -1);
        this.peerId = o.optInt("peer_id", -1);
        this.date = o.optInt("date", -1);
        this.conversationMessageId = o.optInt("conversation_message_id", -1);
        this.text = o.optString("text");

        JSONArray attachments = o.optJSONArray("attachments");
        if (!ArrayUtil.INSTANCE.isEmpty(attachments)) {
            this.attachments = VKAttachments.parse(attachments);
        }
    }

    public VKMessage asMessage() {
        VKMessage message = new VKMessage();

        message.setId(getId());
        message.setFromId(getFromId());
        message.setPeerId(getPeerId());
        message.setDate(getDate());
        message.setConversationMessageId(getConversationMessageId());
        message.setText(getText());
        message.setAttachments(getAttachments());

        return message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public int getConversationMessageId() {
        return conversationMessageId;
    }

    public void setConversationMessageId(int conversationMessageId) {
        this.conversationMessageId = conversationMessageId;
    }

    public ArrayList<VKModel> getAttachments() {
        return attachments;
    }

    public void setAttachments(ArrayList<VKModel> attachments) {
        this.attachments = attachments;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
