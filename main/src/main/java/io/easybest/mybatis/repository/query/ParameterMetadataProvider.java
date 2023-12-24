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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.ToString;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.precompile.ParameterExpression;
import io.easybest.mybatis.repository.support.ResidentParameterName;

/**
 * .
 *
 * @author Jarvis Song
 */
class ParameterMetadataProvider {

	private final EntityManager entityManager;

	private final Iterator<? extends Parameter> parameters;

	private final List<ParameterMetadata<?>> expressions;

	private final @Nullable Iterator<Object> bindableParameterValues;

	public ParameterMetadataProvider(EntityManager entityManager, ParametersParameterAccessor accessor) {
		this(entityManager, accessor.iterator(), accessor.getParameters());
	}

	public ParameterMetadataProvider(EntityManager entityManager, Parameters<?, ?> parameters) {
		this(entityManager, null, parameters);
	}

	private ParameterMetadataProvider(EntityManager entityManager, @Nullable Iterator<Object> bindableParameterValues,
			Parameters<?, ?> parameters) {

		Assert.notNull(entityManager, "EntityManager must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		this.entityManager = entityManager;
		this.parameters = parameters.getBindableParameters().iterator();
		this.expressions = new ArrayList<>();
		this.bindableParameterValues = bindableParameterValues;
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
		@SuppressWarnings("unchecked")
		Class<T> reifiedType = Expression.class.equals(type) ? (Class<T>) Object.class : type;

		// Supplier<String> name = () -> parameter.getName()
		// .orElseThrow(() -> new IllegalArgumentException("o_O Parameter needs to be
		// named"));
		// ParameterExpression<T> expression = parameter.isExplicitlyNamed() //
		// ? new ParameterExpression<>(name.get(), reifiedType)//
		// : new ParameterExpression<>(ResidentParameterName.POSITION_PREFIX +
		// parameter.getIndex(), reifiedType);

		ParameterExpression<T> expression = new ParameterExpression<>(
				parameter.getName().orElse(ResidentParameterName.POSITION_PREFIX + parameter.getIndex()), reifiedType);

		Object value = this.bindableParameterValues == null ? ParameterMetadata.PLACEHOLDER
				: this.bindableParameterValues.next();

		ParameterMetadata<T> metadata = new ParameterMetadata<>(expression, part, value);
		this.expressions.add(metadata);

		return metadata;
	}

	@ToString
	static class ParameterMetadata<T> {

		static final Object PLACEHOLDER = new Object();

		private final Part.Type type;

		private final ParameterExpression<T> expression;

		private final boolean ignoreCase;

		public ParameterMetadata(ParameterExpression<T> expression, Part part, @Nullable Object value) {

			this.expression = expression;
			this.type = null == value && Part.Type.SIMPLE_PROPERTY.equals(part.getType()) ? Part.Type.IS_NULL
					: part.getType();
			this.ignoreCase = Part.IgnoreCaseType.ALWAYS.equals(part.shouldIgnoreCase());
		}

		public ParameterExpression<T> getExpression() {
			return this.expression;
		}

		public boolean isIsNullParameter() {
			return Part.Type.IS_NULL.equals(this.type);
		}

	}

}
