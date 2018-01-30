package org.springframework.data.mybatis.util;

import java.util.Iterator;

/**
 * @author Jarvis Song
 */
public class StringUtil {

	public static boolean isEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static String join(String seperator, String[] strings) {
		int length = strings.length;
		if (length == 0) {
			return "";
		}
		// Allocate space for length * firstStringLength;
		// If strings[0] is null, then its length is defined as 4, since that's the
		// length of "null".
		final int firstStringLength = strings[0] != null ? strings[0].length() : 4;
		StringBuilder buf = new StringBuilder(length * firstStringLength).append(strings[0]);
		for (int i = 1; i < length; i++) {
			buf.append(seperator).append(strings[i]);
		}
		return buf.toString();
	}

	public static String join(String seperator, Iterator objects) {
		StringBuilder buf = new StringBuilder();
		if (objects.hasNext()) {
			buf.append(objects.next());
		}
		while (objects.hasNext()) {
			buf.append(seperator).append(objects.next());
		}
		return buf.toString();
	}

	public static String join(String separator, Iterable objects) {
		return join(separator, objects.iterator());
	}

	public static String replaceOnce(String template, String placeholder, String replacement) {
		if (template == null) {
			return null; // returnign null!
		}
		int loc = template.indexOf(placeholder);
		if (loc < 0) {
			return template;
		} else {
			return template.substring(0, loc) + replacement + template.substring(loc + placeholder.length());
		}
	}

}
