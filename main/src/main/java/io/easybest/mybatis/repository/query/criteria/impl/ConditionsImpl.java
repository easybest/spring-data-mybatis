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

import java.util.function.Consumer;

import org.springframework.data.mapping.MappingException;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.repository.query.criteria.Condition;
import io.easybest.mybatis.repository.query.criteria.Conditions;
import io.easybest.mybatis.repository.query.criteria.Operator;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;
import io.easybest.mybatis.repository.query.criteria.PredicateType;

import static io.easybest.mybatis.repository.query.criteria.Operator.AND;
import static io.easybest.mybatis.repository.query.criteria.Operator.OR;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.BETWEEN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.CONTAINING;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.ENDING_WITH;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.GREATER_THAN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.GREATER_THAN_EQUAL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.IN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.IS_NOT_NULL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.IS_NULL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.LESS_THAN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.LESS_THAN_EQUAL;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.LIKE;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NEGATING_SIMPLE_PROPERTY;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_CONTAINING;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_IN;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.NOT_LIKE;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.REGEX;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.SIMPLE_PROPERTY;
import static io.easybest.mybatis.repository.query.criteria.PredicateType.STARTING_WITH;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public class ConditionsImpl<T, R, F, V> implements Conditions<R, F, V> {

	protected final Class<T> domainClass;

	/**
	 * Conditions tree.
	 */
	protected Condition<F> condition;

	protected Operator flushOperator;

	protected Predicate<F> pointer;

	public ConditionsImpl(Class<T> domainClass) {

		this.domainClass = domainClass;
	}

	@SuppressWarnings("unchecked")
	protected R getReturns() {
		return (R) this;
	}

	public PredicateResult toConditionSQL(EntityManager entityManager, ParamValueCallback callback, boolean tr,
			boolean alias) {
		if (null == this.condition) {
			return null;
		}
		return this.condition.toSQL(entityManager, this.domainClass, callback, tr, alias);
	}

	@Override
	public R or() {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.flushOperator = OR;

		return this.getReturns();
	}

	@Override
	public R and() {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.flushOperator = AND;

		return this.getReturns();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public R or(Conditions<R, F, V> conditions) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.condition.or(((ConditionsImpl<T, R, F, V>) conditions).condition);

		return this.getReturns();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public R and(Conditions<R, F, V> conditions) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.condition.and(((ConditionsImpl<T, R, F, V>) conditions).condition);

		return this.getReturns();
	}

	@Override
	public R or(Consumer<Conditions<R, F, V>> consumer) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		Conditions<R, F, V> nested = this.createConditionsInstance();
		consumer.accept(nested);

		return this.or(nested);
	}

	@Override
	public R and(Consumer<Conditions<R, F, V>> consumer) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		Conditions<R, F, V> nested = this.createConditionsInstance();
		consumer.accept(nested);

		return this.and(nested);
	}

	protected Conditions<R, F, V> createConditionsInstance() {
		return new ConditionsImpl<>(this.domainClass);
	}

	@Override
	public R eq(F field, V value) {

		this.addPredicate(field, SIMPLE_PROPERTY,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R ne(F field, V value) {

		this.addPredicate(field, NEGATING_SIMPLE_PROPERTY,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R between(F field, V begin, V end) {

		this.addPredicate(field, BETWEEN, (begin instanceof ParamValue) ? (ParamValue) begin : ParamValue.of(begin),
				(end instanceof ParamValue) ? (ParamValue) end : ParamValue.of(end));

		return this.getReturns();
	}

	@Override
	public R gt(F field, V value) {

		this.addPredicate(field, GREATER_THAN,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R ge(F field, V value) {

		this.addPredicate(field, GREATER_THAN_EQUAL,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R lt(F field, V value) {

		this.addPredicate(field, LESS_THAN, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R le(F field, V value) {

		this.addPredicate(field, LESS_THAN_EQUAL,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R like(F field, V value) {

		this.addPredicate(field, LIKE, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R notLike(F field, V value) {

		this.addPredicate(field, NOT_LIKE, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R startingWith(F field, V value) {

		this.addPredicate(field, STARTING_WITH,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R endingWith(F field, V value) {

		this.addPredicate(field, ENDING_WITH,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R containing(F field, V value) {

		this.addPredicate(field, CONTAINING, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R notContaining(F field, V value) {

		this.addPredicate(field, NOT_CONTAINING,
				(value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R isNull(F field) {

		this.addPredicate(field, IS_NULL);

		return this.getReturns();
	}

	@Override
	public R isNotNull(F field) {

		this.addPredicate(field, IS_NOT_NULL);

		return this.getReturns();
	}

	@Override
	public R in(F field, V value) {

		this.addPredicate(field, IN, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R notIn(F field, V value) {

		this.addPredicate(field, NOT_IN, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R regex(F field, V value) {

		this.addPredicate(field, REGEX, (value instanceof ParamValue) ? (ParamValue) value : ParamValue.of(value));

		return this.getReturns();
	}

	@Override
	public R ignoreCase() {

		if (null != this.pointer) {
			this.pointer.setIgnoreCase(true);
		}
		return this.getReturns();
	}

	@SafeVarargs
	@Override
	public final R custom(String sql, V... values) {

		if (null == this.condition) {
			this.condition = new Condition<>();
		}
		Predicate<F> predicate;
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

			predicate = Predicate.of(sql, pvs);
		}
		else {
			predicate = Predicate.of(sql);
		}

		this.addPredicate(predicate);

		return this.getReturns();
	}

	@Override
	public R predicate(Predicate<F> predicate) {

		this.addPredicate(predicate);

		return this.getReturns();
	}

	private void addPredicate(F field, PredicateType type, ParamValue... values) {

		this.addPredicate(Predicate.of(field, type, false, values));
	}

	private void addPredicate(Predicate<F> predicate) {

		this.pointer = predicate;

		if (null == this.condition) {
			this.condition = new Condition<>();
		}

		if (null == this.condition.getPredicate()) {
			this.condition.setPredicate(predicate);
		}
		else {
			this.condition.getPredicate().opposing(this.takeFlushOperator(), predicate);
		}
	}

	private Operator takeFlushOperator() {

		if (null == this.flushOperator) {
			return AND;
		}

		Operator fo = this.flushOperator;
		this.flushOperator = null;

		return fo;
	}

	protected SQL logicDeleteClause(MybatisPersistentEntityImpl<?> entity, boolean alias) {

		if (!entity.getLogicDeleteColumn().isPresent()) {
			return SQL.EMPTY;
		}

		Column col = alias ? Column.base(entity.getLogicDeleteColumn().get())
				: Column.of(entity.getLogicDeleteColumn().get());
		return SQL.of("AND " + col + " = 0");
	}

	@Override
	public String toString() {
		return "ConditionImpl";
	}

}
