package org.mybatis.scripting.beetl;

import org.beetl.core.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author jarvis@caomeitu.com
 * @date 15/12/17
 */
public class WhereTag extends Tag {
    private transient static final Logger logger = LoggerFactory.getLogger(WhereTag.class);

    @Override
    public void render() {
        String body = getBodyContent().getBody().trim();
        if (StringUtils.hasText(body)) {
            if (body.startsWith("AND ") || body.startsWith("and ")) {
                body = body.substring(4);
            }
            try {
                ctx.byteWriter.writeString(" WHERE " + body);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }
}
