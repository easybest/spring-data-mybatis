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
import org.beetl.core.statement.VarRef;

/**
 * beetl var ref .
 *
 */
public class SQLVarRef extends VarRef {
    private String attr;

    public SQLVarRef(VarRef ref) {
        super(ref.attributes, ref.hasSafe, ref.safe, ref.token, ref.token);
        this.varIndex = ref.varIndex;
        this.attr = getAttrNameIfRoot(ref.token.text);
    }

    @Override
    public Object evaluate(Context ctx) {
        if (null != ctx && null != ctx.globalVar && "true".equals(ctx.getGlobal("_mybatis_auto_mapping"))) {
            return super.evaluate(ctx);
        }

        Object value = ctx.vars[varIndex];
        if (value == Context.NOT_EXIST_OBJECT) {
            ctx.getGlobal("_root");

        }
        return super.evaluate(ctx);
    }

    private String getAttrNameIfRoot(String name) {
        int index = name.indexOf('.');
        if (index >= 0) {
            return name.substring(0, index);
        }
        return name;
    }
}
