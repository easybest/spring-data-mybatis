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

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import lombok.Data;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Bind;
import io.easybest.mybatis.mapping.precompile.Choose;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Escape;
import io.easybest.mybatis.mapping.precompile.Foreach;
import io.easybest.mybatis.mapping.precompile.Function;
import io.easybest.mybatis.mapping.precompile.MethodInvocation;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;

import static io.easybest.mybatis.repository.query.criteria.Operator.AND;
import static io.easybest.mybatis.repository.query.criteria.Operator.OR;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.CUSTOM;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.ENDING_WITH;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.GREATER_THAN_EQUAL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.LESS_THAN_EQUAL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_CONTAINING;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_IN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_LIKE;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.SIMPLE_PROPERTY;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.STARTING_WITH;

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

	private String customSql;

	private int idx;

	private Predicate() {
	}

	public static <F> Predicate<F> of(String customSql, ParamValue... values) {

		Predicate<F> predicate = new Predicate<>();
		predicate.setCustomSql(customSql);
		predicate.setType(CUSTOM);
		predicate.setValues(values);
		return predicate;
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

		if (predicate.type == CUSTOM) {

			String sql = predicate.customSql;
			// Parse parameters, such as ?1 or :param
			builder.append(QueryUtils.parse(sql, callback, index -> predicate.get(group, predicate.idx, index)));

			return new PredicateResult(builder.toString());
		}

		PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp;
		if (predicate.field instanceof PersistentPropertyPath) {
			ppp = (PersistentPropertyPath<MybatisPersistentPropertyImpl>) predicate.field;
		}
		else {
			ppp = entityManager.getPersistentPropertyPath(predicate.fieldName, domainClass);
		}
		MybatisPersistentPropertyImpl leaf = ppp.getLeafProperty();

		Column column;
		Parameter parameter;
		ParamValue pv;
		Set<String> connectors = null;

		if (leaf.isAssociation()) {
			// TODO

			MybatisAssociation association = leaf.getRequiredAssociation();
			switch (predicate.type) {
			case CONTAINING:
			case NOT_CONTAINING:
				String sql = null;
				MybatisAssociation.JoinTable joinTable = association.getJoinTable();
				if (null != joinTable) {

					pv = predicate.get(group, predicate.idx, 0);
					if (null != callback) {
						parameter = callback.apply(pv);
					}
					else {
						parameter = Parameter.of(pv);
					}

					sql = "(SELECT "
							+ joinTable.getJoinColumns()[0].getColumnName()
									.getReference(entityManager.getDialect().getIdentifierProcessing())
							+ " FROM "
							+ joinTable.getTable().getReference(entityManager.getDialect().getIdentifierProcessing())
							+ " WHERE "
							+ joinTable.getInverseJoinColumns()[0]
									.getColumnName().getReference(entityManager.getDialect().getIdentifierProcessing())
							+ "="
							+ Parameter.of(
									parameter.getProperty() + "."
											+ association.getTargetEntity().getRequiredIdProperty().getName(),
									parameter)
							+ ")";
				}
				// TODO ONE2MANY

				column = Column.of(SQL.ROOT_ALIAS.toString(), entityManager.getRequiredPersistentEntity(domainClass)
						.getRequiredIdProperty().getColumnName().getReference());
				return new PredicateResult(
						column + (predicate.type.equals(NOT_CONTAINING) ? " NOT IN " : " IN ") + sql);

			}

		}

		SqlIdentifier columnName = leaf.getColumnName();

		if (alias) {
			SqlIdentifier tableAlias = SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue());
			String tablePath = ppp.toDotPath(source -> source.isAssociation() ? source.getName() : null);
			// if (null != tablePath) {
			if (StringUtils.hasText(tablePath)) {
				tableAlias = SqlIdentifier.quoted(tablePath);
			}

			column = Column.of(tableAlias.toSql(entityManager.getDialect().getIdentifierProcessing()),
					columnName.getReference(entityManager.getDialect().getIdentifierProcessing()));
			connectors = Syntax.connectors(entityManager, ppp);
		}
		else {
			column = Column.of(columnName.getReference(entityManager.getDialect().getIdentifierProcessing()));
		}

		switch (predicate.type) {
		case BETWEEN:
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
			builder.append("BETWEEN").append(" ");

			ParamValue pv1 = predicate.get(group, predicate.idx, 0);
			ParamValue pv2 = predicate.get(group, predicate.idx, 1);

			Parameter p1;
			Parameter p2;
			if (null != callback) {
				p1 = callback.apply(pv1);
				p2 = callback.apply(pv2);
			}
			else {
				p1 = Parameter.of(pv1);
				p2 = Parameter.of(pv2);
			}

			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, p1));
			builder.append(" AND ");
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, p2));
			break;

		case AFTER:
		case GREATER_THAN:
		case GREATER_THAN_EQUAL:
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
			builder.append(predicate.type.equals(GREATER_THAN_EQUAL) //
					? (tr ? SQL.GREATER_THAN_EQUAL_TR : SQL.GREATER_THAN_EQUAL) //
					: (tr ? SQL.GREATER_THAN_TR : SQL.GREATER_THAN));

			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}

			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, parameter));
			break;

		case BEFORE:
		case LESS_THAN:
		case LESS_THAN_EQUAL:
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
			builder.append(predicate.type.equals(LESS_THAN_EQUAL) //
					? (tr ? SQL.LESS_THAN_EQUAL_TR : SQL.LESS_THAN_EQUAL) //
					: (tr ? SQL.LESS_THAN_TR : SQL.LESS_THAN));

			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}

			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, parameter));
			break;

		case IS_NULL:
			builder.append(column).append(" IS NULL");
			break;

		case IS_NOT_NULL:
			builder.append(column).append(" IS NOT NULL");
			break;

		case IN:
		case NOT_IN:
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
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

		case STARTING_WITH:
		case ENDING_WITH:
		case CONTAINING:
		case NOT_CONTAINING:
			if (leaf.isCollectionLike()) {
				// TODO
				return null;
			}

			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}

			Bind bind = Bind.of("__bindable_" + parameter.getProperty(),
					(predicate.type.equals(STARTING_WITH) ? "" : "'%'+") + "entityManager.escapeCharacter.escape("
							+ parameter.getProperty() + ")" + (predicate.type.equals(ENDING_WITH) ? "" : "+'%'"));
			builder.append(bind);
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
			builder.append(predicate.type.equals(NOT_CONTAINING) ? SQL.NOT_LIKE : SQL.LIKE).append(" ");
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager,
					Parameter.of("__bindable_" + parameter.getProperty()))).append(" ");
			builder.append(Escape.of(entityManager.getDialect()));
			break;

		case LIKE:
		case NOT_LIKE:
			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column)).append(" ");
			builder.append(predicate.type.equals(NOT_LIKE) ? SQL.NOT_LIKE : SQL.LIKE).append(" ");
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, parameter)).append(" ");
			builder.append(Escape.of(entityManager.getDialect()));
			break;

		case TRUE:
			builder.append(column).append(SQL.EQUALS)
					.append(entityManager.getDialect().supportsBoolean() ? SQL.TRUE : SQL.of("1"));
			break;

		case FALSE:
			builder.append(column).append(SQL.EQUALS)
					.append(entityManager.getDialect().supportsBoolean() ? SQL.FALSE : SQL.of("0"));
			break;

		case SIMPLE_PROPERTY:
		case NEGATING_SIMPLE_PROPERTY:
			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, column));
			builder.append(
					predicate.type.equals(SIMPLE_PROPERTY) ? SQL.EQUALS : (tr ? SQL.NOT_EQUALS_TR : SQL.NOT_EQUALS));

			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}

			builder.append(this.lowerIfIgnoreCase(predicate, entityManager, parameter));
			break;

		case REGEX:
			pv = predicate.get(group, predicate.idx, 0);
			if (null != callback) {
				parameter = callback.apply(pv);
			}
			else {
				parameter = Parameter.of(pv);
			}
			builder.append(entityManager.getDialect().regexpLike(column.toString(), parameter.toString()));
			break;

		case IS_EMPTY:
		case IS_NOT_EMPTY:
			if (!leaf.isCollectionLike()) {
				throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties!");
			}

			// TODO

			break;
		default:
			throw new IllegalArgumentException("Unsupported keyword " + predicate.type);
		}

		return new PredicateResult(builder.toString(), connectors);
	}

	private Segment lowerIfIgnoreCase(Predicate<F> predicate, EntityManager entityManager, Segment segment) {

		return predicate.ignoreCase ? Function.of(entityManager.getDialect().getFunction("lower"), segment) : segment;
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
			pv.setName((null != this.fieldName ? this.fieldName : "nofield") + "_" + group + "_" + idx);
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
