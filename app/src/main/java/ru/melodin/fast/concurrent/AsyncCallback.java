package ru.melodin.fast.concurrent;

import android.app.Activity;

import java.lang.ref.WeakReference;

import ru.melodin.fast.api.VKApi;

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
            e.printStackTrace();

            if (ref != null && ref.get() != null) {
                ref.get().runOnUiThread(() -> {
                    error(e);
                    VKApi.checkError(ref.get(), e);
                });
            }
            return;
        }

        if (ref != null && ref.get() != null) {
            ref.get().runOnUiThread(this::done);
        }
    }
}
