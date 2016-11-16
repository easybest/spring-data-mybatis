package org.springframework.data.mybatis.repository.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by songjiawei on 2016/11/13.
 */
public abstract class StringUtils {

    /**
     * 下划线字符
     */
    public static final char UNDERLINE = '_';

    /**
     * 空字符串
     */
    public static final String EMPTY_STRING = "";

    /**
     * <p>
     * 判断字符串是否为空
     * </p>
     *
     * @param str 需要判断字符串
     * @return 判断结果
     */
    public static boolean isEmpty(String str) {
        return str == null || EMPTY_STRING.equals(str.trim());
    }

    /**
     * <p>
     * 判断字符串是否不为空
     * </p>
     *
     * @param str 需要判断字符串
     * @return 判断结果
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * <p>
     * 字符串驼峰转下划线格式
     * </p>
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String camelToUnderline(String param) {
        if (isEmpty(param)) {
            return EMPTY_STRING;
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append(UNDERLINE);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * <p>
     * 字符串下划线转驼峰格式
     * </p>
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String underlineToCamel(String param) {
        if (isEmpty(param)) {
            return EMPTY_STRING;
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == UNDERLINE) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * 判断字符串是否为纯大写字母
     * </p>
     *
     * @param str 要匹配的字符串
     * @return
     */
    public static boolean isUpperCase(String str) {
        return match("^[A-Z]+$", str);
    }

    /**
     * <p>
     * 正则表达式匹配
     * </p>
     *
     * @param regex 正则表达式字符串
     * @param str   要匹配的字符串
     * @return 如果str 符合 regex的正则表达式格式,返回true, 否则返回 false;
     */
    public static boolean match(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

}
