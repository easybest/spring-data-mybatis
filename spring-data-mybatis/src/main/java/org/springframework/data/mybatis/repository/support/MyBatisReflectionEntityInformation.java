package org.springframework.data.mybatis.repository.support;

import org.springframework.data.mybatis.annotation.Id;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Jarvis Song
 */
public class MyBatisReflectionEntityInformation<T, ID> extends MyBatisEntityInformationSupport<T, ID> {

	private static final Class<Id> DEFAULT_ID_ANNOTATION = Id.class;

	private Field field;

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the
	 * {@link org.springframework.data.annotation.Id} annotation.
	 *
	 * @param domainClass must not be {@literal null}.
	 */
	public MyBatisReflectionEntityInformation(Class<T> domainClass) {
		this(domainClass, DEFAULT_ID_ANNOTATION);
	}

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the given
	 * annotation.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public MyBatisReflectionEntityInformation(Class<T> domainClass, final Class<? extends Annotation> annotation) {

		super(domainClass);

		Assert.notNull(annotation, "Annotation must not be null!");

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(annotation);
		ReflectionUtils.doWithFields(domainClass, callback);

		try {
			this.field = callback.getRequiredField();
		} catch (IllegalStateException o_O) {
			throw new IllegalArgumentException(String.format("Couldn't find field with annotation %s!", annotation), o_O);
		}

		ReflectionUtils.makeAccessible(this.field);
	}

	@Override
	@Nullable
	public ID getId(Object entity) {

		Assert.notNull(entity, "Entity must not be null!");

		return (ID) ReflectionUtils.getField(field, entity);
	}

	@Override
	public Class<ID> getIdType() {
		return (Class<ID>) field.getType();
	}
}
