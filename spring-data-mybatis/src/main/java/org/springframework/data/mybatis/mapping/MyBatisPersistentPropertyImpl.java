package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.ParsingUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.ibatis.type.JdbcType.*;

class MyBatisPersistentPropertyImpl extends AnnotationBasedPersistentProperty<MyBatisPersistentProperty>
		implements MyBatisPersistentProperty {

	private static Map<Class<?>, JdbcType> javaTypesMappedToJdbcTypes = new HashMap<Class<?>, JdbcType>();

	static {
		javaTypesMappedToJdbcTypes.put(String.class, VARCHAR);
		javaTypesMappedToJdbcTypes.put(java.math.BigDecimal.class, NUMERIC);
		javaTypesMappedToJdbcTypes.put(boolean.class, BIT);
		javaTypesMappedToJdbcTypes.put(byte.class, TINYINT);
		javaTypesMappedToJdbcTypes.put(short.class, SMALLINT);
		javaTypesMappedToJdbcTypes.put(int.class, INTEGER);
		javaTypesMappedToJdbcTypes.put(long.class, BIGINT);
		javaTypesMappedToJdbcTypes.put(float.class, REAL);
		javaTypesMappedToJdbcTypes.put(double.class, DOUBLE);
		javaTypesMappedToJdbcTypes.put(byte[].class, VARBINARY);
		javaTypesMappedToJdbcTypes.put(java.util.Date.class, TIMESTAMP);
		javaTypesMappedToJdbcTypes.put(java.sql.Date.class, DATE);
		javaTypesMappedToJdbcTypes.put(java.sql.Time.class, TIME);
		javaTypesMappedToJdbcTypes.put(java.sql.Timestamp.class, TIMESTAMP);

		javaTypesMappedToJdbcTypes.put(Boolean.class, BIT);
		javaTypesMappedToJdbcTypes.put(Integer.class, INTEGER);
		javaTypesMappedToJdbcTypes.put(Long.class, BIGINT);
		javaTypesMappedToJdbcTypes.put(Float.class, REAL);
		javaTypesMappedToJdbcTypes.put(Double.class, DOUBLE);

	}

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(Embedded.class);
		// annotations.add(Embeddable.class);
		annotations.add(ElementCollection.class);

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

		MyBatisPersistentProperty obverse = null;

		// if (isAssociation()) {
		// Class<?> targetType = getActualType();
		// MyBatisPersistentEntity owner = (MyBatisPersistentEntity) getOwner();
		// MyBatisPersistentEntityImpl<?> targetEntity = owner.getMappingContext().getPersistentEntity(targetType);
		//
		//
		//
		// }

		return new Association<>(this, obverse);
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
	public String getColumnName() {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Annotation annotation = findAnnotation(annotationType);

			if (null == annotation) {
				continue;
			}

			String name = (String) AnnotationUtils.getValue(annotation, "name");
			if (StringUtils.hasText(name)) {
				return name;
			}
		}

		return ParsingUtils.reconcatenateCamelCase(getName(), "_");
	}

	/**
	 * 1 将组件类注解为@Embeddable,并将组件的属性注解为@Id <br/>
	 * 2 将组件的属性注解为@EmbeddedId (方便) <br/>
	 * 3 将类注解为@IdClass,并将该实体中所有属于主键的属性都注解为@Id<br/>
	 * 第三种情况在此返回false.
	 * 
	 * @return
	 */
	@Override
	public boolean isCompositeId() {

		// 2.
		if (isAnnotationPresent(EmbeddedId.class)) {
			return true;
		}

		// 1.
		if (isAnnotationPresent(Id.class) && isAssociation()) {

			if (this.getAssociation().getInverse().getOwner().isAnnotationPresent(Embeddable.class)) {
				return true;
			} else {
				return false;
			}
		}

		return isIdProperty() && getOwner().isAnnotationPresent(IdClass.class);
	}

	@Override
	public boolean isClearlyId() {

		return (isAnnotationPresent(Id.class) && !isCompositeId())
				|| (isAnnotationPresent(Id.class) && getOwner().isAnnotationPresent(IdClass.class));
	}

	@Override
	public boolean isSingleId() {
		return isIdProperty() && !isCompositeId();
	}

	/**
	 * Java Type JDBC type String VARCHAR or LONGVARCHAR java.math.BigDecimal NUMERIC boolean BIT byte TINYINT short
	 * SMALLINT int INTEGER long BIGINT float REAL double DOUBLE byte[] VARBINARY or LONGVARBINARY java.sql.Date DATE
	 * java.sql.Time TIME java.sql.Timestamp TIMESTAMP ---------------------------------------- Java Object Type JDBC Type
	 * String VARCHAR or LONGVARCHAR java.math.BigDecimal NUMERIC Boolean BIT Integer INTEGER Long BIGINT Float REAL
	 * Double DOUBLE byte[] VARBINARY or LONGVARBINARY java.sql.Date DATE java.sql.Time TIME java.sql.Timestamp TIMESTAMP
	 *
	 * @return
	 */
	@Override
	public JdbcType getJdbcType() {

		org.springframework.data.mybatis.annotation.JdbcType jdbcType = findAnnotation(
				org.springframework.data.mybatis.annotation.JdbcType.class);
		if (null != jdbcType) {
			return JdbcType.valueOf(jdbcType.value());
		}
		Class<?> type = getActualType();

		JdbcType t = javaTypesMappedToJdbcTypes.get(type);
		if (null != t) {
			return t;
		}

		return UNDEFINED;
	}

	@Override
	public MyBatisPersistentEntity<?> getOwnerEntity() {
		return (MyBatisPersistentEntity<?>) getOwner();
	}



	/**
	 * Looks up both Spring Data's and JPA's access type definition annotations on the property or type level to determine
	 * the access type to be used. Will consider property-level annotations over type-level ones, favoring the Spring Data
	 * ones over the JPA ones if found on the same level. Returns {@literal null} if no explicit annotation can be found
	 * falling back to the defaults implemented in the super class.
	 *
	 * @return
	 */
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
