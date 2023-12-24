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

import io.easybest.mybatis.mapping.precompile.Column;

/**
 * .
 *
 * @author Jarvis Song
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public interface InsertQuery<R, F, V> {

	static <T, V> LambdaInsertQuery<T, V> lambda(Class<T> domainClass) {

		return new LambdaInsertQuery<>(domainClass);
	}

	static <T, V> DefaultInsertQuery<T, V> create(Class<T> domainClass) {
		return new DefaultInsertQuery<>(domainClass);
	}

	R set(F field, V value);

	R set(Column column, V value);

	@SuppressWarnings("unchecked")
	R customSet(String columnPart, String valuePart, V... values);

	R selective();

	R selectKey();

}
