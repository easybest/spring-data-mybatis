/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to declare finder queries directly on repository methods.
 *
 * @author Jarvis Song
 * @since 2.0.0
 * @see Modifying
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@QueryAnnotation
@Documented
public @interface Query {

	/**
	 * Query string.
	 * @return query sql
	 */
	String value() default "";

	/**
	 * In page query, define the count query string.
	 * @return count query string
	 */
	String countQuery() default "";

	/**
	 * Count projection.
	 * @return count projection
	 */
	String countProjection() default "";

	/**
	 * Named query's key.
	 * @return named query
	 */
	String name() default "";

	/**
	 * Named count query's name.
	 * @return named query for count
	 */
	String countName() default "";

	/**
	 * Statement's name. If statement has value, will not pre create any statement and use
	 * the original statement in the user defined mapper files.
	 * @return statement name
	 */
	String statement() default "";

	/**
	 * Count statement's name.
	 * @return count statement
	 */
	String countStatement() default "";

	/**
	 * Mapper namespace.
	 * @return namespace
	 */
	String namespace() default "";

	boolean jpaStyle() default false;

}
