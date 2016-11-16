package org.mybatis.scripting.beetl;

import org.beetl.core.Context;
import org.beetl.core.Function;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.Iterator;

/**
 * @author jarvis@ifrabbit.com
 * @date 16/3/18
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
