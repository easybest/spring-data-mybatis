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
import org.beetl.core.Context;
import org.beetl.core.Function;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Join directive in sql.
 *
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
