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

import io.easybest.mybatis.mapping.EntityManager;
import lombok.Data;

import static io.easybest.mybatis.repository.query.criteria.Operator.AND;
import static io.easybest.mybatis.repository.query.criteria.Operator.OR;

/**
 * .
 *
 * @author Jarvis Song
 * @param <F> field type
 */
@Data
public class Condition<F> implements Serializable {

	private Operator operator;

	private Condition<F> opposite;

	private Condition<F> pointer = this;

	private Predicate<F> predicate;

	private int idx = 0;

	public Condition<F> and(Condition<F> opposite) {

		return this.opposing(AND, opposite);
	}

	public Condition<F> or(Condition<F> opposite) {

		return this.opposing(OR, opposite);
	}

	public Condition<F> opposing(Operator operator, Condition<F> opposite) {

		this.pointer.opposite = opposite;
		this.pointer.opposite.operator = operator;
		this.pointer.opposite.idx = this.pointer.idx + 1;
		this.pointer = opposite;

		return this;
	}

	public PredicateResult toSQL(EntityManager entityManager, Class<?> domainClass, ParamValueCallback callback,
			boolean tr) {

		Condition<F> opposite = this.opposite;

		PredicateResult pr = this.predicate.toSQL(this.idx, entityManager, domainClass, callback, tr);

		while (null != opposite) {

			pr.append(opposite.operator, opposite.toSQL(entityManager, domainClass, callback, tr), true);

			opposite = opposite.opposite;
		}
		return pr;
	}

	@Override
	public String toString() {
		return "Condition{" + "idx=" + this.idx + '}';
	}

}
