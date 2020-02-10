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
package org.springframework.data.mybatis.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Indicates a query method should be considered as modifying query as that changes the
 * way it needs to be executed. This annotation is only considered if used on query
 * methods defined through a {@link Query} annotation). It's not applied on custom
 * implementation methods or queries derived from the method name as they already have
 * control over the underlying data access APIs or specify if they are modifying by their
 * name.
 * </p>
 * <p>
 * Queries that require a `@Modifying` annotation include {@code INSERT}, {@code UPDATE},
 * {@code DELETE}, and DDL statements.
 * </p>
 *
 * @author JARVIS SONG
 * @since 2.0.0
 * @see Query
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface Modifying {

	/**
	 * select, insert, update, delete.
	 * @return modify type
	 */
	TYPE value() default TYPE.SELECT;

	enum TYPE {

		SELECT, INSERT, UPDATE, DELETE;

	}

}
