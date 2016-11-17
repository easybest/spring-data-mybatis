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

package org.springframework.data.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Searcher Condition.
 *
 * @author Jarvis Song
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Searchable {

    OPERATE operate() default OPERATE.EQUALS;


    String columnName() default "";

    String properyName() default "";

    String customSQL() default "";

    String alias() default "";

    enum OPERATE {
        EQUALS("="),
        NOTEQUALS("<>"),
        LIKE(" LIKE "),
        LLIKE(" LIKE "),
        RLIKE(" LIKE "),
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<="),
        CUSTOM(""),
        IN(" IN "),
        NOTIN(" NOT IN ");


        private final String oper;

        OPERATE(String oper) {
            this.oper = oper;
        }

        public String getOper() {
            return oper;
        }
    }
}
