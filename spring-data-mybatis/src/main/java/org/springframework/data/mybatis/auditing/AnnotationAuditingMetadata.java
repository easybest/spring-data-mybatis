/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.auditing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.data.util.ReflectionUtils.DescribedFieldFilter;
import org.springframework.util.Assert;

import lombok.NonNull;

/**
 * Inspects the given {@link Class} for fields annotated by {@link CreatedBy},
 * {@link CreatedDate}, {@link LastModifiedBy} , and {@link LastModifiedDate}. Only one
 * field per annotation is stored.
 *
 * @author Ranie Jade Ramiso
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
final class AnnotationAuditingMetadata {

	private static final AnnotationsFieldFilter CREATED_BY_FILTER = new AnnotationsFieldFilter(
			CreatedBy.class, org.springframework.data.mybatis.annotation.CreatedBy.class);

	private static final AnnotationsFieldFilter CREATED_DATE_FILTER = new AnnotationsFieldFilter(
			CreatedDate.class,
			org.springframework.data.mybatis.annotation.CreatedDate.class);

	private static final AnnotationsFieldFilter LAST_MODIFIED_BY_FILTER = new AnnotationsFieldFilter(
			LastModifiedBy.class,
			org.springframework.data.mybatis.annotation.LastModifiedBy.class);

	private static final AnnotationsFieldFilter LAST_MODIFIED_DATE_FILTER = new AnnotationsFieldFilter(
			LastModifiedDate.class,
			org.springframework.data.mybatis.annotation.LastModifiedDate.class);

	private static final Map<Class<?>, AnnotationAuditingMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

	public static final boolean IS_JDK_8 = org.springframework.util.ClassUtils.isPresent(
			"java.time.Clock", AnnotationAuditingMetadata.class.getClassLoader());

	static final List<String> SUPPORTED_DATE_TYPES;

	static {

		List<String> types = new ArrayList<>(5);
		types.add("org.joda.time.DateTime");
		types.add("org.joda.time.LocalDateTime");
		types.add(Date.class.getName());
		types.add(Long.class.getName());
		types.add(long.class.getName());

		SUPPORTED_DATE_TYPES = Collections.unmodifiableList(types);
	}

	private final Optional<Field> createdByField;

	private final Optional<Field> createdDateField;

	private final Optional<Field> lastModifiedByField;

	private final Optional<Field> lastModifiedDateField;

	/**
	 * Creates a new {@link AnnotationAuditingMetadata} instance for the given type.
	 * @param type must not be {@literal null}.
	 */
	private AnnotationAuditingMetadata(Class<?> type) {

		Assert.notNull(type, "Given type must not be null!");

		this.createdByField = Optional
				.ofNullable(ReflectionUtils.findField(type, CREATED_BY_FILTER));
		this.createdDateField = Optional
				.ofNullable(ReflectionUtils.findField(type, CREATED_DATE_FILTER));
		this.lastModifiedByField = Optional
				.ofNullable(ReflectionUtils.findField(type, LAST_MODIFIED_BY_FILTER));
		this.lastModifiedDateField = Optional
				.ofNullable(ReflectionUtils.findField(type, LAST_MODIFIED_DATE_FILTER));

		assertValidDateFieldType(createdDateField);
		assertValidDateFieldType(lastModifiedDateField);
	}

	/**
	 * Checks whether the given field has a type that is a supported date type.
	 * @param field
	 */
	private void assertValidDateFieldType(Optional<Field> field) {

		field.ifPresent(it -> {

			if (SUPPORTED_DATE_TYPES.contains(it.getType().getName())) {
				return;
			}

			Class<?> type = it.getType();

			if (Jsr310Converters.supports(type)
					|| ThreeTenBackPortConverters.supports(type)) {
				return;
			}

			throw new IllegalStateException(String.format(
					"Found created/modified date field with type %s but only %s as well as java.time types are supported!",
					type, SUPPORTED_DATE_TYPES));
		});
	}

	/**
	 * Return a {@link AnnotationAuditingMetadata} for the given {@link Class}.
	 * @param type the type to inspect, must not be {@literal null}.
	 */
	public static AnnotationAuditingMetadata getMetadata(Class<?> type) {
		return METADATA_CACHE.computeIfAbsent(type, AnnotationAuditingMetadata::new);
	}

	/**
	 * Returns whether the {@link Class} represented in this instance is auditable or not.
	 */
	public boolean isAuditable() {
		return Optionals.isAnyPresent(createdByField, createdDateField,
				lastModifiedByField, lastModifiedDateField);
	}

	/**
	 * Return the field annotated by {@link CreatedBy}.
	 */
	public Optional<Field> getCreatedByField() {
		return createdByField;
	}

	/**
	 * Return the field annotated by {@link CreatedDate}.
	 */
	public Optional<Field> getCreatedDateField() {
		return createdDateField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedBy}.
	 */
	public Optional<Field> getLastModifiedByField() {
		return lastModifiedByField;
	}

	/**
	 * Return the field annotated by {@link LastModifiedDate}.
	 */
	public Optional<Field> getLastModifiedDateField() {
		return lastModifiedDateField;
	}

	public static class AnnotationsFieldFilter implements DescribedFieldFilter {

		private final @NonNull Class<? extends Annotation>[] annotationTypes;

		public AnnotationsFieldFilter(Class<? extends Annotation>... annotationTypes) {
			this.annotationTypes = annotationTypes;
		}

		public boolean matches(Field field) {

			return Stream.of(annotationTypes)
					.anyMatch(a -> null != AnnotationUtils.getAnnotation(field, a));
		}

		public String getDescription() {
			return String.format("Annotation filter for %s",
					Arrays.toString(annotationTypes));
		}

	}

}
