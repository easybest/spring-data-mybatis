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

package io.easybest.mybatis.repository.query.criteria;

import java.util.function.Consumer;

/**
 * .
 *
 * @author Jarvis Song
 * @param <R> return type
 * @param <F> field type
 */
public interface Conditions<R, F> {

	/**
	 * Mark the next condition as OR. The default relation is AND.
	 * @return return type
	 */
	R or();

	/**
	 * Mark the next condition as AND. It's the default relation.
	 * @return return type
	 */
	R and();

	/**
	 * Nested query criteria or.
	 * @param conditions nested condition
	 * @return return type
	 */
	R or(Conditions<R, F> conditions);

	/**
	 * Nested query criteria and.
	 * @param conditions nested condition
	 * @return return type
	 */
	R and(Conditions<R, F> conditions);

	R or(Consumer<Conditions<R, F>> consumer);

	R and(Consumer<Conditions<R, F>> consumer);

	/**
	 * Equals.
	 * @param field field
	 * @param value value
	 * @return return type
	 */
	R eq(F field, Object value);

	/**
	 * Not equals.
	 * @param field field
	 * @param value value
	 * @return return type
	 */
	R ne(F field, Object value);

}
