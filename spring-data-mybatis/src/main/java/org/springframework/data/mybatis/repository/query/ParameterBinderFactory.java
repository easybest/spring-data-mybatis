/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.repository.query;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public final class ParameterBinderFactory {

	private ParameterBinderFactory() {
	}

	static ParameterBinder createBinder(MybatisParameters parameters) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");

		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.basic(parameters);
		List<StringQuery.ParameterBinding> bindings = getBindings(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, setterFactory));
	}

	static ParameterBinder createCriteriaBinder(MybatisParameters parameters,
			List<ParameterMetadataProvider.ParameterMetadata<?>> metadata) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");
		Assert.notNull(metadata, "Parameter metadata must not be null!");

		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.forCriteriaQuery(parameters, metadata);
		List<StringQuery.ParameterBinding> bindings = getBindings(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, setterFactory));
	}

	static ParameterBinder createQueryAwareBinder(MybatisParameters parameters, DeclaredQuery query,
			SpelExpressionParser parser, QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");
		Assert.notNull(query, "StringQuery must not be null!");
		Assert.notNull(parser, "SpelExpressionParser must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		List<StringQuery.ParameterBinding> bindings = query.getParameterBindings();
		QueryParameterSetterFactory expressionSetterFactory = QueryParameterSetterFactory.parsing(parser,
				evaluationContextProvider, parameters);
		QueryParameterSetterFactory basicSetterFactory = QueryParameterSetterFactory.basic(parameters);

		return new ParameterBinder(parameters,
				createSetters(bindings, query, expressionSetterFactory, basicSetterFactory), !query.usesPaging());
	}

	private static List<StringQuery.ParameterBinding> getBindings(MybatisParameters parameters) {

		List<StringQuery.ParameterBinding> result = new ArrayList<>();
		int bindableParameterIndex = 0;

		for (MybatisParameters.MybatisParameter parameter : parameters) {

			if (parameter.isBindable()) {
				result.add(new StringQuery.ParameterBinding(++bindableParameterIndex));
			}
		}

		return result;
	}

	private static Iterable<QueryParameterSetter> createSetters(List<StringQuery.ParameterBinding> parameterBindings,
			QueryParameterSetterFactory... factories) {
		return createSetters(parameterBindings, EmptyDeclaredQuery.EMPTY_QUERY, factories);
	}

	private static Iterable<QueryParameterSetter> createSetters(List<StringQuery.ParameterBinding> parameterBindings,
			DeclaredQuery declaredQuery, QueryParameterSetterFactory... strategies) {

		List<QueryParameterSetter> setters = new ArrayList<>(parameterBindings.size());
		for (StringQuery.ParameterBinding parameterBinding : parameterBindings) {
			setters.add(createQueryParameterSetter(parameterBinding, strategies, declaredQuery));
		}

		return setters;
	}

	private static QueryParameterSetter createQueryParameterSetter(StringQuery.ParameterBinding binding,
			QueryParameterSetterFactory[] strategies, DeclaredQuery declaredQuery) {

		for (QueryParameterSetterFactory strategy : strategies) {

			QueryParameterSetter setter = strategy.create(binding, declaredQuery);

			if (setter != null) {
				return setter;
			}
		}

		return QueryParameterSetter.NOOP;
	}

}
