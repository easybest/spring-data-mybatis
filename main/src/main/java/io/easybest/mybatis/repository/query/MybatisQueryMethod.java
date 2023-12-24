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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.repository.BasicQuery;
import io.easybest.mybatis.repository.Modifying;
import io.easybest.mybatis.repository.Query;
import io.easybest.mybatis.repository.ResultMap;
import io.easybest.mybatis.repository.ResultType;
import io.easybest.mybatis.repository.support.ResidentStatementName;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisQueryMethod extends QueryMethod {

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

	private final Class<?> returnType;

	private final Lazy<Modifying> modifying;

	private final Lazy<Boolean> isCollectionQuery;

	private final Lazy<Boolean> isProcedureQuery;

	private final Lazy<Boolean> isBasicQuery;

	private final Lazy<String> resultMap;

	private final Lazy<Class<?>> resultType;

	private final Lazy<MybatisEntityMetadata<?>> entityMetadata;

	private final String namespace;

	private final String statementName;

	private final String countStatementName;

	public MybatisQueryMethod(EntityManager entityManager, Method method, RepositoryMetadata metadata,
			ProjectionFactory factory) {

		super(method, metadata, factory);

		Assert.notNull(method, "Method must not be null!");

		this.method = method;
		this.returnType = potentiallyUnwrapReturnTypeFor(metadata, method);
		this.modifying = Lazy.of(() -> AnnotatedElementUtils.findMergedAnnotation(method, Modifying.class));
		this.isCollectionQuery = Lazy
				.of(() -> super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(this.returnType));
		this.isProcedureQuery = Lazy.of(() -> null != AnnotationUtils.findAnnotation(method, Procedure.class));
		this.isBasicQuery = Lazy.of(() -> null != AnnotationUtils.findAnnotation(method, BasicQuery.class));
		this.entityMetadata = Lazy.of(() -> new DefaultMybatisEntityMetadata<>(
				entityManager.getRequiredPersistentEntity(this.getDomainClass())));

		this.resultMap = Lazy.of(() -> {
			ResultMap annotation = AnnotationUtils.findAnnotation(method, ResultMap.class);
			return null != annotation && StringUtils.hasText(annotation.value()) ? annotation.value() : null;
		});
		this.resultType = Lazy.of(() -> {
			ResultType annotation = AnnotationUtils.findAnnotation(method, ResultType.class);
			return null == annotation || annotation.value() == Void.class ? null : annotation.value();
		});

		Assert.isTrue(!(this.isModifyingQuery() && this.getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s!", Parameters.TYPES));
		this.assertParameterNamesInAnnotatedQuery();

		this.namespace = this.getAnnotationStringValue("namespace", this.getDomainClass().getName());
		this.statementName = this.getAnnotationStringValue("statement", () -> {
			long count = Arrays.stream(metadata.getRepositoryInterface().getMethods())
					.filter(m -> m.getName().equals(method.getName())).count();
			if (count == 1) {
				return method.getName();
			}
			return method.getName() + UUID.randomUUID().toString().replace("-", "");
		});
		this.countStatementName = this.getAnnotationStringValue("countStatement",
				ResidentStatementName.COUNT_PREFIX + this.statementName);
	}

	private static Class<?> potentiallyUnwrapReturnTypeFor(RepositoryMetadata metadata, Method method) {

		TypeInformation<?> returnType = metadata.getReturnType(method);

		while (QueryExecutionConverters.supports(returnType.getType())
				|| QueryExecutionConverters.supportsUnwrapping(returnType.getType())) {
			returnType = returnType.getRequiredComponentType();
		}

		return returnType.getType();
	}

	@Override
	public String getNamedQueryName() {

		if (this.hasAnnotatedQueryName()) {
			return this.getAnnotationValue("name", String.class);
		}

		return super.getNamedQueryName();
	}

	@Override
	public Class<?> getDomainClass() {
		return super.getDomainClass();
	}

	// @Override
	// protected MybatisParameters createParameters(Method method) {
	//
	// return new MybatisParameters(method);
	// }

	@Override
	protected Parameters<?, ?> createParameters(ParametersSource parametersSource) {
		return new MybatisParameters(parametersSource);
	}

	@Override
	public MybatisParameters getParameters() {

		return (MybatisParameters) super.getParameters();
	}

	@Override
	public boolean isModifyingQuery() {

		return null != this.modifying.getNullable();
	}

	public SqlCommandType getModifyingType() {

		if (!this.isModifyingQuery()) {
			return null;
		}
		return this.modifying.get().value();
	}

	Class<?> getReturnType() {
		return this.returnType;
	}

	public String getActualResultType() {

		if (this.getResultType().isPresent()) {

			return this.getResultType().get().getName();
		}

		Class<?> type = this.getReturnType();
		if (type == Map.class) {
			return "map";
		}
		if (this.isCollectionQuery() || this.isSliceQuery() || this.isPageQuery() || this.isStreamQuery()) {
			type = this.getReturnedObjectType();
		}

		if (type.isInterface()) {
			type = this.getDomainClass();
		}

		return type.getName();
	}

	boolean hasQueryAnnotation() {

		Query annotation = AnnotationUtils.findAnnotation(this.method, Query.class);
		return null != annotation;
	}

	@Nullable
	String getAnnotatedQuery() {

		String query = this.getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return this.getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	public String getAnnotationStringValue(String attribute, String defaultValue) {

		String value = this.getMergedOrDefaultAnnotationValue(attribute, Query.class, String.class);

		if (StringUtils.hasText(value)) {
			return value;
		}

		return defaultValue;
	}

	private String getAnnotationStringValue(String attribute, Supplier<String> defaultValue) {

		String value = this.getMergedOrDefaultAnnotationValue(attribute, Query.class, String.class);

		if (StringUtils.hasText(value)) {
			return value;
		}

		return defaultValue.get();
	}

	boolean hasAnnotatedQueryName() {
		return StringUtils.hasText(this.getAnnotationValue("name", String.class));
	}

	String getRequiredAnnotatedQuery() throws IllegalStateException {

		String query = this.getAnnotatedQuery();

		if (query != null) {
			return query;
		}

		throw new IllegalStateException(String.format("No annotated query found for query method %s!", this.getName()));
	}

	@Nullable
	String getCountQuery() {

		String countQuery = this.getAnnotationValue("countQuery", String.class);
		return StringUtils.hasText(countQuery) ? countQuery : null;
	}

	@Nullable
	String getCountQueryProjection() {

		String countProjection = this.getAnnotationValue("countProjection", String.class);
		return StringUtils.hasText(countProjection) ? countProjection : null;
	}

	String getNamedCountQueryName() {

		String annotatedName = this.getAnnotationValue("countName", String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : this.getNamedQueryName() + ".count";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
		if (annotation == null) {
			return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	private void assertParameterNamesInAnnotatedQuery() {

		String annotatedQuery = this.getAnnotatedQuery();

		if (!DeclaredQuery.of(annotatedQuery).hasNamedParameter()) {
			return;
		}

		for (Parameter parameter : this.getParameters()) {

			if (!parameter.isNamedParameter()) {
				continue;
			}

			if (!StringUtils.hasText(annotatedQuery)
					|| !annotatedQuery.contains(String.format(":%s", parameter.getName().get()))
							&& !annotatedQuery.contains(String.format("#%s", parameter.getName().get()))) {
				throw new IllegalStateException(String.format(
						"Using named parameters for method %s but parameter '%s' not found in annotated query '%s'!",
						this.method, parameter.getName().get(), annotatedQuery));
			}
		}
	}

	@Override
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	public boolean isProcedureQuery() {
		return this.isProcedureQuery.get();
	}

	public boolean isBasicQuery() {
		return this.isBasicQuery.get();
	}

	public Optional<String> getResultMap() {
		return this.resultMap.getOptional();
	}

	public Optional<Class<?>> getResultType() {
		return this.resultType.getOptional();
	}

	@Override
	public MybatisEntityMetadata<?> getEntityInformation() {
		return this.entityMetadata.get();
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getStatementName() {
		return this.statementName;
	}

	public String getCountStatementName() {
		return this.countStatementName;
	}

	public String getStatementId() {
		return this.getNamespace() + '.' + this.getStatementName();
	}

}
