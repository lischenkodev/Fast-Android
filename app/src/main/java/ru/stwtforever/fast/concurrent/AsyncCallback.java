package ru.stwtforever.fast.concurrent;

import android.app.*;
import java.lang.ref.*;
import ru.stwtforever.fast.common.*;
import ru.stwtforever.fast.util.*;

public abstract class AsyncCallback implements Runnable {
    private WeakReference<Activity> ref;

    public AsyncCallback(Activity activity) {
        this.ref = new WeakReference<>(activity);
    }

    public abstract void ready() throws Exception;

    public abstract void done();

    public abstract void error(Exception e);

    @Override
    public void run() {
        try {
            ready();
        } catch (final Exception e) {
			AppGlobal.putException(e);
            e.printStackTrace();
			
            if (ref != null && ref.get() != null) {
                ref.get().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							error(e);
						}
					});
            }
            return;
        }

        if (ref != null && ref.get() != null) {
            ref.get().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						done();
					}
				});
        }
    }
}
