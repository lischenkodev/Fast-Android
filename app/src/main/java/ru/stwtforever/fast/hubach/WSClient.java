package ru.stwtforever.fast.hubach;

import android.app.*;
import java.net.*;
import java.util.*;

import ru.stwtforever.fast.*;

public class WSClient {//WebSocketClient {

	private MainActivity a;
	
	public WSClient(MainActivity a, URI url, Map<String, String> headers) {
		//super(url, headers);
		
		this.a = a;
	}
	/*
	@Override
	public void onOpen(ServerHandshake p1) {
		a.onOpen();
	}

	@Override
	public void onMessage(String message) {
		a.onMessage(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		a.onClose(code, reason);
	}

	@Override
	public void onError(Exception e) {
		a.onError(e);
	}
	
	*/
}
