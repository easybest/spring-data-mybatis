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

import org.beetl.core.Context;
import org.beetl.core.Function;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * Order by directive in sql.
 *
 * @author Jarvis Song
 */
public class OrderByFunction implements Function {
    @Override
    public Object call(Object[] args, Context ctx) {
        if (null == args || args.length == 0) return "";
        if (null == args[0] && args.length > 1 && !StringUtils.isEmpty(args[1])) {
            return args[1];
        }
        if (null != args[0] && (args[0] instanceof Sort)) {
            boolean sorted = false;
            Sort sort = (Sort) args[0];
            Iterator<Sort.Order> iterator = sort.iterator();
            StringBuilder builder = new StringBuilder();
            String databaseId = (String) ctx.getGlobal("_databaseId");
            while (iterator.hasNext()) {
                Sort.Order order = iterator.next();
                if ("oracle".equalsIgnoreCase(databaseId)) {
                    builder.append("\"").append(order.getProperty()).append("\"");
                } else {
                    builder.append(order.getProperty());
                }
                builder.append(" ").append(order.getDirection().name()).append(",");
                sorted = true;
            }
            if (sorted) {
                builder.deleteCharAt(builder.length() - 1);
                return " ORDER BY " + builder.toString();
            }
        }


        return "";
    }
}
