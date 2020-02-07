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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * .
 *
 * @author JARVIS SONG
 */
class ParameterMetadataProvider {

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
