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

import io.easybest.mybatis.repository.query.criteria.impl.CriteriaQueryImpl;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <V> value type
 */
public class LambdaCriteriaQuery<T, V> extends CriteriaQueryImpl<T, LambdaCriteriaQuery<T, V>, FieldFunction<T, ?>, V> {

	public LambdaCriteriaQuery(Class<T> domainClass) {
		super(domainClass);
	}

	@Override
	protected Conditions<LambdaCriteriaQuery<T, V>, FieldFunction<T, ?>, V> createConditionsInstance() {
		return new LambdaCriteriaQuery<>(this.domainClass);
	}

}
