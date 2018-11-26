package org.springframework.data.mybatis.repository.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.IdClass;
import org.springframework.beans.BeanUtils;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

public class MybatisReflectionEntityInformation<T, ID>
		extends MybatisEntityInformationSupport<T, ID> {

	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;
	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<>();

		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

	}

	private Field field;

	private Class<ID> idClass;

	public MybatisReflectionEntityInformation(Class<T> domainClass) {
		super(domainClass);

		IdClass idClass = domainClass.getAnnotation(IdClass.class);
		if (null != idClass) {
			this.idClass = idClass.value();
		}
		else {
			for (Class<? extends Annotation> annotation : ID_ANNOTATIONS) {
				AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(
						annotation);
				ReflectionUtils.doWithFields(domainClass, callback);

				try {
					this.field = callback.getRequiredField();
				}
				catch (IllegalStateException o_O) {

				}
				if (null != this.field) {
					ReflectionUtils.makeAccessible(this.field);
					break;
				}
			}
		}
	}

	@Override
	@Nullable
	public ID getId(T entity) {
		Assert.notNull(entity, "Entity must not be null!");

		if (null != idClass) {
			ID id = BeanUtils.instantiateClass(idClass);
			BeanUtils.copyProperties(entity, id);
			return id;
		}

		return (ID) ReflectionUtils.getField(field, entity);
	}

	@Override
	public Class<ID> getIdType() {
		if (null != idClass) {
			return idClass;
		}
		return (Class<ID>) field.getType();
	}

}
