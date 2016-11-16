package org.springframework.data.mybatis.repository.query;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Created by songjiawei on 2016/11/10.
 */
public abstract class QueryUtils {

    private static final String  IDENTIFIER      = "[\\p{Lu}\\P{InBASIC_LATIN}\\p{Alnum}._$]+";
    private static final Pattern NAMED_PARAMETER = Pattern.compile(":" + IDENTIFIER + "|\\#" + IDENTIFIER,
            CASE_INSENSITIVE);

    public static boolean hasNamedParameter(String query) {
        return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
    }

}
