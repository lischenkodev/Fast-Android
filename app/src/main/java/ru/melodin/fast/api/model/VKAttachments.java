package ru.melodin.fast.api.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

import ru.melodin.fast.R;
import ru.melodin.fast.util.ArrayUtil;

public class VKAttachments extends VKModel implements Serializable {

    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_DOC = "doc";
    public static final String TYPE_WALL = "wall";
    public static final String TYPE_POSTED_PHOTO = "posted_photo";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_APP = "app";
    public static final String TYPE_POLL = "poll";
    public static final String TYPE_WIKI_PAGE = "page";
    public static final String TYPE_ALBUM = "album";
    public static final String TYPE_STICKER = "sticker";
    public static final String TYPE_GIFT = "gift";
    public static final String TYPE_AUDIO_MESSAGE = "audio_message";
    public static final String TYPE_GRAFFITI = "graffiti";

    public static ArrayList<VKModel> parse(JSONArray array) {
        ArrayList<VKModel> attachments = new ArrayList<>(array.length());

        for (int i = 0; i < array.length(); i++) {
            JSONObject attachment = array.optJSONObject(i);
            if (attachment.has("attachment")) {
                attachment = attachment.optJSONObject("attachment");
            }

            String type = attachment.optString("type");
            JSONObject object = attachment.optJSONObject(type);

            switch (type) {
                case TYPE_PHOTO:
                    attachments.add(new VKPhoto(object));
                    break;
                case TYPE_AUDIO:
                    attachments.add(new VKAudio(object));
                    break;
                case TYPE_VIDEO:
                    attachments.add(new VKVideo(object));
                    break;
                case TYPE_DOC:
                    attachments.add(new VKDoc(object));
                    break;
                case TYPE_STICKER:
                    attachments.add(new VKSticker(object));
                    break;
                case TYPE_LINK:
                    attachments.add(new VKLink(object));
                    break;
                case TYPE_GIFT:
                    attachments.add(new VKGift(object));
                    break;
                case TYPE_AUDIO_MESSAGE:
                    attachments.add(new VKVoice(object));
                    break;
                case TYPE_GRAFFITI:
                    attachments.add(new VKGraffiti(object));
                    break;
                case TYPE_WALL:
                    attachments.add(new VKWall(object));
                    break;
            }
        }

        return attachments;
    }

    public static String getAttachmentString(ArrayList<VKModel> attachments) {
        if (ArrayUtil.isEmpty(attachments)) return "";
        StringBuilder b = new StringBuilder();

        if (attachments.size() > 1) {
            return attachments.size() + " " + getString(R.string.attachments_lot).toLowerCase();
        }

        for (VKModel attach : attachments) {
            if (attach instanceof VKAudio) {
                b.append(getString(R.string.audio));
            } else if (attach instanceof VKPhoto) {
                b.append(getString(R.string.photo));
            } else if (attach instanceof VKSticker) {
                b.append(getString(R.string.sticker));
            } else if (attach instanceof VKDoc) {
                b.append(getString(R.string.doc));
            } else if (attach instanceof VKLink) {
                b.append(getString(R.string.link));
            } else if (attach instanceof VKVideo) {
                b.append(getString(R.string.video));
            } else if (attach instanceof VKVoice) {
                b.append(getString(R.string.voice_message));
            } else if (attach instanceof VKGraffiti) {
                b.append(getString(R.string.graffiti));
            } else if (attach instanceof VKGift) {
                b.append(getString(R.string.gift));
            } else if (attach instanceof VKWall) {
                b.append(getString(R.string.wall_post));
            } else {
                b.append(getString(R.string.attachment));
            }
        }

        return b.toString();
    }

    private static boolean isOneType(Class type, ArrayList<VKModel> attachments) {
        if (ArrayUtil.isEmpty(attachments) || type == null) return false;

        for (VKModel a : attachments) {
            if (!a.getClass().equals(type)) {
                return false;
            }
        }

        return true;
    }

    public static ArrayList<VKModel> parseFromLongPoll(JSONObject o) {
        ArrayList<VKModel> attachments = new ArrayList<>();

        for (int i = 0; i < o.length() / 2; i++) {
            String a_type = "attach" + i + "_type";
            String attach = "attach" + i;

            if (o.has(a_type)) {
                String type = o.optString(a_type);

                String attachment = o.optString(attach);
                String[] s = attachment.split("_");

                if (ArrayUtil.isEmpty(s) || s.length == 1) return null;

                int peerId = Integer.parseInt(s[0]);
                int attId = Integer.parseInt(s[1]);

                switch (type) {
                    case TYPE_PHOTO:
                        attachments.add(new VKPhoto(peerId, attId));
                        break;
                    case TYPE_AUDIO:
                        attachments.add(new VKAudio(peerId, attId));
                        break;
                    case TYPE_VIDEO:
                        attachments.add(new VKVideo(peerId, attId));
                        break;
                    case TYPE_DOC:
                        attachments.add(new VKDoc(peerId, attId));
                        break;
                    case TYPE_STICKER:
                        attachments.add(new VKSticker(peerId, attId));
                        break;
                    case TYPE_LINK:
                        attachments.add(new VKLink());
                        break;
                    case TYPE_GIFT:
                        attachments.add(new VKGift(peerId, attId));
                        break;
                }
            }
        }

        return attachments;
    }
}
