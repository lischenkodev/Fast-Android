package ru.stwtforever.fast.helper;
import android.app.*;
import android.content.pm.*;
import android.support.transition.*;
import android.os.*;

public class PermissionHelper {
	
	private static Activity context;
	
	public static PermissionHelper init(Activity a) {
		PermissionHelper h = new PermissionHelper();
		h.context = a;
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			h.context = null;
		}
		return h;
	}
	
	private static boolean isL() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
	}
	
	public static boolean isPermissionDenied(String permission) {
		if (context == null || permission == null) return false;
		if (isL()) return true;
		return context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED;
	}
	
	public static boolean[] isPermissionsDenied(String[] permissions) {
		if (context == null || permissions == null || isL()) return null;
		
		boolean[] b = new boolean[permissions.length];
		
		for (int i = 0; i < permissions.length; i++) {
			b[i] = isPermissionDenied(permissions[i]);
		}
		
		return b;
	}
	
	public static boolean isGrantedPermission(Integer grant_res) {
		if (grant_res == null) return false;
		if (isL()) return true;
		return grant_res == PackageManager.PERMISSION_GRANTED;
	}
	
	public static boolean[] isGrantedPermissions(Integer[] grant_res) {
		if (grant_res == null) return null;
		boolean[] b = new boolean[grant_res.length];
		
		for (int i = 0; i < grant_res.length; i++) {
			if (isL())
				b[i] = true;
			else
				b[i] = isGrantedPermission(grant_res[i]);
		}
		
		return b;
	}
	
	public static boolean isGrantedPermission(String permission) {
		if (isL()) return true;
		if (context == null || permission == null) return false;
		return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}
	
	public static boolean[] isGrantedPermissions(String[] permissions) {
		if (context == null || permissions == null) return null;
		
		boolean[] granted = new boolean[permissions.length];
		
		for (int i = 0; i < permissions.length; i++) {
			if (isL()) granted[i] = true;
			else
				granted[i] = (isGrantedPermission(permissions[i]));
		}
		return granted;
	}
	
	public static void requestPermission(String permission, int request_code) {
		if (context == null || permission == null || isL()) return;
		context.requestPermissions(new String[]{permission}, request_code);
	}
	
	public static void requestPermissions(String[] permissions, int request_code) {
		if (context == null || permissions == null || isL()) return;
		context.requestPermissions(permissions, request_code);
	}
}
