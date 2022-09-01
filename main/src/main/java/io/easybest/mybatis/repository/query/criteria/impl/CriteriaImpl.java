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

package io.easybest.mybatis.repository.query.criteria.impl;

import io.easybest.mybatis.repository.query.criteria.Conditions;
import io.easybest.mybatis.repository.query.criteria.Criteria;

import org.springframework.data.mapping.MappingException;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 */
public class CriteriaImpl<T, R, F> extends ConditionsImpl<T, R, F> implements Criteria<R, F> {

	public CriteriaImpl(Class<T> domainClass) {
		super(domainClass);
	}

	public Class<T> getDomainClass() {
		return this.domainClass;
	}

	@Override
	public R returns(R returns) {

		if (null != this.condition) {
			throw new MappingException("Setting returns must be the first.");
		}

		this.returns = returns;
		return this.returns;
	}

	@SafeVarargs
	@Override
	public final R select(F... fields) {

		return this.returns;
	}

	@SafeVarargs
	@Override
	public final R exclude(F... fields) {

		return this.returns;
	}

	@Override
	protected Conditions<R, F> createConditionsInstance() {
		return new CriteriaImpl<>(this.domainClass);
	}

}
