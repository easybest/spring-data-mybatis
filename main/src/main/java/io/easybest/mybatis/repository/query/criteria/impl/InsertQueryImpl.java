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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Composite;
import io.easybest.mybatis.mapping.precompile.Insert;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.SafeVars;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.Trim;
import io.easybest.mybatis.repository.query.criteria.InsertQuery;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.QueryUtils;

import static io.easybest.mybatis.mapping.precompile.Constant.COMMA;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME_PURE;
import static io.easybest.mybatis.mapping.precompile.Insert.SelectKey.Order.AFTER;
import static io.easybest.mybatis.mapping.precompile.Insert.SelectKey.Order.BEFORE;
import static io.easybest.mybatis.mapping.precompile.MybatisMapperSnippet.DEFAULT_SEQUENCE_NAME;
import static io.easybest.mybatis.mapping.precompile.SQL.INSERT_INTO;
import static io.easybest.mybatis.repository.support.MybatisContext.PARAM_INSTANCE_PREFIX;
import static io.easybest.mybatis.repository.support.MybatisContext.PARAM_TENANT_ID;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public class InsertQueryImpl<T, R, F, V> implements InsertQuery<R, F, V> {

	private final Set<FieldAndValue<F>> sets = new HashSet<>();

	private final Set<ColumnAndValue> columnAndValues = new HashSet<>();

	private final Set<CustomSet> customSets = new HashSet<>();

	private final Class<T> domainClass;

	private boolean selective;

	private boolean selectKey;

	public InsertQueryImpl(Class<T> domainClass) {
		this.domainClass = domainClass;
	}

	@SuppressWarnings("unchecked")
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

	@SafeVarargs
	@Override
	public final R customSet(String columnPart, String valuePart, V... values) {

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

			this.customSets.add(new CustomSet(columnPart, valuePart, pvs));
		}
		else {
			this.customSets.add(new CustomSet(columnPart, valuePart));
		}

		return this.getReturns();
	}

	@Override
	public R selective() {

		this.selective = true;

		return this.getReturns();
	}

	@Override
	public R selectKey() {

		this.selectKey = true;

		return this.getReturns();
	}

	public Insert presupposed(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity, String id,
			String parameterType, ParamValueCallback callback) {

		Insert.Builder builder = Insert.builder().id(id);

		if (null != parameterType) {
			builder.parameterType(parameterType);
		}
		List<Segment> columns = new LinkedList<>();
		List<Segment> values = new LinkedList<>();

		if (entity.getTenantIdColumn().isPresent()) {
			columns.add(Composite.of(SQL.of(entity.getTenantIdColumn().get()), COMMA));
			values.add(Composite.of(Parameter.of(PARAM_TENANT_ID), COMMA));
		}

		this.columnAndValues.forEach(cv -> {

			Column column = cv.column;

			Parameter val = null != callback ? callback.apply(cv.value) : Parameter.of(cv.value);

			columns.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(this.selective ? 0 : 1)
					.contents(Arrays.asList(column, COMMA)).build());
			values.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(this.selective ? 0 : 1)
					.contents(Arrays.asList(val, COMMA)).build());

		});

		this.sets.forEach(fv -> {
			String field = Predicate.convertFieldName(fv.field);
			PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager.getPersistentPropertyPath(field,
					this.domainClass);
			MybatisPersistentPropertyImpl leaf = ppp.getLeafProperty();
			if (leaf.isAssociation()) {
				// TODO MANY2ONE

			}
			Column column = Column
					.of(leaf.getColumnName().getReference(entityManager.getDialect().getIdentifierProcessing()));
			Parameter val = null != callback ? callback.apply(fv.value) : Parameter.of(fv.value);
			columns.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(this.selective ? 0 : 1)
					.contents(Arrays.asList(column, COMMA)).build());
			values.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(this.selective ? 0 : 1)
					.contents(Arrays.asList(val, COMMA)).build());

		});

		this.customSets.forEach(cs -> {
			String columnPart = cs.columnPart;
			String valuePart = cs.valuePart;

			if (null != cs.values && cs.values.length > 0) {
				columnPart = QueryUtils.parse(columnPart, callback, idx -> cs.values[idx]);
				valuePart = QueryUtils.parse(valuePart, callback, idx -> cs.values[idx]);
			}

			columns.add(SQL.of(columnPart));
			values.add(SQL.of(valuePart));

		});

		builder.contents(Arrays.asList(INSERT_INTO, TABLE_NAME_PURE, SQL.of("("),
				Trim.builder().suffixOverrides(",").contents(columns).build(), SQL.of(") VALUES ("),
				Trim.builder().suffixOverrides(",").contents(values).build(), SQL.of(")")));

		if (this.selectKey) {
			builder.selectKey(this.selectKey(entityManager, entity));
		}

		return builder.build();
	}

	private Insert.SelectKey selectKey(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity) {

		if (!entity.hasIdProperty() || entity.isCompositeId()) {
			return null;
		}

		MybatisPersistentPropertyImpl idProperty = entity.getRequiredIdProperty();
		GeneratedValue gv = idProperty.findAnnotation(GeneratedValue.class);
		if (null == gv) {
			return null;
		}

		GenerationType generationType = entity.getGenerationType();

		if (generationType == GenerationType.IDENTITY) {

			return Insert.SelectKey.builder().keyProperty(PARAM_INSTANCE_PREFIX + idProperty.getName())
					.keyColumn(idProperty.getColumnName()
							.getReference(entityManager.getDialect().getIdentifierProcessing()))
					.resultType(idProperty.getJavaType()).order(AFTER)
					.contents(Collections.singletonList(SQL
							.of(entityManager.getDialect().getIdentitySelectString(entity.getTableName().getReference(),
									idProperty.getColumnName().getReference(), idProperty.getJdbcType().TYPE_CODE))))
					.build();
		}
		else if (generationType == GenerationType.SEQUENCE) {

			String sequenceName = DEFAULT_SEQUENCE_NAME;

			Map<String, String> sequenceGenerators = new HashMap<>();
			Optional.ofNullable(idProperty.findPropertyOrOwnerAnnotation(SequenceGenerators.class))
					.filter(sgs -> sgs.value().length > 0)
					.ifPresent(sgs -> Arrays.stream(sgs.value()).filter(sg -> StringUtils.hasText(sg.sequenceName()))
							.forEach(sg -> sequenceGenerators.put(sg.name(), sg.sequenceName())));
			Optional.ofNullable(idProperty.findPropertyOrOwnerAnnotation(SequenceGenerator.class))
					.filter(sg -> StringUtils.hasText(sg.sequenceName()))
					.ifPresent(sg -> sequenceGenerators.put(sg.name(), sg.sequenceName()));

			String sn = sequenceGenerators.get(gv.generator());
			if (StringUtils.hasText(sn)) {
				sequenceName = sn;
			}

			String sql = entityManager.getDialect().getSequenceNextValString(sequenceName);
			return Insert.SelectKey.builder().keyProperty(PARAM_INSTANCE_PREFIX + idProperty.getName())
					.keyColumn(idProperty.getColumnName()
							.getReference(entityManager.getDialect().getIdentifierProcessing()))
					.resultType(idProperty.getJavaType()).order(BEFORE).contents(Collections.singletonList(SQL.of(sql)))
					.build();
		}

		return null;
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

		private String columnPart;

		private String valuePart;

		private ParamValue[] values;

		public CustomSet(String columnPart, String valuePart) {
			this.columnPart = columnPart;
			this.valuePart = valuePart;
		}

	}

}
