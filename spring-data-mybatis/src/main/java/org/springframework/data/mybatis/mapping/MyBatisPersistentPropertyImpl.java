package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mybatis.annotation.Column;
import org.springframework.data.mybatis.annotation.Id;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.StringUtils;

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

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(Id.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(Column.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	private final Lazy<Boolean> isId = Lazy.of(() -> isAnnotationPresent(Id.class));

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
	}

	@Override
	protected Association<MyBatisPersistentProperty> createAssociation() {
		return null;
	}

	@Override
	public boolean isIdProperty() {
		return ID_ANNOTATIONS.stream().anyMatch(it -> isAnnotationPresent(it));
	}

	@Override
	public String getColumnName() {
		Column column = findAnnotation(Column.class);
		if (null != column && StringUtils.hasText(column.name())) {
			return column.name();
		}
		return ParsingUtils.reconcatenateCamelCase(getName(), "_");
	}

	@Override
	public boolean isCompositeId() {
		return isIdProperty() && isEntity();
	}

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
}
