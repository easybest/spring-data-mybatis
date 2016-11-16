package org.mybatis.scripting.beetl;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songjiawei on 2016/11/13.
 */
public class BeetlSqlSource implements SqlSource {

    private final Configuration configuration;
    private final String        script;
    private final Class<?>      parameterType;

    public BeetlSqlSource(Configuration configuration, String script, Class<?> parameterType) {

        this.configuration = configuration;
        this.script = script;
        this.parameterType = parameterType;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("_configuration", configuration);
        context.put("_parameterMappings", parameterMappings);

        context.put("_databaseId", configuration.getDatabaseId());
        context.put("_parameter", parameterObject);
        Map<String, Object> params = new HashMap<String, Object>();
        context.put("_params", params); // 用于绑定?的实际参数值


        String sql = BeetlFacade.apply(script, context);

        BoundSql boundSql = new BoundSql(configuration, sql, parameterMappings, parameterObject);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
        }

        return boundSql;
    }
}
