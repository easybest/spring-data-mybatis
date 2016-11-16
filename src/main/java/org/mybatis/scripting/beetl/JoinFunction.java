package org.mybatis.scripting.beetl;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.beetl.core.Context;
import org.beetl.core.Function;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JoinFunction.
 */
public class JoinFunction implements Function {
    @Override
    public Object call(Object[] paras, Context ctx) {
        Object[] objs;
        Object temp = paras[0];
        if (temp instanceof Object[]) {
            objs = (Object[]) paras[0];
        } else if (temp instanceof Collection) {
            objs = ((Collection) temp).toArray();
        } else {
            throw new RuntimeException("join.paras[0] must be a collection or an array!");
        }

        Configuration configuration = (Configuration) ctx.getGlobal("_configuration");
        List<ParameterMapping> parameterMappings = (List<ParameterMapping>) ctx.getGlobal("_parameterMappings");
        Map<String, Object> params = (Map<String, Object>) ctx.getGlobal("_params");

        int c = 0;
        StringBuilder builder = new StringBuilder();
        for (Object obj : objs) {
            builder.append("?,");
            params.put("_JOIN_C_" + c, obj);

            ParameterMapping parameterMapping = new ParameterMapping.Builder(configuration, "_JOIN_C_" + c, null == obj ? Object.class : obj.getClass()).build();
            parameterMappings.add(parameterMapping);
            c++;
        }
        builder.deleteCharAt(builder.length() - 1);

        try {
            ctx.byteWriter.writeString(builder.toString());
        } catch (IOException e) {
        }

        return null;
    }

}
