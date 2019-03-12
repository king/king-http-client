package com.king.platform.net.http;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Headers {
	String get(CharSequence name);

	List<String> getAll(CharSequence name);

	List<Map.Entry<String, String>> entries();

	boolean contains(CharSequence name);

	int size();

	Set<String> names();

	Iterator<Map.Entry<String, String>> iterator();
}
