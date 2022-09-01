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

import java.util.function.Consumer;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.repository.query.criteria.Condition;
import io.easybest.mybatis.repository.query.criteria.Conditions;
import io.easybest.mybatis.repository.query.criteria.Operator;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.query.parser.Part;

import static io.easybest.mybatis.repository.query.criteria.Operator.AND;
import static io.easybest.mybatis.repository.query.criteria.Operator.OR;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 */
public class ConditionsImpl<T, R, F> implements Conditions<R, F> {

	protected final Class<T> domainClass;

	@SuppressWarnings({ "unchecked" })
	protected R returns = (R) this;

	/**
	 * Conditions tree.
	 */
	protected Condition<F> condition;

	protected Operator flushOperator;

	public ConditionsImpl(Class<T> domainClass) {

		this.domainClass = domainClass;
	}

	public PredicateResult toConditionSQL(EntityManager entityManager, ParamValueCallback callback, boolean tr) {

		return this.condition.toSQL(entityManager, this.domainClass, callback, tr);
	}

	@Override
	public R or() {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.flushOperator = OR;

		return this.returns;
	}

	@Override
	public R and() {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.flushOperator = AND;

		return this.returns;
	}

	@Override
	public R or(Conditions<R, F> conditions) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.condition.or(((ConditionsImpl<T, R, F>) conditions).condition);

		return this.returns;
	}

	@Override
	public R and(Conditions<R, F> conditions) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		this.condition.and(((ConditionsImpl<T, R, F>) conditions).condition);

		return this.returns;
	}

	@Override
	public R or(Consumer<Conditions<R, F>> consumer) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		Conditions<R, F> nested = this.createConditionsInstance();
		consumer.accept(nested);

		return this.or(nested);
	}

	@Override
	public R and(Consumer<Conditions<R, F>> consumer) {

		if (null == this.condition) {
			throw new MappingException("Don't specify operator at the first place.");
		}

		Conditions<R, F> nested = this.createConditionsInstance();
		consumer.accept(nested);

		return this.and(nested);
	}

	protected Conditions<R, F> createConditionsInstance() {
		return new ConditionsImpl<>(this.domainClass);
	}

	@Override
	public R eq(F field, Object value) {

		this.addPredicate(field, Part.Type.SIMPLE_PROPERTY, ParamValue.of(value));

		return this.returns;
	}

	@Override
	public R ne(F field, Object value) {

		this.addPredicate(field, Part.Type.NEGATING_SIMPLE_PROPERTY, ParamValue.of(value));

		return this.returns;
	}

	private void addPredicate(F field, Part.Type type, ParamValue... values) {

		if (null == this.condition) {
			this.condition = new Condition<>();
		}

		Predicate<F> predicate = Predicate.of(field, type, values);
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

	@Override
	public String toString() {
		return "ConditionImpl";
	}

}
