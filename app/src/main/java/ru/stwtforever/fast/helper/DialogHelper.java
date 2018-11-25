package ru.stwtforever.fast.helper;
import androidx.appcompat.app.*;
import android.content.*;

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
	
}
