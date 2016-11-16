package org.mybatis.scripting.beetl;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.beetl.core.Context;
import org.beetl.core.Function;

import java.util.List;
import java.util.Map;

/**
 * @author jarvis@ifrabbit.com
 * @date 16/3/17
 */
public class ParameterHandlerFunction implements Function {

    @Override
    public Object call(Object[] paras, Context ctx) {
        if (null == paras || paras.length == 0)
            throw new RuntimeException("param.paras[0] must not be empty!");

        Configuration configuration = (Configuration) ctx.getGlobal("_configuration");
        List<ParameterMapping> parameterMappings = (List<ParameterMapping>) ctx.getGlobal("_parameterMappings");
        Map<String, Object> params = (Map<String, Object>) ctx.getGlobal("_params");

        String jdbcType = paras.length > 1 ? (String) paras[1] : null, javaType = paras.length > 2 ? (String) paras[2] : null;
        String property = "_PARAM_" + parameterMappings.size();
        params.put(property, paras[0]);
        Class<?> javaTypeClass;
        try {
            javaTypeClass = null != javaType ? Class.forName(javaType) : (null == paras[0] ? Object.class : paras[0].getClass());
        } catch (ClassNotFoundException e) {
            javaTypeClass = (null == paras[0] ? Object.class : paras[0].getClass());
        }
        ParameterMapping parameterMapping = new ParameterMapping.Builder(configuration, property, javaTypeClass)
                .jdbcTypeName(jdbcType)
                .jdbcType(null != jdbcType ? JdbcType.valueOf(jdbcType) : null).build();

        parameterMappings.add(parameterMapping);

        return "?";
    }

}
