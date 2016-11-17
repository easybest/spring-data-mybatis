/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.mybatis.scripting.beetl;

import org.beetl.core.Tag;

import java.io.IOException;
import java.util.Map;

/**
 * Implementation of mybatis trim. <code>
 * &lt;trim prefix="WHERE" prefixOverrides="AND |OR"&gt; &lt;/trim&gt;
 * &lt;trim prefix="SET" suffixOverrides=","&gt;&lt;/trim&gt;
 * </code>
 *
 * @author zhoupan
 */
public class TrimTag extends Tag {

    /**
     * The Constant SPACE.
     */
    public static final String SPACE = " ";

    /**
     * The Constant SEPARATOR_CHAR.
     */
    public static final char SEPARATOR_CHAR = '|';

    /**
     * The Constant SUFFIX_OVERRIDES.
     */
    public static final String SUFFIX_OVERRIDES = "suffixOverrides";

    /**
     * The Constant PREFIX_OVERRIDES.
     */
    public static final String PREFIX_OVERRIDES = "prefixOverrides";

    /**
     * The Constant SUFFIX.
     */
    public static final String SUFFIX = "suffix";

    /**
     * The Constant PREFIX.
     */
    public static final String PREFIX = "prefix";

    /**
     * The prefix.
     */
    private String prefix = "";

    /**
     * The prefix overrides.
     */
    private String[] prefixOverrides;

    /**
     * The suffix overrides.
     */
    private String[] suffixOverrides;

    /**
     * The suffix.
     */
    private String suffix = "";

    /*
     * (non-Javadoc)
     *
     * @see org.beetl.core.Tag#render()
     */
    public void render() {
        try {
            Object[] args = this.args;
            if (args != null && args.length != 0) {
                initTrimArgs(args);
                StringBuilder sb = buildTrimContent();
                this.ctx.byteWriter.writeString(sb.toString());
            } else {
                // 兼容老版本 trim.
                String sql = getBodyContent().getBody().trim();
                if (sql.endsWith(",")) {
                    this.ctx.byteWriter.writeString(sql.substring(0, sql.length() - 1));
                } else {
                    this.ctx.byteWriter.writeString(sql);
                }
            }
        } catch (IOException ie) {
            ie.printStackTrace();
        }

    }

    /**
     * Builds the trim content.
     *
     * @return the string builder
     */
    protected StringBuilder buildTrimContent() {
        StringBuilder sb = new StringBuilder();
        String sql = getBodyContent().getBody();
        boolean isSqlBlank = this.isSqlBlank(sql);
        if (!isSqlBlank) {
            // prefix
            if (StringUtils.isNotBlank(this.prefix)) {
                appendSql(sb, this.prefix);
            }
            // prefixOverrides
            String trimSql = " " + StringUtils.trim(sql);
            if (this.prefixOverrides != null && this.prefixOverrides.length > 0) {
                for (String prefixOverride : this.prefixOverrides) {
                    if (StringUtils.startsWith(trimSql, prefixOverride, true)) {
                        trimSql = StringUtils.substringAfter(trimSql, prefixOverride);
                    }
                }
            }
            // suffixOverrides
            if (this.suffixOverrides != null && this.suffixOverrides.length > 0) {
                for (String suffixOverride : this.suffixOverrides) {
                    if (StringUtils.endsWith(trimSql, suffixOverride, true)) {
                        trimSql = StringUtils.substringBeforeLast(trimSql, suffixOverride);
                    }
                }
            }
            this.appendSql(sb, trimSql);
            // suffix
            if (StringUtils.isNotBlank(this.suffix)) {
                appendSql(sb, this.suffix);
            }
        }
        return sb;
    }

    /**
     * init param.
     *
     * @param args the args
     */
    @SuppressWarnings("unchecked")
    protected void initTrimArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Map) {
                Map<String, Object> params = (Map<String, Object>) arg;
                if (params.containsKey(PREFIX)) {
                    this.prefix = (String) params.get(PREFIX);
                }
                if (params.containsKey(SUFFIX)) {
                    this.suffix = (String) params.get(SUFFIX);
                }
                if (params.containsKey(PREFIX_OVERRIDES)) {
                    this.prefixOverrides = StringUtils.split((String) params.get(PREFIX_OVERRIDES), SEPARATOR_CHAR);
                }
                if (params.containsKey(SUFFIX_OVERRIDES)) {
                    this.suffixOverrides = StringUtils.split((String) params.get(SUFFIX_OVERRIDES), SEPARATOR_CHAR);
                }
            }
        }
    }

    /**
     * Append sql.
     *
     * @param sb  the sb
     * @param sql the sql
     */
    protected void appendSql(StringBuilder sb, String sql) {
        if (StringUtils.isNotBlank(sql)) {
            sb.append(SPACE).append(sql).append(SPACE);
        }
    }

    /**
     * Checks if is sql blank.
     *
     * @param sql the sql
     * @return true, if checks if is sql blank
     */
    protected boolean isSqlBlank(String sql) {
        if (StringUtils.isBlank(sql)) {
            return true;
        }
        return StringUtils.trim(sql).isEmpty();
    }


}
