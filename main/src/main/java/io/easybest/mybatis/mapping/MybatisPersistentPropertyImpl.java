/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.mapping;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.annotation.DatabaseDefault;
import io.easybest.mybatis.annotation.GetterOptional;
import io.easybest.mybatis.annotation.TypeHandler;
import io.easybest.mybatis.mapping.handler.UUIDTypeHandler;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisPersistentPropertyImpl extends AnnotationBasedPersistentProperty<MybatisPersistentPropertyImpl>
		implements MybatisPersistentProperty {

	private static final Map<Class<?>, String> TYPE_ALIASES;

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> ENTITY_ANNOTATIONS;

	private final Lazy<SqlIdentifier> columnName;

	private final @Nullable Boolean usePropertyAccess;

	private final @Nullable TypeInformation<?> associationTargetType;

	private final boolean updateable;

	private final Lazy<Boolean> isIdProperty;

	private final Lazy<Boolean> isAssociation;

	private final Lazy<Boolean> isEntity;

	private final Lazy<JdbcType> jdbcType;

	private final Lazy<String> javaType;

	private final Lazy<Class<?>> typeHandler;

	private final EntityManager entityManager;

	private boolean forceQuote = true;

	static {
		TYPE_ALIASES = new HashMap<>();
		Map<String, Class<?>> typeAliases = new TypeAliasRegistry().getTypeAliases();
		typeAliases.forEach((key, value) -> TYPE_ALIASES.put(value, key));

		Set<Class<? extends Annotation>> annotations = new HashSet<>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
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

		annotations = new HashSet<>();
		annotations.add(Entity.class);
		annotations.add(MappedSuperclass.class);
		annotations.add(Embeddable.class);

		ENTITY_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	public MybatisPersistentPropertyImpl(Property property, PersistentEntity<?, MybatisPersistentPropertyImpl> owner,
			SimpleTypeHolder simpleTypeHolder, NamingStrategy namingStrategy, EntityManager entityManager) {

		super(property, owner, simpleTypeHolder);
		this.entityManager = entityManager;

		this.isAssociation = Lazy.of(
				() -> super.isAssociation() || ASSOCIATION_ANNOTATIONS.stream().anyMatch(this::isAnnotationPresent));
		this.usePropertyAccess = this.detectPropertyAccess();

		this.associationTargetType = this.detectAssociationTargetType();
		this.updateable = this.detectUpdatability();
		this.isIdProperty = Lazy.of(() -> ID_ANNOTATIONS.stream().anyMatch(this::isAnnotationPresent));
		this.isEntity = Lazy
				.of(() -> ENTITY_ANNOTATIONS.stream().anyMatch(a -> this.getActualType().isAnnotationPresent(a)));

		this.columnName = Lazy.of(() -> Optional.ofNullable(this.findAnnotation(Column.class)).map(Column::name)
				.filter(StringUtils::hasText).map(this::createSqlIdentifier)
				.orElseGet(() -> this.createDerivedSqlIdentifier(namingStrategy.getColumnName(this))));

		this.jdbcType = Lazy.of(() -> {
			io.easybest.mybatis.annotation.JdbcType jdbcTypeAnn = this
					.findAnnotation(io.easybest.mybatis.annotation.JdbcType.class);
			if (null != jdbcTypeAnn) {
				return JdbcType.forCode(jdbcTypeAnn.value());
			}

			Temporal temporalAnn = this.findAnnotation(Temporal.class);
			if (null != temporalAnn) {
				switch (temporalAnn.value()) {
				case DATE:
					return JdbcType.DATE;
				case TIME:
					return JdbcType.TIME;
				case TIMESTAMP:
					return JdbcType.TIMESTAMP;
				}
			}

			if (this.getType().isEnum()) {
				Enumerated enumeratedAnn = this.findAnnotation(Enumerated.class);
				if (null != enumeratedAnn) {
					switch (enumeratedAnn.value()) {
					case ORDINAL:
						return JdbcType.INTEGER;
					case STRING:
						return JdbcType.VARCHAR;
					}
				}
				return JdbcType.VARCHAR;
			}

			Lob lobAnn = this.findAnnotation(Lob.class);
			if (null != lobAnn) {
				if (CharSequence.class.isAssignableFrom(this.getType())) {
					return JdbcType.CLOB;
				}
				return JdbcType.BLOB;
			}
			return TypeRegistry.convert(this.getType()).orElse(JdbcType.UNDEFINED);
		});
		this.javaType = Lazy.of(() -> {
			String type = TYPE_ALIASES.get(this.getType());
			if (null != type) {
				return type;
			}
			return this.getType().getName();
		});
		this.typeHandler = Lazy.of(() -> {
			TypeHandler typeHandlerAnn = this.findAnnotation(TypeHandler.class);
			if (null != typeHandlerAnn) {

				Class<?> clz = typeHandlerAnn.value();
				if (org.apache.ibatis.type.TypeHandler.class.isAssignableFrom(clz)) {
					return clz;
				}
				else {
					throw new MappingException(clz.getName() + " is not a validated type handler.");
				}
			}
			if (this.isEnumerated()) {
				Enumerated enumeratedAnn = this.findAnnotation(Enumerated.class);
				if (null != enumeratedAnn) {
					switch (enumeratedAnn.value()) {
					case ORDINAL:
						return EnumOrdinalTypeHandler.class;

					case STRING:
						return EnumTypeHandler.class;
					}
				}
				return EnumTypeHandler.class;
			}
			if (this.getType() == UUID.class) {
				return UUIDTypeHandler.class;
			}
			return null;
		});
	}

	@Override
	public MybatisPersistentEntityImpl<?> getOwner() {
		return (MybatisPersistentEntityImpl<?>) super.getOwner();
	}

	@Override
	protected MybatisAssociation createAssociation() {
		return new MybatisAssociation(this, null, this.entityManager, this.forceQuote);
	}

	@Override
	public MybatisAssociation getAssociation() {
		return (MybatisAssociation) super.getAssociation();
	}

	@Override
	public MybatisAssociation getRequiredAssociation() {
		return (MybatisAssociation) super.getRequiredAssociation();
	}

	@Override
	public Class<?> getActualType() {
		return null != this.associationTargetType ? this.associationTargetType.getType() : super.getActualType();
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypes() {
		return this.getPersistentEntityTypeInformation();
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypeInformation() {
		return null != this.associationTargetType ? Collections.singleton(this.associationTargetType)
				: super.getPersistentEntityTypeInformation();
	}

	@Override
	public TypeInformation<?> getAssociationTargetTypeInformation() {

		if (!this.isAssociation()) {
			return null;
		}

		if (null != this.associationTargetType) {
			return this.associationTargetType;
		}

		TypeInformation<?> targetType = super.getAssociationTargetTypeInformation();
		return null != targetType ? targetType : this.getActualTypeInformation();

	}

	@Override
	public boolean isIdProperty() {
		return this.isIdProperty.get();
	}

	@Override
	public boolean isEntity() {
		return this.isEntity.get();
	}

	@Override
	public boolean isAssociation() {
		return this.isAssociation.get();
	}

	@Override
	public boolean isTransient() {
		return this.isAnnotationPresent(Transient.class) || super.isTransient();
	}

	@Override
	public boolean usePropertyAccess() {
		return null != this.usePropertyAccess ? this.usePropertyAccess : super.usePropertyAccess();
	}

	@Override
	public boolean isVersionProperty() {
		return this.isAnnotationPresent(Version.class);
	}

	@Override
	public boolean isWritable() {
		return this.updateable && super.isWritable();
	}

	@Override
	public boolean isEmbeddable() {
		return this.isAnnotationPresent(Embedded.class) || this.hasActualTypeAnnotation(Embeddable.class);
	}

	@Override
	public boolean isDatabaseDefaultValue() {
		return this.isAnnotationPresent(DatabaseDefault.class);
	}

	private SqlIdentifier createSqlIdentifier(String name) {
		return this.isForceQuote() ? SqlIdentifier.quoted(name) : SqlIdentifier.unquoted(name);
	}

	private SqlIdentifier createDerivedSqlIdentifier(String name) {
		return new DerivedSqlIdentifier(name, this.isForceQuote());
	}

	@Override
	public SqlIdentifier getColumnName() {
		return this.columnName.get();
	}

	@Override
	public JdbcType getJdbcType() {
		return this.jdbcType.get();
	}

	@Override
	public String getJavaType() {
		return this.javaType.get();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Class<? extends org.apache.ibatis.type.TypeHandler<?>> getTypeHandler() {
		return (Class<? extends org.apache.ibatis.type.TypeHandler<?>>) this.typeHandler.getNullable();
	}

	@Override
	public boolean isGetterOptional() {

		return null != this.getGetter() && this.getGetter().getReturnType() == Optional.class
				|| this.isAnnotationPresent(GetterOptional.class);
	}

	private boolean isEnumerated() {
		return this.isAnnotationPresent(Enumerated.class) || this.getType().isEnum();
	}

	public boolean isForceQuote() {
		return this.forceQuote;
	}

	public void setForceQuote(boolean forceQuote) {
		this.forceQuote = forceQuote;
	}

	@Nullable
	private Boolean detectPropertyAccess() {

		org.springframework.data.annotation.AccessType accessType = this
				.findAnnotation(org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return AccessType.Type.PROPERTY.equals(accessType.value());
		}

		Access access = this.findAnnotation(Access.class);

		if (access != null) {
			return javax.persistence.AccessType.PROPERTY.equals(access.value());
		}

		accessType = this.findPropertyOrOwnerAnnotation(org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return AccessType.Type.PROPERTY.equals(accessType.value());
		}

		access = this.findPropertyOrOwnerAnnotation(Access.class);

		if (access != null) {
			return javax.persistence.AccessType.PROPERTY.equals(access.value());
		}

		return null;
	}

	@Nullable
	private TypeInformation<?> detectAssociationTargetType() {

		if (!this.isAssociation()) {
			return null;
		}

		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {

			Annotation annotation = this.findAnnotation(annotationType);

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

	private boolean detectUpdatability() {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Annotation annotation = this.findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			return (boolean) AnnotationUtils.getValue(annotation, "updatable");
		}

		return true;
	}

}
