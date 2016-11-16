package org.mybatis.scripting.beetl;

import org.beetl.core.Tag;

import java.io.IOException;
import java.util.Map;

/**
 * 实现mybatis trim. <code>
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
            if (StringKit.isNotBlank(this.prefix)) {
                appendSql(sb, this.prefix);
            }
            // prefixOverrides
            String trimSql = " " + StringKit.trim(sql);
            if (this.prefixOverrides != null && this.prefixOverrides.length > 0) {
                for (String prefixOverride : this.prefixOverrides) {
                    if (StringKit.startsWith(trimSql, prefixOverride, true)) {
                        trimSql = StringKit.substringAfter(trimSql, prefixOverride);
                    }
                }
            }
            // suffixOverrides
            if (this.suffixOverrides != null && this.suffixOverrides.length > 0) {
                for (String suffixOverride : this.suffixOverrides) {
                    if (StringKit.endsWith(trimSql, suffixOverride, true)) {
                        trimSql = StringKit.substringBeforeLast(trimSql, suffixOverride);
                    }
                }
            }
            this.appendSql(sb, trimSql);
            // suffix
            if (StringKit.isNotBlank(this.suffix)) {
                appendSql(sb, this.suffix);
            }
        }
        return sb;
    }

    /**
     * 初始化参数.
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
                    this.prefixOverrides = StringKit.split((String) params.get(PREFIX_OVERRIDES), SEPARATOR_CHAR);
                }
                if (params.containsKey(SUFFIX_OVERRIDES)) {
                    this.suffixOverrides = StringKit.split((String) params.get(SUFFIX_OVERRIDES), SEPARATOR_CHAR);
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
        if (StringKit.isNotBlank(sql)) {
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
        if (StringKit.isBlank(sql)) {
            return true;
        }
        return StringKit.trim(sql).isEmpty();
    }


}
