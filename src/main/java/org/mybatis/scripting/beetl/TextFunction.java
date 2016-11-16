package org.mybatis.scripting.beetl;

import org.beetl.core.Context;
import org.beetl.core.Function;

/**
 * Station Symbol Call This function will output the text directly instead of ？
 *
 * @author joelli
 */
public class TextFunction implements Function {

    @Override
    public Object call(Object[] paras, Context ctx) {
        return paras[0];
    }


}
