package org.springframework.data.mybatis.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.data.mybatis.annotation.Condition.IgnoreCaseType.NEVER;
import static org.springframework.data.mybatis.annotation.Condition.Type.SIMPLE_PROPERTY;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Repeatable(Conditions.class)
public @interface Condition {

	String column() default "";

	String[] properties() default {};

	String alias() default "";

	Type type() default SIMPLE_PROPERTY;

	IgnoreCaseType ignoreCaseType() default NEVER;

	enum Type {

		BETWEEN, IS_NOT_NULL, IS_NULL, LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, BEFORE, AFTER, NOT_LIKE, LIKE, STARTING_WITH, ENDING_WITH, IS_NOT_EMPTY, NOT_CONTAINING, CONTAINING, NOT_IN, IN, NEAR, WITHIN, REGEX, EXISTS, TRUE, FALSE, NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY;

	}

	enum IgnoreCaseType {

		/**
		 * Should not ignore the sentence case.
		 */
		NEVER,

		/**
		 * Should ignore the sentence case, throwing an exception if this is not possible.
		 */
		ALWAYS,

		/**
		 * Should ignore the sentence case when possible to do so, silently ignoring the
		 * option when not possible.
		 */
		WHEN_POSSIBLE

	}

}
