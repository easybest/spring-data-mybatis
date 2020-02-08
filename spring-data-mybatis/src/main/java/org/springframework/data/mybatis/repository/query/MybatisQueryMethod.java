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

import lombok.Getter;
import org.apache.ibatis.mapping.SqlCommandType;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.mybatis.repository.ResultMap;
import org.springframework.data.mybatis.repository.ResultType;
import org.springframework.data.mybatis.repository.SelectColumns;
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

		String namespace = this.getAnnotationValue("namespace", String.class);
		String statementName = this.getAnnotationValue("statement", String.class);
		this.namespace = StringUtils.hasText(namespace) ? namespace : metadata.getRepositoryInterface().getName();
		this.statementName = StringUtils.hasLength(statementName) ? statementName : (method.getName());
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

	Class<?> getReturnType() {
		return this.method.getReturnType();
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

	public boolean isProcedureQuery() {
		return this.isProcedureQuery.get();
	}

	@Override
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	@Override
	public boolean isModifyingQuery() {
		return null != this.modifying.getNullable();
	}

	public String getResultMap() {
		ResultMap resultMap = AnnotationUtils.findAnnotation(this.method, ResultMap.class);
		return (null != resultMap && StringUtils.hasText(resultMap.value())) ? resultMap.value() : null;
	}

	public Class<?> getResultType() {
		ResultType resultMap = AnnotationUtils.findAnnotation(this.method, ResultType.class);
		if (null == resultMap) {
			return null;
		}
		return resultMap.value();
	}

	public String getSelectColumns() {
		SelectColumns columns = AnnotationUtils.findAnnotation(this.method, SelectColumns.class);
		return (null != columns && StringUtils.hasText(columns.value())) ? columns.value() : null;
	}

	public String getStatementId() {
		return this.getNamespace() + '.' + getStatementName();
	}

	public SqlCommandType getModifyingType() {
		if (!this.isModifyingQuery()) {
			return null;
		}

		String value = this.getMergedOrDefaultAnnotationValue("value", Modifying.class, String.class);
		if (StringUtils.isEmpty(value)) {
			return null;
		}
		if ("insert".equalsIgnoreCase(value)) {
			return SqlCommandType.INSERT;
		}
		if ("update".equalsIgnoreCase(value)) {
			return SqlCommandType.UPDATE;
		}
		if ("delete".equalsIgnoreCase(value)) {
			return SqlCommandType.DELETE;
		}
		return null;
	}

	@Override
	public String getNamedQueryName() {

		String annotatedName = this.getAnnotationValue("name", String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : super.getNamedQueryName();
	}

	String getNamedCountQueryName() {

		String annotatedName = getAnnotationValue("countName", String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : getNamedQueryName() + ".count";
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
