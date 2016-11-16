package org.mybatis.scripting.beetl;

import org.beetl.core.Event;
import org.beetl.core.Listener;
import org.beetl.core.statement.PlaceholderST;
import org.beetl.core.statement.VarRef;

import java.util.Stack;

/**
 * @author jarvis@ifrabbit.com
 * @date 16/3/16
 */
public class PlaceHolderListener implements Listener {
    @Override
    public Object onEvent(Event e) {
        Stack stack = (Stack) e.getEventTaget();
        Object o = stack.peek();
        if (o instanceof PlaceholderST) {
            PlaceholderST pst = (PlaceholderST) o;
            SQLPlaceholderST sst = new SQLPlaceholderST(pst);
            return sst;
        }
        if (o instanceof VarRef) {
            return new SQLVarRef((VarRef) o);
        }
        return null;
    }
}
