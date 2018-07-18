package org.springframework.data.mybatis.mapping;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.Column;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jarvis Song
 */
class MyBatisPersistentPropertyImpl extends AnnotationBasedPersistentProperty<MyBatisPersistentProperty>
		implements MyBatisPersistentProperty {

	static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;
	static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;
	static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(ElementCollection.class);
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(Embedded.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Column.class);
		annotations.add(OrderColumn.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	private final @Nullable Boolean usePropertyAccess;
	private final @Nullable TypeInformation<?> associationTargetType;
	private final boolean updateable;

	private org.springframework.data.mybatis.mapping.Column column;
	private org.springframework.data.mybatis.mapping.Association association;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 *
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 * @param simpleTypeHolder
	 */
	public MyBatisPersistentPropertyImpl(Property property, PersistentEntity<?, MyBatisPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);

		this.usePropertyAccess = detectPropertyAccess();
		this.associationTargetType = detectAssociationTargetType();
		this.updateable = detectUpdatability();
	}

	@Override
	public Class<?> getActualType() {
		return null != associationTargetType ? associationTargetType.getType() : super.getActualType();
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {
		return null != associationTargetType ? Collections.singleton(associationTargetType)
				: super.getPersistentEntityType();
	}

	@Override
	public boolean isIdProperty() {
		return ID_ANNOTATIONS.stream().anyMatch(it -> isAnnotationPresent(it));
	}

	@Override
	public boolean isEntity() {
		return super.isEntity();
	}

	@Override
	public boolean isAssociation() {
		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {
			if (findAnnotation(annotationType) != null) {
				return true;
			}
		}

		return getType().isAnnotationPresent(Embeddable.class);
	}

	@Override
	public boolean isTransient() {
		return isAnnotationPresent(Transient.class) || super.isTransient();
	}

	@Override
	protected Association<MyBatisPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	@Override
	public boolean usePropertyAccess() {
		return null != usePropertyAccess ? usePropertyAccess : super.usePropertyAccess();
	}

	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	@Override
	public boolean isWritable() {
		return updateable && super.isWritable();
	}

	@Override
	public org.springframework.data.mybatis.mapping.Column getMappedColumn() {
		return this.column;
	}

	@Override
	public org.springframework.data.mybatis.mapping.Association getMappedAssociation() {
		return this.association;
	}

	public void setMappedAssociation(org.springframework.data.mybatis.mapping.Association association) {
		this.association = association;
	}

	public void setMappedColumn(org.springframework.data.mybatis.mapping.Column column) {
		this.column = column;
	}

	@Nullable
	private Boolean detectPropertyAccess() {

		org.springframework.data.annotation.AccessType accessType = findAnnotation(
				org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return AccessType.Type.PROPERTY.equals(accessType.value());
		}

		Access access = findAnnotation(Access.class);

		if (access != null) {
			return javax.persistence.AccessType.PROPERTY.equals(access.value());
		}

		accessType = findPropertyOrOwnerAnnotation(org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return AccessType.Type.PROPERTY.equals(accessType.value());
		}

		access = findPropertyOrOwnerAnnotation(Access.class);

		if (access != null) {
			return javax.persistence.AccessType.PROPERTY.equals(access.value());
		}

		return null;
	}

	/**
	 * Inspects the association annotations on the property and returns the target entity type if specified.
	 *
	 * @return
	 */
	@Nullable
	private TypeInformation<?> detectAssociationTargetType() {

		if (!isAssociation()) {
			return null;
		}

		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {

			Annotation annotation = findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			Object entityValue = AnnotationUtils.getValue(annotation, "targetEntity");

			if (entityValue == null || entityValue.equals(void.class)) {
				continue;
			}

			return ClassTypeInformation.from((Class<?>) entityValue);
		}

		return null;
	}

	/**
	 * Checks whether {@code updatable} attribute of any of the {@link #UPDATEABLE_ANNOTATIONS} is configured to
	 * {@literal true}.
	 *
	 * @return
	 */
	private boolean detectUpdatability() {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Annotation annotation = findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			return (boolean) AnnotationUtils.getValue(annotation, "updatable");
		}

		return true;
	}
}
