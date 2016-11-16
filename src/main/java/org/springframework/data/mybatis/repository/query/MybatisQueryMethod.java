package org.springframework.data.mybatis.repository.query;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.repository.annotation.Query;
import org.springframework.data.mybatis.repository.support.MybatisEntityInformationSupport;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class MybatisQueryMethod extends QueryMethod {

    private static final Set<Class<?>> NATIVE_ARRAY_TYPES;

    private final Method method;

    static {

        Set<Class<?>> types = new HashSet<Class<?>>();
        types.add(byte[].class);
        types.add(Byte[].class);
        types.add(char[].class);
        types.add(Character[].class);

        NATIVE_ARRAY_TYPES = Collections.unmodifiableSet(types);
    }


    /**
     * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query to use for following
     * invocations of the method given.
     *
     * @param method   must not be {@literal null}.
     * @param metadata must not be {@literal null}.
     * @param factory  must not be {@literal null}.
     */
    public MybatisQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory);

        Assert.notNull(method, "Method must not be null!");
        this.method = method;

        Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()),
                String.format("Modifying method must not contain %s!", Parameters.TYPES));

    }

    @Override
    protected Parameters<?, ?> createParameters(Method method) {
        return new MybatisParameters(method);
    }

    @Override
    public MybatisParameters getParameters() {
        return (MybatisParameters) super.getParameters();
    }

    @Override
    public MybatisEntityInformationSupport<?, ?> getEntityInformation() {
        return MybatisEntityInformationSupport.getEntityInformation(getDomainClass());
    }

    @Override
    public boolean isCollectionQuery() {
        return super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(method.getReturnType());

    }

    Class<?> getReturnType() {
        return method.getReturnType();
    }

    Query getQueryAnnotation() {
        return method.getAnnotation(Query.class);
    }

    String getAnnotatedQuery() {

        String query = getAnnotationValue("value", String.class);
        return StringUtils.hasText(query) ? query : null;
    }

    private <T> T getAnnotationValue(String attribute, Class<T> type) {
        return getMergedOrDefaultAnnotationValue(attribute, Query.class, type);
    }

    private <T> T getMergedOrDefaultAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {

        Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (annotation == null) {
            return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
        }

        return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
    }

    private void assertParameterNamesInAnnotatedQuery() {

        String annotatedQuery = getAnnotatedQuery();

        if (!QueryUtils.hasNamedParameter(annotatedQuery)) {
            return;
        }

        for (Parameter parameter : getParameters()) {

            if (!parameter.isNamedParameter()) {
                continue;
            }

            if (!annotatedQuery.contains(String.format(":%s", parameter.getName()))
                    && !annotatedQuery.contains(String.format("#%s", parameter.getName()))) {
                throw new IllegalStateException(
                        String.format("Using named parameters for method %s but parameter '%s' not found in annotated query '%s'!",
                                method, parameter.getName(), annotatedQuery));
            }
        }
    }
}
