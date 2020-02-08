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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * .
 *
 * @author JARVIS SONG
 */
class ParameterMetadataProvider {

	private final CriteriaBuilder builder;

	private final Iterator<? extends Parameter> parameters;

	private final List<ParameterMetadata<?>> expressions;

	private final @Nullable Iterator<Object> bindableParameterValues;

	private final EscapeCharacter escape;

	ParameterMetadataProvider(CriteriaBuilder builder, ParametersParameterAccessor accessor, EscapeCharacter escape) {
		this(builder, accessor.iterator(), accessor.getParameters(), escape);
	}

	ParameterMetadataProvider(CriteriaBuilder builder, Parameters<?, ?> parameters, EscapeCharacter escape) {
		this(builder, null, parameters, escape);
	}

	private ParameterMetadataProvider(CriteriaBuilder builder, @Nullable Iterator<Object> bindableParameterValues,
			Parameters<?, ?> parameters, EscapeCharacter escape) {

		// Assert.notNull(builder, "CriteriaBuilder must not be null!");
		// Assert.notNull(parameters, "Parameters must not be null!");
		// Assert.notNull(escape, "EscapeCharacter must not be null!");

		this.builder = builder;
		this.parameters = parameters.getBindableParameters().iterator();
		this.expressions = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
		this.escape = escape;
	}

	public List<ParameterMetadata<?>> getExpressions() {
		return this.expressions;
	}

	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<T> next(Part part) {

		Assert.isTrue(this.parameters.hasNext(), () -> String.format("No parameter available for part %s.", part));

		Parameter parameter = this.parameters.next();
		return (ParameterMetadata<T>) this.next(part, parameter.getType(), parameter);
	}

	@SuppressWarnings("unchecked")
	public <T> ParameterMetadata<? extends T> next(Part part, Class<T> type) {

		Parameter parameter = this.parameters.next();
		Class<?> typeToUse = ClassUtils.isAssignable(type, parameter.getType()) ? parameter.getType() : type;
		return (ParameterMetadata<? extends T>) this.next(part, typeToUse, parameter);
	}

	private <T> ParameterMetadata<T> next(Part part, Class<T> type, Parameter parameter) {

		Assert.notNull(type, "Type must not be null!");

		/*
		 * We treat Expression types as Object vales since the real value to be bound as a
		 * parameter is determined at query time.
		 */
		@SuppressWarnings("unchecked")
		Class<T> reifiedType = Expression.class.equals(type) ? (Class<T>) Object.class : type;

		Supplier<String> name = () -> parameter.getName()
				.orElseThrow(() -> new IllegalArgumentException("o_O Parameter needs to be named"));

		ParameterExpression<T> expression = parameter.isExplicitlyNamed() //
				? this.builder.parameter(reifiedType, name.get()) //
				: this.builder.parameter(reifiedType);

		Object value = (this.bindableParameterValues != null) ? this.bindableParameterValues.next()
				: ParameterMetadata.PLACEHOLDER;

		ParameterMetadata<T> metadata = new ParameterMetadata<>(expression, part, value, this.escape);
		this.expressions.add(metadata);

		return metadata;
	}

	EscapeCharacter getEscape() {
		return this.escape;
	}

	static class ParameterMetadata<T> {

		static final Object PLACEHOLDER = new Object();

		private final Part.Type type;

		private final ParameterExpression<T> expression;

		private final EscapeCharacter escape;

		private final boolean ignoreCase;

		ParameterMetadata(ParameterExpression<T> expression, Part part, @Nullable Object value,
				EscapeCharacter escape) {

			this.expression = expression;
			this.type = ((value != null) || !Part.Type.SIMPLE_PROPERTY.equals(part.getType())) ? part.getType()
					: Part.Type.IS_NULL;
			this.ignoreCase = Part.IgnoreCaseType.ALWAYS.equals(part.shouldIgnoreCase());
			this.escape = escape;
		}

		public ParameterExpression<T> getExpression() {
			return this.expression;
		}

		public boolean isIsNullParameter() {
			return Part.Type.IS_NULL.equals(this.type);
		}

		@Nullable
		public Object prepare(Object value) {

			Assert.notNull(value, "Value must not be null!");

			Class<? extends T> expressionType = this.expression.getJavaType();

			if (String.class.equals(expressionType)) {

				switch (this.type) {
				case STARTING_WITH:
					return String.format("%s%%", this.escape.escape(value.toString()));
				case ENDING_WITH:
					return String.format("%%%s", this.escape.escape(value.toString()));
				case CONTAINING:
				case NOT_CONTAINING:
					return String.format("%%%s%%", this.escape.escape(value.toString()));
				default:
					return value;
				}
			}

			return Collection.class.isAssignableFrom(expressionType) //
					? upperIfIgnoreCase(this.ignoreCase, toCollection(value)) //
					: value;
		}

		@Nullable
		private static Collection<?> toCollection(@Nullable Object value) {

			if (value == null) {
				return null;
			}

			if (value instanceof Collection) {

				Collection<?> collection = (Collection<?>) value;
				return collection.isEmpty() ? null : collection;
			}

			if (ObjectUtils.isArray(value)) {

				List<Object> collection = Arrays.asList(ObjectUtils.toObjectArray(value));
				return collection.isEmpty() ? null : collection;
			}

			return Collections.singleton(value);
		}

		@Nullable
		@SuppressWarnings("unchecked")
		private static Collection<?> upperIfIgnoreCase(boolean ignoreCase, @Nullable Collection<?> collection) {

			if (!ignoreCase || CollectionUtils.isEmpty(collection)) {
				return collection;
			}

			return ((Collection<String>) collection).stream() //
					.map(it -> (it != null) ? it.toUpperCase() : null) //
					.collect(Collectors.toList());
		}

	}

}
