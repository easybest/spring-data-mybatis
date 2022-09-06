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

import org.springframework.data.domain.Example;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public interface CriteriaQuery<T, R, F, V> extends SelectRange<R, F>, Conditions<R, F, V>, Sorting<R, F> {

	static <T, V> LambdaCriteriaQuery<T, V> lambda(Class<T> domainClass) {

		return new LambdaCriteriaQuery<>(domainClass);
	}

	static <T, V> DefaultCriteriaQuery<T, V> create(Class<T> domainClass) {
		return new DefaultCriteriaQuery<>(domainClass);
	}

	R paging();

	<S extends T> R example(Example<S> example);

	R exampling();

}
