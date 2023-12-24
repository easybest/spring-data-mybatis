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

import java.util.Arrays;
import java.util.List;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Delete;
import io.easybest.mybatis.mapping.precompile.Include;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.SqlDefinition;
import io.easybest.mybatis.mapping.precompile.Update;
import io.easybest.mybatis.mapping.precompile.Where;
import io.easybest.mybatis.repository.query.criteria.DeleteQuery;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public class DeleteQueryImpl<T, R, F, V> extends ConditionsImpl<T, R, F, V> implements DeleteQuery<R, F, V> {

	public DeleteQueryImpl(Class<T> domainClass) {
		super(domainClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected R getReturns() {
		return (R) this;
	}

	public SqlDefinition presupposed(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity, String id,
			String parameterType, ParamValueCallback callback) {
		return this.presupposed(entityManager, entity, id, parameterType, callback, null);
	}

	public SqlDefinition presupposed(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity, String id,
			String parameterType, ParamValueCallback callback, final List<? extends Segment> derived) {

		Delete.Builder builder = Delete.builder().id(id);

		if (null != parameterType) {
			builder.parameterType(parameterType);
		}

		PredicateResult pr = this.toConditionSQL(entityManager, callback, true, false);

		if (entity.getLogicDeleteColumn().isPresent()) {

			Column col = Column.of(entity.getLogicDeleteColumn().get());

			return Update.builder().id(id).parameterType(parameterType).contents(Arrays.asList(//
					SQL.UPDATE, //
					Include.TABLE_NAME_PURE, //
					io.easybest.mybatis.mapping.precompile.Set.of(SQL.of(col + " = 1")), //
					Where.of(//
							null == pr ? SQL.EMPTY : SQL.of(pr.getSql()) //
					))).build();
		}

		return Delete.builder().id(id).parameterType(parameterType).contents(Arrays.asList(//
				SQL.DELETE_FROM, //
				Include.TABLE_NAME_PURE, //
				Where.of(//
						null == pr ? SQL.EMPTY : SQL.of(pr.getSql()) //
				))).derived(derived).build();

	}

}
