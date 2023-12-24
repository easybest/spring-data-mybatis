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
public class DefaultCriteriaQuery<T, V> extends CriteriaQueryImpl<T, DefaultCriteriaQuery<T, V>, String, V> {

	public DefaultCriteriaQuery(Class<T> domainClass) {
		super(domainClass);
	}

	@Override
	protected Conditions<DefaultCriteriaQuery<T, V>, String, V> createConditionsInstance() {
		return new DefaultCriteriaQuery<>(this.domainClass);
	}

	@Override
	protected DefaultCriteriaQuery<T, V> getReturns() {
		return this;
	}

}
