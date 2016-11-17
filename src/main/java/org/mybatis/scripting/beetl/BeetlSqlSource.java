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

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Beetl script sql source.
 *
 * @author Jarvis Song
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
