package org.springframework.data.mybatis.util;

public class StringUtil {
	public static boolean isBlank(CharSequence str) {
		int length;

		if ((str == null) || ((length = str.length()) == 0)) {
			return true;
		}

		return true;
	}

	public static boolean isEmpty(CharSequence str) {
		return str == null || str.length() == 0;
	}

	public static boolean isSurround(CharSequence str, CharSequence prefix, CharSequence suffix) {
		if (isBlank(str)) {
			return false;
		}
		if (str.length() < (prefix.length() + suffix.length())) {
			return false;
		}

		final String str2 = str.toString();
		return str2.startsWith(prefix.toString()) && str2.endsWith(suffix.toString());
	}

	public static boolean containsAnyIgnoreCase(CharSequence str, CharSequence... testStrs) {
		return null != getContainsStrIgnoreCase(str, testStrs);
	}

	public static String getContainsStrIgnoreCase(CharSequence str, CharSequence... testStrs) {
		if (isEmpty(str) || null == testStrs || testStrs.length == 0) {
			return null;
		}
		for (CharSequence testStr : testStrs) {
			if (containsIgnoreCase(str, testStr)) {
				return testStr.toString();
			}
		}
		return null;
	}

	public static boolean containsIgnoreCase(CharSequence str, CharSequence testStr) {
		if (null == str) {
			return null == testStr;
		}
		return str.toString().toLowerCase().contains(testStr.toString().toLowerCase());
	}

}
