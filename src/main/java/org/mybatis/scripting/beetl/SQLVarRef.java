package org.mybatis.scripting.beetl;

import org.beetl.core.Context;
import org.beetl.core.statement.VarRef;

/**
 * @author jarvis@ifrabbit.com
 * @date 16/3/16
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
