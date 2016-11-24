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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * where directive.
 *
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
