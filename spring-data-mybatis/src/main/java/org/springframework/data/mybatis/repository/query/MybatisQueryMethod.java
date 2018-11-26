package org.springframework.data.mybatis.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private final RepositoryMetadata metadata;

	private final String namespace;

	private final String statementName;

	private Integer limitSize;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct
	 * query to use for following invocations of the method given.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public MybatisQueryMethod(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory) {

		super(method, metadata, factory);

		Assert.notNull(method, "Method must not be null!");

		this.method = method;

		Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()),
				String.format("Modifying method must not contain %s!", Parameters.TYPES));

		this.metadata = metadata;
		String namespace = getAnnotationValue("namespace", String.class);
		String statementName = getAnnotationValue("statement", String.class);
		this.namespace = StringUtils.hasText(namespace) ? namespace
				: metadata.getRepositoryInterface().getName();
		this.statementName = StringUtils.hasText(statementName) ? statementName
				: (method.getName() + UUID.randomUUID().toString().replace("-", ""));

	}

	@Override
	public MybatisEntityMetadata<?> getEntityInformation() {
		return new DefaultMybatisEntityMetadata<>(getDomainClass());
	}

	@Override
	public boolean isModifyingQuery() {
		return null != AnnotationUtils.findAnnotation(method, Modifying.class);
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
	public boolean isCollectionQuery() {
		return super.isCollectionQuery()
				&& !NATIVE_ARRAY_TYPES.contains(method.getReturnType());
	}

	Class<?> getReturnType() {

		return method.getReturnType();
	}

	private <T> T getAnnotationValue(String attribute, Class<T> type) {
		return getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
	}

	private <T> T getMergedOrDefaultAnnotationValue(String attribute,
			Class annotationType, Class<T> targetType) {

		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(method,
				annotationType);
		if (annotation == null) {
			return targetType
					.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	public boolean isAnnotatedQuery() {
		return null != AnnotationUtils.findAnnotation(method, Query.class);
	}

	public RepositoryMetadata getMetadata() {
		return metadata;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getStatementName() {
		return statementName;
	}

	public String getStatementId() {
		return getNamespace() + '.' + getStatementName();
	}

	public Integer getLimitSize() {
		return limitSize;
	}

	public void setLimitSize(Integer limitSize) {
		this.limitSize = limitSize;
	}

	String getCountStatementName() {
		String statementName = getAnnotationValue("countStatement", String.class);
		return StringUtils.hasText(statementName) ? statementName
				: ("count_" + getStatementName());
	}

	String getCountStatementId() {
		return getNamespace() + "." + getCountStatementName();
	}

}
