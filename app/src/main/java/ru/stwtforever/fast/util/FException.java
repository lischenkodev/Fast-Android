package ru.stwtforever.fast.util;

public class FException {
	
	public String exception;
	public long time;
	
	public FException() {}
	
	public FException(String exception, long time) {
		this.exception = exception;
		this.time = time;
	}
	
}
