// Copyright (C) king.com Ltd 2015
// https://github.com/king/king-http-client
// Author: Magnus Gustafsson
// License: Apache 2.0, https://raw.github.com/king/king-http-client/LICENSE-APACHE

package com.king.platform.net.http.netty.util;


public class StringUtil {

	public static String substringBefore(String value, char delim) {
		if (value == null) {
			return null;
		}

		int pos = value.indexOf(delim);
		if (pos >= 0) {
			return value.substring(0, pos);
		}
		return value;
	}

	public static String substringAfter(String value, char delim, boolean stripQuoteMarks) {
		if (value == null) {
			return null;
		}

		int startPos = value.indexOf(delim) + 1;
		int endPos = value.length();

		if (startPos == 0) {
			return null;
		}

		if (startPos == endPos) {
			return "";
		}

		if (stripQuoteMarks && isQuoteMark(value.charAt(startPos))) {
			startPos++;
			endPos--;
		}

		return value.substring(startPos, endPos);

	}

	private static boolean isQuoteMark(char c) {
		return c == '\"' || c == '\'';
	}


	public static String substringKeyValue(String keyToFind, String src, char delimiter, boolean stripQuoteMarks) {
		if (keyToFind == null) {
			return null;
		}

		if (src ==  null) {
			return null;
		}

		int srcLength = src.length();

		int startPosOfKey = src.indexOf(keyToFind);
		if (startPosOfKey == -1) {
			return null;
		}

		int endPosOfKey = startPosOfKey + keyToFind.length();

		while(endPosOfKey != srcLength && src.charAt(endPosOfKey) == ' ') {  //trim whitespace between key and delimiter
			endPosOfKey++;
		}

		if (endPosOfKey == srcLength) { //no key was found for this value
			return  "";
		}

		if (src.charAt(endPosOfKey) != '=') {
			return "";
		}

		int startOfValue = endPosOfKey + 1;

		while(src.charAt(startOfValue) == ' ') {  //trim whitespace between delimiter and value
			startOfValue++;
		}

		int endOfValue = src.indexOf(delimiter, startOfValue);
		if (endOfValue == -1) {

			endOfValue = srcLength;
		}


		while(src.charAt(endOfValue-1) == ' ') { //trim whitespace between value and next delimiter
			endOfValue--;
		}


		if (stripQuoteMarks && isQuoteMark(src.charAt(startOfValue))) {
			startOfValue++;
			endOfValue--;
		}

		return src.substring(startOfValue, endOfValue);


	}
}
