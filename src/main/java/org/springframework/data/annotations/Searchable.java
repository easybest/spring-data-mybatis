package org.springframework.data.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Searcher Condition.
 *
 * @author jarvis@caomeitu.com
 * @date 15/9/30
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
