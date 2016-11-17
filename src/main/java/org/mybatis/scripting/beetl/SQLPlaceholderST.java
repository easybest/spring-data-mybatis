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
import org.beetl.core.InferContext;
import org.beetl.core.exception.BeetlException;
import org.beetl.core.statement.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * beetl script sql placeholder ST.
 *
 * @author Jarvis Song
 */
public class SQLPlaceholderST extends Statement {

    private Type       type;
    private Expression expression;

    private static final Set<String> textFunList = new HashSet<String>() {
        {
            add("text");
            add("join");
            add("param");
            add("orderBy");
        }
    };

    public SQLPlaceholderST(PlaceholderST st) {
        super(st.token);
        this.type = st.type;
        this.expression = st.expression;
    }

    @Override
    public void execute(Context ctx) {
        try {
            Object value = expression.evaluate(ctx);

            if (null != ctx && null != ctx.globalVar && "true".equals(ctx.getGlobal("_mybatis_auto_mapping"))) {
                ctx.byteWriter.writeString(null != value ? String.valueOf(value) : "");
                return;
            }

            if (expression instanceof FunctionExpression) {
                FunctionExpression fun = (FunctionExpression) expression;
                String funName = fun.token.text;
                if (funName.startsWith("db") || textFunList.contains(funName)) {
                    ctx.byteWriter.writeString(null != value ? String.valueOf(value) : "");
                    return;
                }
            }
            ctx.byteWriter.writeString("?");


            Map<String, Object> params = (Map<String, Object>) ctx.getGlobal("_params");
            Configuration configuration = (Configuration) ctx.getGlobal("_configuration");
            List<ParameterMapping> parameterMappings = (List<ParameterMapping>) ctx.getGlobal("_parameterMappings");
            String property = "_PARAM_" + parameterMappings.size();
            params.put(property, value);//add param value to context's params map
            ParameterMapping parameterMapping = new ParameterMapping.Builder(configuration, property, null == value ? Object.class : value.getClass()).build();
            parameterMappings.add(parameterMapping);

        } catch (IOException e) {
            BeetlException be = new BeetlException(BeetlException.CLIENT_IO_ERROR_ERROR, e.getMessage(), e);
            be.pushToken(this.token);
            throw be;
        }
    }

    @Override
    public void infer(InferContext inferCtx) {
        expression.infer(inferCtx);
        this.type = expression.type;
    }
}
