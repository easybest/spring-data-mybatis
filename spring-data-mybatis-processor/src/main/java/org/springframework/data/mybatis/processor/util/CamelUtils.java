package org.springframework.data.mybatis.processor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class CamelUtils {

    private static final String UPPER = "\\p{Lu}|\\P{InBASIC_LATIN}";
    private static final String LOWER = "\\p{Ll}";
    private static final String CAMEL_CASE_REGEX = "(?<!(^|[%u_$]))(?=[%u])|(?<!^)(?=[%u][%l])". //
            replace("%u", UPPER).replace("%l", LOWER);

    private static final Pattern CAMEL_CASE = Pattern.compile(CAMEL_CASE_REGEX);

    public static List<String> split(String source, boolean toLower) {

        String[] parts = CAMEL_CASE.split(source);
        List<String> result = new ArrayList<>(parts.length);

        for (String part : parts) {
            result.add(toLower ? part.toLowerCase() : part);
        }

        return Collections.unmodifiableList(result);
    }

    public static String toSnake(String source) {
        return String.join("_", split(source, true));
    }

}
