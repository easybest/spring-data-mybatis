/*
 * Copyright 2019-2023 the original author or authors.
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
 * @param <V> value type
 */
public interface Conditions<R, F, V> {

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
	R or(Conditions<R, F, V> conditions);

	/**
	 * Nested query criteria and.
	 * @param conditions nested condition
	 * @return return type
	 */
	R and(Conditions<R, F, V> conditions);

	R or(Consumer<Conditions<R, F, V>> consumer);

	R and(Consumer<Conditions<R, F, V>> consumer);

	/**
	 * Equals.
	 * @param field field
	 * @param value value
	 * @return return type
	 */
	R eq(F field, V value);

	/**
	 * Not equals.
	 * @param field field
	 * @param value value
	 * @return return type
	 */
	R ne(F field, V value);

	R between(F field, V begin, V end);

	R gt(F field, V value);

	R ge(F field, V value);

	R lt(F field, V value);

	R le(F field, V value);

	R like(F field, V value);

	R notLike(F field, V value);

	R startingWith(F field, V value);

	R endingWith(F field, V value);

	R containing(F field, V value);

	R notContaining(F field, V value);

	R isNull(F field);

	R isNotNull(F field);

	R in(F field, V value);

	R notIn(F field, V value);

	R regex(F field, V value);

	/**
	 * Customize a query condition.
	 * @param sql predicate SQL
	 * @param values values
	 * @return return type
	 */
	@SuppressWarnings("unchecked")
	R custom(String sql, V... values);

	R ignoreCase();

	R predicate(Predicate<F> predicate);

}
