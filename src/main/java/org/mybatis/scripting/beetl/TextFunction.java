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
import org.beetl.core.Function;

/**
 * Station Symbol Call This function will output the text directly instead of ？
 *
 * @author Jarvis Song
 */
public class TextFunction implements Function {

    @Override
    public Object call(Object[] paras, Context ctx) {
        return paras[0];
    }


}
