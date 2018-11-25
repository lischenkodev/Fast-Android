package ru.stwtforever.fast.helper;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import ru.stwtforever.fast.R;
import ru.stwtforever.fast.common.AppGlobal;

public class DialogHelper {

    public static AlertDialog create(AlertDialog.Builder adb, String[] entries, DialogInterface.OnClickListener click) {
        if (click == null) return null;

        adb.setItems(entries, click);

        AlertDialog dialog = create(adb);
        dialog.show();

        return dialog;
    }

    public static AlertDialog create(AlertDialog.Builder adb, String[] entries, boolean[] values, DialogInterface.OnMultiChoiceClickListener click) {
        if (click == null) return null;

        adb.setMultiChoiceItems(entries, values, click);

        AlertDialog dialog = create(adb);
        dialog.show();

        return dialog;
    }

    public static AlertDialog create(AlertDialog.Builder adb) {
        return adb.create();
    }

    public static void show(AlertDialog.Builder adb) {
        create(adb).show();
    }


    public static AlertDialog showConfirmDialog(String confirm_text, DialogInterface.OnClickListener positive, DialogInterface.OnClickListener negative, boolean cancelable) {
        AlertDialog.Builder adb = new AlertDialog.Builder(AppGlobal.context);
        adb.setTitle(R.string.confirmation);
        adb.setMessage(confirm_text);
        adb.setPositiveButton(R.string.yes, positive);
        adb.setNegativeButton(R.string.no, negative);
        adb.setCancelable(cancelable);

        return create(adb);
    }
}
