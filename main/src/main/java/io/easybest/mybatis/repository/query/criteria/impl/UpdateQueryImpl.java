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

package io.easybest.mybatis.repository.query.criteria.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mapping.PersistentPropertyPath;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.SafeVars;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.Update;
import io.easybest.mybatis.mapping.precompile.Where;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;
import io.easybest.mybatis.repository.query.criteria.QueryUtils;
import io.easybest.mybatis.repository.query.criteria.UpdateQuery;

import static io.easybest.mybatis.mapping.precompile.Constant.COMMA;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME_PURE;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public class UpdateQueryImpl<T, R, F, V> extends ConditionsImpl<T, R, F, V> implements UpdateQuery<R, F, V> {

	private final Set<FieldAndValue<F>> sets = new HashSet<>();

	private final Set<ColumnAndValue> columnAndValues = new HashSet<>();

	private final Set<CustomSet> customSets = new HashSet<>();

	private boolean selective;

	public UpdateQueryImpl(Class<T> domainClass) {
		super(domainClass);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected R getReturns() {
		return (R) this;
	}

	@Override
	public R set(F field, V value) {

		this.sets.add(
				new FieldAndValue<>(field, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value)));

		return this.getReturns();
	}

	@Override
	public R set(Column column, V value) {

		this.columnAndValues.add(
				new ColumnAndValue(column, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value)));

		return this.getReturns();
	}

	@SuppressWarnings("unchecked")
	@Override
	public R customSet(String sql, V... values) {

		if (null != values && values.length > 0) {

			ParamValue[] pvs = new ParamValue[values.length];
			for (int i = 0; i < values.length; i++) {
				V v = values[i];
				if (v instanceof ParamValue) {
					pvs[i] = (ParamValue) v;
				}
				else {
					pvs[i] = ParamValue.of(v);
				}
			}

			this.customSets.add(new CustomSet(sql, pvs));
		}
		else {
			this.customSets.add(new CustomSet(sql));
		}

		return this.getReturns();
	}

	@Override
	public R selective() {

		this.selective = true;

		return this.getReturns();
	}

	public Update presupposed(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity, String id,
			String parameterType, ParamValueCallback callback) {

		Update.Builder builder = Update.builder().id(id);

		if (null != parameterType) {
			builder.parameterType(parameterType);
		}

		PredicateResult pr = this.toConditionSQL(entityManager, callback, true, false);

		builder.contents(Arrays.asList(//
				SQL.UPDATE, //
				TABLE_NAME_PURE, //
				io.easybest.mybatis.mapping.precompile.Set.of(

						Stream.concat(//
								Stream.concat(//
										this.columnAndValues.stream().map(cv -> {
											Column column = cv.column;

											Parameter parameter = null != callback ? callback.apply(cv.value)
													: Parameter.of(cv.value);

											return SafeVars.builder().var(parameter.getProperty()).stripPrefix(1)
													.stripSuffix(this.selective ? 0 : 1)
													.contents(Arrays.asList(column, SQL.EQUALS, parameter, COMMA))
													.build();
										}), //
										this.sets.stream().map(fv -> {

											String field = Predicate.convertFieldName(fv.field);
											PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager
													.getPersistentPropertyPath(field, this.domainClass);
											MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
											if (leaf.isAssociation()) {
												// TODO MANY2ONE

											}
											Column column = Column.of(leaf.getColumnName().getReference(
													entityManager.getDialect().getIdentifierProcessing()));
											Parameter parameter = null != callback ? callback.apply(fv.value)
													: Parameter.of(fv.value);

											return SafeVars.builder().var(parameter.getProperty()).stripPrefix(1)
													.stripSuffix(this.selective ? 0 : 1)
													.contents(Arrays.asList(column, SQL.EQUALS, parameter, COMMA))
													.build();
										})//
								), //

								this.customSets.stream().map(cs -> {
									String sql = cs.sql;
									if (null != cs.values && cs.values.length > 0) {
										sql = QueryUtils.parse(sql, callback, idx -> cs.values[idx]);
									}
									return SQL.of(sql + ",");

								})).toArray(Segment[]::new)), //
				Where.of(//
						null == pr ? SQL.EMPTY : SQL.of(pr.getSql()), //
						this.logicDeleteClause(entity, false), //
						this.tenantIdClause(entity, false) //
				)));

		return builder.build();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class FieldAndValue<F> {

		private F field;

		private ParamValue value;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			FieldAndValue<?> that = (FieldAndValue<?>) o;
			return Objects.equals(this.field, that.field);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.field);
		}

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class ColumnAndValue {

		private Column column;

		private ParamValue value;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			ColumnAndValue that = (ColumnAndValue) o;
			return Objects.equals(this.column, that.column);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.column);
		}

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class CustomSet {

		private String sql;

		private ParamValue[] values;

		public CustomSet(String sql) {
			this.sql = sql;
		}

	}

}
