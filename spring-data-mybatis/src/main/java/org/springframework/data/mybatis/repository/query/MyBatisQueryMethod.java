package org.springframework.data.mybatis.repository.query;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.annotation.Modifying;
import org.springframework.data.mybatis.repository.annotation.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The mybatis implementation of {@link QueryMethod}.
 *
 * @author Jarvis Song
 */
public class MyBatisQueryMethod extends QueryMethod {

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
	private final RepositoryMetadata metadata;
	private final String namespace;
	private final String statementName;
	private Integer limitSize;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query to use for following
	 * invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public MyBatisQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
		this.metadata = metadata;

		Assert.notNull(method, "Method must not be null!");

		this.method = method;

		String namespace = getAnnotationValue("namespace", String.class);
		String statementName = getAnnotationValue("countStatement", String.class);
		this.namespace = StringUtils.hasText(namespace) ? namespace : metadata.getRepositoryInterface().getName();
		this.statementName = StringUtils.hasText(statementName) ? statementName
				: (method.getName() + UUID.randomUUID().toString().replace("-", ""));
	}

	/**
	 * Returns the actual return type of the method.
	 *
	 * @return
	 */
	Class<?> getReturnType() {
		return method.getReturnType();
	}

	@Nullable
	String getAnnotatedQuery() {

		String query = getAnnotationValue("value", String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	@Nullable
	String getCountQuery() {

		String countQuery = getAnnotationValue("countQuery", String.class);
		return StringUtils.hasText(countQuery) ? countQuery : null;
	}

	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	@Nullable
	public String getStatementName() {
		return this.statementName;
	}

	@Nullable
	String getStatementId() {
		return getNamespace() + "." + getStatementName();
	}

	@Nullable
	String getCountStatementName() {
		String statementName = getAnnotationValue("countStatement", String.class);
		return StringUtils.hasText(statementName) ? statementName : ("count_" + getStatementName());
	}

	@Nullable
	String getCountStatementId() {
		return getNamespace() + "." + getCountStatementName();
	}

	public boolean isComplexQuery() {
		return getAnnotationValue("withAssociations", Boolean.class).booleanValue();
	}

	@Override
	public boolean isCollectionQuery() {
		return super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(method.getReturnType());
	}

	@Override
	public boolean isModifyingQuery() {

		return null != AnnotationUtils.findAnnotation(method, Modifying.class);
	}

	@Override
	protected MyBatisParameters createParameters(Method method) {
		return new MyBatisParameters(method);
	}

	@Override
	public MyBatisParameters getParameters() {
		return (MyBatisParameters) super.getParameters();
	}

	@Override
	public EntityMetadata<?> getEntityInformation() {
		return new DefaultMyBatisEntityMetadata<>(getDomainClass());
	}

	/**
	 * Returns the {@link Query} annotation's attribute casted to the given type or default value if no annotation
	 * available.
	 *
	 * @param attribute
	 * @param type
	 * @return
	 */
	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
		if (annotation == null) {
			return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	public Integer getLimitSize() {
		return limitSize;
	}

	public void setLimitSize(Integer limitSize) {
		this.limitSize = limitSize;
	}
}
