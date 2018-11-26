package org.springframework.data.mybatis.mapping;

import static org.apache.ibatis.type.JdbcType.BIGINT;
import static org.apache.ibatis.type.JdbcType.BIT;
import static org.apache.ibatis.type.JdbcType.BLOB;
import static org.apache.ibatis.type.JdbcType.CLOB;
import static org.apache.ibatis.type.JdbcType.DATE;
import static org.apache.ibatis.type.JdbcType.DOUBLE;
import static org.apache.ibatis.type.JdbcType.INTEGER;
import static org.apache.ibatis.type.JdbcType.NUMERIC;
import static org.apache.ibatis.type.JdbcType.REAL;
import static org.apache.ibatis.type.JdbcType.SMALLINT;
import static org.apache.ibatis.type.JdbcType.TIME;
import static org.apache.ibatis.type.JdbcType.TIMESTAMP;
import static org.apache.ibatis.type.JdbcType.TINYINT;
import static org.apache.ibatis.type.JdbcType.UNDEFINED;
import static org.apache.ibatis.type.JdbcType.VARBINARY;
import static org.apache.ibatis.type.JdbcType.VARCHAR;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class MybatisPersistentPropertyImpl
		extends AnnotationBasedPersistentProperty<MybatisPersistentProperty>
		implements MybatisPersistentProperty {

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	private static final Map<Class<?>, JdbcType> JAVA_MAPPED_TO_JDBC_TYPES;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Column.class);
		annotations.add(OrderColumn.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		Map<Class<?>, JdbcType> javaTypesMappedToJdbcTypes = new HashMap<>();
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

		JAVA_MAPPED_TO_JDBC_TYPES = Collections
				.unmodifiableMap(javaTypesMappedToJdbcTypes);

	}

	private final @Nullable Boolean usePropertyAccess;

	private final @Nullable TypeInformation<?> associationTargetType;

	private final boolean updateable;

	private final Lazy<Boolean> isIdProperty;

	private final Lazy<Boolean> isAssociation;

	private final Lazy<Boolean> isEntity;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 */
	public MybatisPersistentPropertyImpl(Property property,
			PersistentEntity<?, MybatisPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.isAssociation = Lazy.of(() -> ASSOCIATION_ANNOTATIONS.stream()
				.anyMatch(this::isAnnotationPresent));
		this.usePropertyAccess = detectPropertyAccess();
		this.associationTargetType = detectAssociationTargetType();
		this.updateable = detectUpdatability();

		this.isIdProperty = Lazy.of(
				() -> ID_ANNOTATIONS.stream().anyMatch(it -> isAnnotationPresent(it)));
		this.isEntity = Lazy.of(getOwner().isAnnotationPresent(Entity.class));
	}

	@Override
	protected Association<MybatisPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	@Override
	public boolean isIdProperty() {
		return isIdProperty.get();
	}

	@Override
	public boolean isEntity() {
		return isEntity.get();
	}

	@Override
	public boolean isEmbeddable() {
		return isAnnotationPresent(Embedded.class)
				|| hasActualTypeAnnotation(Embeddable.class);
	}

	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	@Override
	public Class<?> getActualType() {
		return associationTargetType != null ? associationTargetType.getType()
				: super.getActualType();
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypes() {
		return associationTargetType != null //
				? Collections.singleton(associationTargetType) //
				: super.getPersistentEntityTypes();
	}

	@Override
	public boolean isTransient() {
		return isAnnotationPresent(Transient.class) || super.isTransient();
	}

	@Override
	public boolean isAssociation() {
		return isAssociation.get();
	}

	@Override
	public boolean usePropertyAccess() {
		return usePropertyAccess != null ? usePropertyAccess : super.usePropertyAccess();
	}

	@Override
	public boolean isWritable() {
		return updateable && super.isWritable();
	}

	@Override
	public String getColumnName() {

		if (isAnnotationPresent(Column.class)) {
			Column column = getRequiredAnnotation(Column.class);
			if (StringUtils.hasText(column.name())) {
				return column.name();
			}
		}

		if (isAnnotationPresent(OrderColumn.class)) {
			OrderColumn orderColumn = getRequiredAnnotation(OrderColumn.class);
			if (StringUtils.hasText(orderColumn.name())) {
				return orderColumn.name();
			}
		}

		return getOwner().getFieldNamingStrategy().getFieldName(this);
	}

	@Override
	public JdbcType getJdbcType() {

		if (isAnnotationPresent(
				org.springframework.data.mybatis.annotation.JdbcType.class)) {
			return JdbcType.valueOf(getRequiredAnnotation(
					org.springframework.data.mybatis.annotation.JdbcType.class).value());
		}

		if (isAnnotationPresent(Temporal.class)) {
			Temporal temporal = getRequiredAnnotation(Temporal.class);
			switch (temporal.value()) {

			case DATE:
				return DATE;
			case TIME:
				return TIME;
			case TIMESTAMP:
				return TIMESTAMP;
			}
		}

		Class<?> actualType = getActualType();

		if (isAnnotationPresent(Lob.class)) {
			if (actualType == String.class) {
				return CLOB;
			}
			return BLOB;
		}

		JdbcType jdbcType = JAVA_MAPPED_TO_JDBC_TYPES.get(actualType);

		return null == jdbcType ? UNDEFINED : jdbcType;
	}

	@Override
	public Class<? extends TypeHandler> getSpecifiedTypeHandler() {
		if (isAnnotationPresent(
				org.springframework.data.mybatis.annotation.TypeHandler.class)) {
			String value = getRequiredAnnotation(
					org.springframework.data.mybatis.annotation.TypeHandler.class)
							.value();
			try {
				Class<?> clz = ClassUtils.forName(value,
						ClassUtils.getDefaultClassLoader());

				if (!TypeHandler.class.isAssignableFrom(clz)) {
					throw new MappingException("The specified type handler with value: "
							+ value
							+ " must implement from org.apache.ibatis.type.TypeHandler");
				}
				return (Class<? extends TypeHandler>) clz;
			}
			catch (ClassNotFoundException e) {
				throw new MappingException("The specified type handler with value: "
						+ value + " not found.");
			}
		}
		return null;
	}

	@Override
	public MybatisPersistentEntity getOwner() {
		return (MybatisPersistentEntity) super.getOwner();
	}

	@Nullable
	private Boolean detectPropertyAccess() {

		org.springframework.data.annotation.AccessType accessType = findAnnotation(
				org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return Type.PROPERTY.equals(accessType.value());
		}

		Access access = findAnnotation(Access.class);

		if (access != null) {
			return AccessType.PROPERTY.equals(access.value());
		}

		accessType = findPropertyOrOwnerAnnotation(
				org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return Type.PROPERTY.equals(accessType.value());
		}

		access = findPropertyOrOwnerAnnotation(Access.class);

		if (access != null) {
			return AccessType.PROPERTY.equals(access.value());
		}

		return null;
	}

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
	 * Checks whether {@code updatable} attribute of any of the
	 * {@link #UPDATEABLE_ANNOTATIONS} is configured to {@literal true}.
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
