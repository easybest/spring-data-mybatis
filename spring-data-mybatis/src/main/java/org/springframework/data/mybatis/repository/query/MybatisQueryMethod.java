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
import java.util.UUID;

import lombok.Getter;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.mybatis.repository.ResultMap;
import org.springframework.data.mybatis.repository.ResultType;
import org.springframework.data.mybatis.repository.SelectColumns;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
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

	private final Lazy<Boolean> isCollectionQuery;

	private final Lazy<Boolean> isProcedureQuery;

	private final Lazy<Modifying> modifying;

	@Getter
	private final String namespace;

	@Getter
	private final String statementName;

	@Getter
	private final String countStatementName;

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

		this.isCollectionQuery = Lazy
				.of(() -> super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(method.getReturnType()));
		this.isProcedureQuery = Lazy.of(() -> AnnotationUtils.findAnnotation(method, Procedure.class) != null);
		this.entityMetadata = Lazy.of(() -> new DefaultMybatisEntityMetadata<>(this.getDomainClass()));
		this.modifying = Lazy.of(() -> AnnotatedElementUtils.findMergedAnnotation(method, Modifying.class));

		Assert.isTrue(!(this.isModifyingQuery() && this.getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s!", Parameters.TYPES));
		this.assertParameterNamesInAnnotatedQuery();

		this.namespace = this.getAnnotationValue("namespace", metadata.getRepositoryInterface().getName());
		this.statementName = this.getAnnotationValue("statement",
				(method.getName() + UUID.randomUUID().toString().replace("-", "")));
		this.countStatementName = this.getAnnotationValue("countStatement",
				ResidentStatementName.COUNT_PREFIX + this.statementName);
	}

	public String getStatementId() {
		return this.getNamespace() + '.' + this.getStatementName();
	}

	public String getCountStatementId() {
		return this.getNamespace() + '.' + this.getCountStatementName();
	}

	@Override
	protected MybatisParameters createParameters(Method method) {
		return new MybatisParameters(method);
	}

	@Override
	public MybatisParameters getParameters() {
		return (MybatisParameters) super.getParameters();
	}

	@Override
	public MybatisEntityMetadata<?> getEntityInformation() {
		return this.entityMetadata.get();
	}

	@Override
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	@Override
	public boolean isModifyingQuery() {
		return null != this.modifying.getNullable();
	}

	@Override
	public String getNamedQueryName() {
		return this.getAnnotationValue("name", super.getNamedQueryName());
	}

	String getNamedCountQueryName() {
		return this.getAnnotationValue("countName", this.getNamedQueryName() + ".count");
	}

	@Nullable
	String getAnnotatedQuery() {
		String query = this.getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	@Nullable
	String getAnnotatedCountQuery() {
		String query = this.getAnnotationValue("countQuery", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	String getRequiredAnnotatedQuery() throws IllegalStateException {
		String query = this.getAnnotatedQuery();

		if (query != null) {
			return query;
		}
		throw new IllegalStateException(String.format("No annotated query found for query method %s!", this.getName()));
	}

	String getResultMap() {
		ResultMap resultMap = AnnotationUtils.findAnnotation(this.method, ResultMap.class);
		return (null != resultMap && StringUtils.hasText(resultMap.value())) ? resultMap.value() : null;
	}

	public Class<?> getResultType() {
		ResultType resultMap = AnnotationUtils.findAnnotation(this.method, ResultType.class);
		if (null == resultMap || resultMap.value() == Void.class) {
			return null;
		}
		return resultMap.value();
	}

	public Modifying.TYPE getModifyingType() {
		if (this.isModifyingQuery()) {
			return this.modifying.get().value();
		}
		return null;
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

			if (StringUtils.isEmpty(annotatedQuery)
					|| !annotatedQuery.contains(String.format(":%s", parameter.getName().get()))
							&& !annotatedQuery.contains(String.format("#%s", parameter.getName().get()))) {
				throw new IllegalStateException(String.format(
						"Using named parameters for method %s but parameter '%s' not found in annotated query '%s'!",
						this.method, parameter.getName(), annotatedQuery));
			}
		}
	}

	////////

	Class<?> getReturnType() {
		return this.method.getReturnType();
	}

	String getActualResultType() {
		Class<?> type = this.getReturnType();
		if (this.isCollectionQuery()) {
			type = this.getReturnedObjectType();
		}

		if (type.isInterface()) {
			type = this.getDomainClass();
		}

		return type.getName();
	}

	public boolean isProcedureQuery() {
		return this.isProcedureQuery.get();
	}

	public String getSelectColumns() {
		SelectColumns columns = AnnotationUtils.findAnnotation(this.method, SelectColumns.class);
		return (null != columns && StringUtils.hasText(columns.value())) ? columns.value() : null;
	}

	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return this.getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	public String getAnnotationValue(String attribute, String defaultValue) {

		String v = this.getMergedOrDefaultAnnotationValue(attribute, Query.class, String.class);
		if (StringUtils.isEmpty(v)) {
			return defaultValue;
		}
		return v;
	}

	private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
		if (null == annotation) {
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
