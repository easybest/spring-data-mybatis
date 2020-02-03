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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Mybatis specific extension of {@link QueryMethod}.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisQueryMethod extends QueryMethod {

	private static final StoredProcedureAttributeSource storedProcedureAttributeSource = StoredProcedureAttributeSource.INSTANCE;

	private static final Set<Class<?>> NATIVE_ARRAY_TYPES;
	static {

		Set<Class<?>> types = new HashSet<>();
		types.add(byte[].class);
		types.add(Byte[].class);
		types.add(char[].class);
		types.add(Character[].class);

		NATIVE_ARRAY_TYPES = Collections.unmodifiableSet(types);
	}
	private final Method method;

	private @Nullable StoredProcedureAttributes storedProcedureAttributes;

	private final Lazy<MybatisEntityMetadata<?>> entityMetadata;

	private final Lazy<Boolean> isProcedureQuery;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct
	 * query to use for following invocations of the method given.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public MybatisQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
		Assert.notNull(method, "Method must not be null!");
		this.method = method;
		this.isProcedureQuery = Lazy.of(() -> AnnotationUtils.findAnnotation(method, Procedure.class) != null);
		this.entityMetadata = Lazy.of(() -> new DefaultMybatisEntityMetadata<>(this.getDomainClass()));

	}

	Class<?> getReturnType() {
		return this.method.getReturnType();
	}

	@Override
	public MybatisParameters getParameters() {
		return (MybatisParameters) super.getParameters();
	}

	@Override
	public MybatisEntityMetadata<?> getEntityInformation() {
		return this.entityMetadata.get();
	}

	public boolean isProcedureQuery() {
		return this.isProcedureQuery.get();
	}

	@Nullable
	String getAnnotatedQuery() {

		String query = this.getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	String getRequiredAnnotatedQuery() throws IllegalStateException {
		String query = this.getAnnotatedQuery();

		if (query != null) {
			return query;
		}
		throw new IllegalStateException(String.format("No annotated query found for query method %s!", this.getName()));
	}

	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return this.getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
		if (annotation == null) {
			return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	StoredProcedureAttributes getProcedureAttributes() {

		if (this.storedProcedureAttributes == null) {
			this.storedProcedureAttributes = storedProcedureAttributeSource.createFrom(this.method,
					this.getEntityInformation());
		}

		return this.storedProcedureAttributes;
	}

}
