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

import org.beetl.core.Event;
import org.beetl.core.Listener;
import org.beetl.core.statement.PlaceholderST;
import org.beetl.core.statement.VarRef;

import java.util.Stack;

/**
 * Place holder listener in beetl sql script.
 *
 * @author Jarvis Song.
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
