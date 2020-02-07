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

import java.util.List;
import java.util.function.Function;

import javax.persistence.TemporalType;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 */
abstract class QueryParameterSetterFactory {

	@Nullable
	abstract QueryParameterSetter create(StringQuery.ParameterBinding binding, DeclaredQuery declaredQuery);

	static QueryParameterSetterFactory basic(MybatisParameters parameters) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");

		return new BasicQueryParameterSetterFactory(parameters);
	}

	static QueryParameterSetterFactory forCriteriaQuery(MybatisParameters parameters,
			List<ParameterMetadataProvider.ParameterMetadata<?>> metadata) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");
		Assert.notNull(metadata, "ParameterMetadata must not be null!");

		return new CriteriaQueryParameterSetterFactory(parameters, metadata);
	}

	static QueryParameterSetterFactory parsing(SpelExpressionParser parser,
			QueryMethodEvaluationContextProvider evaluationContextProvider, Parameters<?, ?> parameters) {

		Assert.notNull(parser, "SpelExpressionParser must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		return new ExpressionBasedQueryParameterSetterFactory(parser, evaluationContextProvider, parameters);
	}

	private static QueryParameterSetter createSetter(
			Function<MybatisParametersParameterAccessor, Object> valueExtractor, StringQuery.ParameterBinding binding,
			@Nullable MybatisParameters.MybatisParameter parameter) {

		TemporalType temporalType = ((parameter != null) && parameter.isTemporalParameter()) //
				? parameter.getRequiredTemporalType() //
				: null;

		return new QueryParameterSetter.NamedOrIndexedQueryParameterSetter(valueExtractor.andThen(binding::prepare),
				ParameterImpl.of(parameter, binding), temporalType);
	}

	private static class ExpressionBasedQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final SpelExpressionParser parser;

		private final QueryMethodEvaluationContextProvider evaluationContextProvider;

		private final Parameters<?, ?> parameters;

		ExpressionBasedQueryParameterSetterFactory(SpelExpressionParser parser,
				QueryMethodEvaluationContextProvider evaluationContextProvider, Parameters<?, ?> parameters) {

			Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
			Assert.notNull(parser, "SpelExpressionParser must not be null!");
			Assert.notNull(parameters, "Parameters must not be null!");

			this.evaluationContextProvider = evaluationContextProvider;
			this.parser = parser;
			this.parameters = parameters;
		}

		@Nullable
		@Override
		public QueryParameterSetter create(StringQuery.ParameterBinding binding, DeclaredQuery declaredQuery) {

			if (!binding.isExpression()) {
				return null;
			}

			Expression expression = this.parser.parseExpression(binding.getExpression());

			return createSetter(values -> this.evaluateExpression(expression, values), binding, null);
		}

		@Nullable
		private Object evaluateExpression(Expression expression, MybatisParametersParameterAccessor accessor) {

			EvaluationContext context = this.evaluationContextProvider.getEvaluationContext(this.parameters,
					accessor.getValues());

			return expression.getValue(context, Object.class);
		}

	}

	private static class BasicQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final MybatisParameters parameters;

		BasicQueryParameterSetterFactory(MybatisParameters parameters) {

			Assert.notNull(parameters, "MybatisParameters must not be null!");

			this.parameters = parameters;
		}

		@Override
		public QueryParameterSetter create(StringQuery.ParameterBinding binding, DeclaredQuery declaredQuery) {

			Assert.notNull(binding, "Binding must not be null.");

			MybatisParameters.MybatisParameter parameter;

			if (declaredQuery.hasNamedParameter()) {
				parameter = this.findParameterForBinding(binding);
			}
			else {

				int parameterIndex = binding.getRequiredPosition() - 1;
				MybatisParameters bindableParameters = this.parameters.getBindableParameters();

				Assert.isTrue(parameterIndex < bindableParameters.getNumberOfParameters(),
						() -> String.format(
								"At least %s parameter(s) provided but only %s parameter(s) present in query.", //
								binding.getRequiredPosition(), //
								bindableParameters.getNumberOfParameters() //
						));

				parameter = bindableParameters.getParameter(binding.getRequiredPosition() - 1);
			}

			return (parameter != null) ? createSetter(values -> this.getValue(values, parameter), binding, parameter)
					: QueryParameterSetter.NOOP;
		}

		@Nullable
		private MybatisParameters.MybatisParameter findParameterForBinding(StringQuery.ParameterBinding binding) {

			MybatisParameters bindableParameters = this.parameters.getBindableParameters();

			for (MybatisParameters.MybatisParameter bindableParameter : bindableParameters) {
				if (binding.getRequiredName().equals(getName(bindableParameter))) {
					return bindableParameter;
				}
			}

			return null;
		}

		private Object getValue(MybatisParametersParameterAccessor accessor, Parameter parameter) {
			return accessor.getValue(parameter);
		}

		private static String getName(MybatisParameters.MybatisParameter p) {
			return p.getName()
					.orElseThrow(() -> new IllegalStateException(ParameterBinder.PARAMETER_NEEDS_TO_BE_NAMED));
		}

	}

	private static class CriteriaQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final MybatisParameters parameters;

		private final List<ParameterMetadataProvider.ParameterMetadata<?>> expressions;

		CriteriaQueryParameterSetterFactory(MybatisParameters parameters,
				List<ParameterMetadataProvider.ParameterMetadata<?>> metadata) {

			Assert.notNull(parameters, "MybatisParameters must not be null!");
			Assert.notNull(metadata, "Expressions must not be null!");

			this.parameters = parameters;
			this.expressions = metadata;
		}

		@Override
		public QueryParameterSetter create(StringQuery.ParameterBinding binding, DeclaredQuery declaredQuery) {

			int parameterIndex = binding.getRequiredPosition() - 1;

			Assert.isTrue(parameterIndex < this.expressions.size(),
					() -> String.format("At least %s parameter(s) provided but only %s parameter(s) present in query.",
							binding.getRequiredPosition(), this.expressions.size()));

			ParameterMetadataProvider.ParameterMetadata<?> metadata = this.expressions.get(parameterIndex);

			if (metadata.isIsNullParameter()) {
				return QueryParameterSetter.NOOP;
			}

			MybatisParameters.MybatisParameter parameter = this.parameters.getBindableParameter(parameterIndex);
			TemporalType temporalType = parameter.isTemporalParameter() ? parameter.getRequiredTemporalType() : null;

			return new QueryParameterSetter.NamedOrIndexedQueryParameterSetter(
					values -> this.getAndPrepare(parameter, metadata, values), metadata.getExpression(), temporalType);
		}

		@Nullable
		private Object getAndPrepare(MybatisParameters.MybatisParameter parameter,
				ParameterMetadataProvider.ParameterMetadata<?> metadata, MybatisParametersParameterAccessor accessor) {
			return metadata.prepare(accessor.getValue(parameter));
		}

	}

	private static final class ParameterImpl<T> implements javax.persistence.Parameter<T> {

		private final Class<T> parameterType;

		private final @Nullable String name;

		private final @Nullable Integer position;

		static javax.persistence.Parameter<?> of(@Nullable MybatisParameters.MybatisParameter parameter,
				StringQuery.ParameterBinding binding) {

			Class<?> type = (parameter != null) ? parameter.getType() : Object.class;

			return new ParameterImpl<>(type, getName(parameter, binding), binding.getPosition());
		}

		private ParameterImpl(Class<T> parameterType, @Nullable String name, @Nullable Integer position) {

			this.name = name;
			this.position = position;
			this.parameterType = parameterType;
		}

		@Nullable
		@Override
		public String getName() {
			return this.name;
		}

		@Nullable
		@Override
		public Integer getPosition() {
			return this.position;
		}

		@Override
		public Class<T> getParameterType() {
			return this.parameterType;
		}

		@Nullable
		private static String getName(@Nullable MybatisParameters.MybatisParameter parameter,
				StringQuery.ParameterBinding binding) {

			if (parameter == null) {
				return binding.getName();
			}

			return parameter.isNamedParameter() //
					? parameter.getName()
							.orElseThrow(() -> new IllegalArgumentException("o_O parameter needs to have a name!")) //
					: null;
		}

	}

}
