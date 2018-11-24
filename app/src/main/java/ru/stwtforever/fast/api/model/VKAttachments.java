package ru.stwtforever.fast.api.model;

import ru.stwtforever.fast.*;
import ru.stwtforever.fast.util.*;
import java.io.*;
import java.util.*;
import org.json.*;

public class VKAttachments extends VKModel implements Serializable {

    public static final String TYPE_PHOTO = "photo";

    /** Attachment is a video. */
    public static final String TYPE_VIDEO = "video";

    /** Attachment is an audio. */
    public static final String TYPE_AUDIO = "audio";

    /** Attachment is a document. */
    public static final String TYPE_DOC = "doc";

    /** Attachment is a wall post. */
    public static final String TYPE_POST = "wall";

    /** Attachment is a posted photo. */
    public static final String TYPE_POSTED_PHOTO = "posted_photo";

    /** Attachment is a link */
    public static final String TYPE_LINK = "link";

    /** Attachment is a note. */
    public static final String TYPE_NOTE = "note";

    /** Attachment is an application content. */
    public static final String TYPE_APP = "app";

    /** Attachment is a poll. */
    public static final String TYPE_POLL = "poll";

    /** Attachment is a WikiPage. */
    public static final String TYPE_WIKI_PAGE = "page";

    /** Attachment is a PhotoAlbum. */
    public static final String TYPE_ALBUM = "album";

    /** Attachment is a Sticker. */
    public static final String TYPE_STICKER = "sticker";

    /** Attachment is a Gift. */
    public static final String TYPE_GIFT = "gift";

    public static ArrayList<VKModel> parse(JSONArray array) {
        ArrayList<VKModel> attachments = new ArrayList<>(array.length());

        for (int i = 0; i < array.length(); i++) {
            JSONObject attach = array.optJSONObject(i);
            if (attach.has("attachment")) {
                attach = attach.optJSONObject("attachment");
            }

            String type = attach.optString("type");
            JSONObject object = attach.optJSONObject(type);

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
            }
        }

        return attachments;
    }

	public static String getAttachmentString(ArrayList<VKModel> attachments) {
		if (ArrayUtil.isEmpty(attachments)) return "";
		StringBuilder b = new StringBuilder();

		if (attachments.size() > 1) {
			if (isOneType(attachments.get(0).getClass(), attachments)) {
				return String.valueOf(attachments.size()) + " " + getAttachmentString(attachments).toLowerCase();
			} else {
				return String.valueOf(attachments.size()) + getString(R.string.attachments_lot).toLowerCase();
			}
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
            } else {
				b.append(getString(R.string.attachment));
			}
		}

		return b.toString();
	}

	public static boolean isOneType(Class type, ArrayList<VKModel> attachments) {
		if (ArrayUtil.isEmpty(attachments) || type == null) return false;

		for (VKModel a : attachments) {
			if (a.getClass() != type) {
				return false;
			}
		}

		return true;
	}

	public static ArrayList<VKModel> parseFromLongPoll(JSONObject o) {
		ArrayList<VKModel> attachments = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			String a_type = "attach" + i + "_type";
			String attach = "attach" + i;

			if (o.has(a_type)) {
				String type = o.optString(a_type);

				String attachment = o.optString(attach);
				String[] s = attachment.split("_");

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

		return new ArrayList<VKModel>();
	}
}
