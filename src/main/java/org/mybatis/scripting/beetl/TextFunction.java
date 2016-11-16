package org.mybatis.scripting.beetl;

import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 * 站位符号调用此函数将直接输出文本而不是？
 *
 * @author joelli
 */
public class TextFunction implements Function {

    @Override
    public Object call(Object[] paras, Context ctx) {
        return paras[0];
    }


}
