package ru.stwtforever.fast.api.model;
import ru.stwtforever.fast.common.*;
import java.io.*;
import org.json.*;

public abstract class VKModel implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Object tag;
	protected boolean selected;
	
    protected VKModel() {
    }
	
    protected VKModel(JSONObject source) {
    }
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return selected;
	}

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }
	
	protected static String getString(int res) {
		return AppGlobal.context.getString(res);
	}
}

