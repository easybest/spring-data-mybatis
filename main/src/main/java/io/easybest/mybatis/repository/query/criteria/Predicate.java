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

import java.io.Serializable;
import java.util.Collections;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Choose;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Foreach;
import io.easybest.mybatis.mapping.precompile.Function;
import io.easybest.mybatis.mapping.precompile.MethodInvocation;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import lombok.Data;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.lang.Nullable;

import static io.easybest.mybatis.repository.query.criteria.Operator.AND;
import static io.easybest.mybatis.repository.query.criteria.Operator.OR;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_IN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.SIMPLE_PROPERTY;

/**
 * .
 *
 * @author Jarvis Song
 * @param <F> field type
 */
@Data
public final class Predicate<F> implements Serializable {

	private F field;

	private PredicateType type;

	@Nullable
	private ParamValue[] values;

	private Operator operator;

	private Predicate<F> opposite;

	private Predicate<F> pointer = this;

	private String fieldName;

	private boolean ignoreCase;

	private int idx;

	private Predicate() {
	}

	public static <F> Predicate<F> of(F field, PredicateType type, boolean ignoreCase, ParamValue... values) {

		Predicate<F> predicate = new Predicate<>();
		predicate.setField(field);
		predicate.setType(type);
		predicate.setValues(values);
		predicate.setFieldName(convertFieldName(field));
		predicate.setIgnoreCase(ignoreCase);
		return predicate;
	}

	public PredicateResult toSQL(int group, EntityManager entityManager, Class<?> domainClass,
			ParamValueCallback callback, boolean tr, boolean alias) {

		Predicate<F> opposite = this.opposite;

		PredicateResult result = this.toSQL(group, entityManager, domainClass, this, callback, tr, alias);

		while (null != opposite) {

			result.append(opposite.operator,
					this.toSQL(group, entityManager, domainClass, opposite, callback, tr, alias));

			opposite = opposite.opposite;
		}
		return result;
	}

	@SuppressWarnings({ "unchecked" })
	private PredicateResult toSQL(int group, EntityManager entityManager, Class<?> domainClass, Predicate<F> predicate,
			ParamValueCallback callback, boolean tr, boolean alias) {

		StringBuilder builder = new StringBuilder();

		PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp;
		if (predicate.field instanceof PersistentPropertyPath) {
			ppp = (PersistentPropertyPath<MybatisPersistentPropertyImpl>) predicate.field;
		}
		else {
			ppp = entityManager.getPersistentPropertyPath(predicate.fieldName, domainClass);
		}
		MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();

		SqlIdentifier tableAlias = SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue());

		if (leaf.isAssociation()) {
			// TODO
		}

		Column column = Column.of(
				alias ? tableAlias.getReference(entityManager.getDialect().getIdentifierProcessing()) : null,
				leaf.getColumnName().getReference());
		Parameter parameter;
		ParamValue pv;

		switch (predicate.type) {
		case IN:
		case NOT_IN:
			builder.append(predicate.ignoreCase ? this.lowerIfIgnoreCase(entityManager, column) : column).append(" ");
			builder.append(predicate.type.equals(NOT_IN) ? SQL.of("NOT IN") : SQL.of("IN")).append(" ");

			pv = predicate.get(group, predicate.idx, 0);

			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}
			builder.append(Choose.of(MethodInvocation.of(Syntax.class, "isEmpty", parameter.getProperty()).toString(),
					SQL.of("(NULL)"), Foreach.builder().collection(parameter.getProperty())
							.contents(Collections.singletonList(Parameter.of("item"))).build()));
			break;
		case SIMPLE_PROPERTY:
		case NEGATING_SIMPLE_PROPERTY:
			builder.append(predicate.ignoreCase ? this.lowerIfIgnoreCase(entityManager, column) : column);
			builder.append(
					predicate.type.equals(SIMPLE_PROPERTY) ? SQL.EQUALS : (tr ? SQL.NOT_EQUALS_TR : SQL.NOT_EQUALS));

			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}

			builder.append(predicate.ignoreCase ? this.lowerIfIgnoreCase(entityManager, parameter) : parameter);
			break;
		}

		return new PredicateResult(builder.toString());
	}

	private Segment lowerIfIgnoreCase(EntityManager entityManager, Segment segment) {

		return Function.of(entityManager.getDialect().getFunction("lower"), segment);
	}

	private ParamValue get(int group, int idx, int i) {

		if (null == this.values || this.values.length == 0) {
			throw new MappingException("Required param values");
		}

		if (i > this.values.length - 1) {
			throw new MappingException("Exceed param values length");
		}
		ParamValue pv = this.values[i];
		if (null == pv.getName()) {
			pv.setName(this.fieldName + "_" + group + "_" + idx);
		}
		return pv;
	}

	public Predicate<F> opposing(Operator operator, Predicate<F> opposite) {

		this.pointer.opposite = opposite;
		this.pointer.opposite.operator = operator;
		this.pointer.opposite.idx = this.pointer.idx + 1;
		this.pointer = opposite;
		return this;
	}

	public Predicate<F> and(Predicate<F> opposite) {

		return this.opposing(AND, opposite);

	}

	public Predicate<F> or(Predicate<F> opposite) {

		return this.opposing(OR, opposite);
	}

	public static <F> String convertFieldName(F field) {
		String f = null;

		if (field instanceof String) {
			f = (String) field;
		}

		else if (field instanceof FieldFunction) {
			f = LambdaUtils.getFieldName((FieldFunction<?, ?>) field);
		}
		else if (field instanceof PersistentPropertyPath) {
			f = ((PersistentPropertyPath<?>) field).toDotPath();
		}
		if (null == f) {
			throw new MappingException("Error field was found in the criteria: " + field);
		}

		return f;
	}

	@Override
	public String toString() {
		return "Predicate{" + "fieldName='" + this.fieldName + '\'' + '}';
	}

}
