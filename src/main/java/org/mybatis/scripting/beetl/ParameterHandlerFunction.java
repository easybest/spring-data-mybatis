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

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.beetl.core.Context;
import org.beetl.core.Function;

import java.util.List;
import java.util.Map;

/**
 * param directive in sql.
 *
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
