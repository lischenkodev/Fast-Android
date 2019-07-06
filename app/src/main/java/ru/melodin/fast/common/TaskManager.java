package ru.melodin.fast.common;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.method.MessageMethodSetter;
import ru.melodin.fast.api.method.MethodSetter;
import ru.melodin.fast.concurrent.LowThread;

public class TaskManager {

    private static TaskManager instance;

    private static ArrayList<Task> tasks = new ArrayList<>();

    @Contract(pure = true)
    public static TaskManager getInstance() {
        return instance;
    }

    static void init() {
        if (instance == null) instance = new TaskManager();
        //EventBus.getDefault().register(instance);
    }

    private static void addProcedure(@NonNull MethodSetter setter, @Nullable Class cls, CompleteListener listener) {
        Task task = new Task(setter, listener);

        if (tasks.indexOf(task) == -1) {
            tasks.add(task);
        }

        new LowThread(() -> setter.execute(cls, new VKApi.OnResponseListener<Object>() {
            @Override
            public void onSuccess(ArrayList<Object> models) {
                if (tasks.indexOf(task) != -1) {
                    tasks.remove(task);
                }

                if (listener != null)
                    listener.onComplete(models);
            }

            @Override
            public void onError(Exception e) {
                if (listener != null)
                    listener.onError(e);
            }
        })).start();
    }

    public static void sendMessage(@NonNull MethodSetter setter, @Nullable CompleteListener listener) {
        addProcedure(setter, Integer.class, listener);
    }

    public static void resendMessage(long randomId) {
        for (Task task : tasks) {
            MessageMethodSetter setter = (MessageMethodSetter) task.getSetter();
            if (setter.getParams().contains(String.valueOf(randomId))) {
                sendMessage(setter, task.getListener());
                break;
            }
        }
    }

    public interface CompleteListener {
        void onComplete(ArrayList<Object> models);

        void onError(Exception e);
    }

    private static class Task {
        private MethodSetter setter;
        private CompleteListener listener;

        Task(MethodSetter setter, CompleteListener listener) {
            this.setter = setter;
            this.listener = listener;
        }

        MethodSetter getSetter() {
            return setter;
        }

        public void setSetter(MethodSetter setter) {
            this.setter = setter;
        }

        CompleteListener getListener() {
            return listener;
        }

        public void setListener(CompleteListener listener) {
            this.listener = listener;
        }
    }
}
