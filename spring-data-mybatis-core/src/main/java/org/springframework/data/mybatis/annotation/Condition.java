/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify query condition.
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Conditions.class)
public @interface Condition {

	String column() default "";

	String[] properties() default {};

	String alias() default "";

	Type type() default Type.SIMPLE_PROPERTY;

	IgnoreCaseType ignoreCaseType() default IgnoreCaseType.NEVER;

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
