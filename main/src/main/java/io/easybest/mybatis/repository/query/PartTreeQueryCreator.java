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

package io.easybest.mybatis.repository.query;

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import io.easybest.mybatis.mapping.precompile.ParameterExpression;
import io.easybest.mybatis.repository.query.ParameterMetadataProvider.ParameterMetadata;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.PredicateType;
import io.easybest.mybatis.repository.query.criteria.impl.ConditionsImpl;
import io.easybest.mybatis.repository.query.criteria.impl.CriteriaQueryImpl;

import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.ALWAYS;
import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.WHEN_POSSIBLE;

/**
 * .
 *
 * @author Jarvis Song
 * @param <C> query type
 */
public class PartTreeQueryCreator<C extends ConditionsImpl<?, ?, String, ParamValue>>
		extends AbstractQueryCreator<C, Predicate<String>> {

	private final ParameterMetadataProvider provider;

	private final C criteriaQuery;

	public PartTreeQueryCreator(PartTree tree, ParameterMetadataProvider provider, C criteriaQuery) {

		super(tree);

		this.provider = provider;
		this.criteriaQuery = criteriaQuery;
	}

	@Override
	protected Predicate<String> create(Part part, Iterator<Object> iterator) {

		PropertyPath pp = part.getProperty();
		Part.Type type = part.getType();
		boolean ignoreCase = ALWAYS.equals(part.shouldIgnoreCase());
		ParamValue[] values = new ParamValue[type.getNumberOfArguments()];
		if (type.getNumberOfArguments() > 0) {
			for (int i = 0; i < values.length; i++) {
				ParameterMetadata<Object> metadata = this.provider.next(part);
				ParameterExpression<Object> expression = metadata.getExpression();
				values[i] = ParamValue.of(expression.getName(), null);
				if (WHEN_POSSIBLE.equals(part.shouldIgnoreCase())) {
					if (String.class.equals(expression.getJavaType())) {
						ignoreCase = true;
					}
				}
			}
		}

		return Predicate.of(pp.toDotPath(), PredicateType.valueOf(type.name()), ignoreCase, values);
	}

	@Override
	protected Predicate<String> and(Part part, Predicate<String> base, Iterator<Object> iterator) {

		return base.and(this.create(part, iterator));
	}

	@Override
	protected Predicate<String> or(Predicate<String> base, Predicate<String> criteria) {

		return base.or(criteria);
	}

	@Override
	protected C complete(Predicate<String> criteria, Sort sort) {

		this.criteriaQuery.predicate(criteria);

		if (this.criteriaQuery instanceof CriteriaQueryImpl) {
			((CriteriaQueryImpl<?, ?, ?, ?>) this.criteriaQuery).orderBy(sort);
		}

		return this.criteriaQuery;
	}

}
