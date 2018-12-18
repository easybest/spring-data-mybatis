package org.springframework.data.mybatis.mapping;

import java.util.Comparator;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class MybatisPersistentEntityImpl<T>
		extends BasicPersistentEntity<T, MybatisPersistentProperty>
		implements MybatisPersistentEntity<T> {

	private final MybatisMappingContext mappingContext;

	private FieldNamingStrategy globalFieldNamingStrategy;

	public MybatisPersistentEntityImpl(TypeInformation<T> information,
			MybatisMappingContext mappingContext) {
		super(information);

		this.mappingContext = mappingContext;
	}

	public MybatisPersistentEntityImpl(TypeInformation<T> information,
			Comparator<MybatisPersistentProperty> comparator,
			MybatisMappingContext mappingContext) {
		super(information, comparator);
		this.mappingContext = mappingContext;

	}

	public MybatisMappingContext getMappingContext() {
		return mappingContext;
	}

	public Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> getPersistentEntity(
			Class<?> type) {

		return Optional.ofNullable(this.mappingContext.getPersistentEntity(type));
	}

	public PersistentEntity<?, ? extends PersistentProperty<?>> getRequiredPersistentEntity(
			Class<?> type) {

		Assert.notNull(type, "Domain type must not be null!");

		return getPersistentEntity(type).orElseThrow(() -> new IllegalArgumentException(
				String.format("Couldn't find PersistentEntity for type %s!", type)));
	}

	@Override
	protected MybatisPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			MybatisPersistentProperty property) {
		return property.isIdProperty() ? property : null;
	}

	@Override
	public String getTableName() {

		if (isAnnotationPresent(Table.class)) {
			StringBuilder builder = new StringBuilder();
			Table table = getRequiredAnnotation(Table.class);
			if (StringUtils.hasText(table.catalog())) {
				builder.append(table.catalog()).append('.');
			}
			if (StringUtils.hasText(table.schema())) {
				builder.append(table.schema()).append('.');
			}
			if (StringUtils.hasText(table.name())) {
				builder.append(table.name());
			}
			else if (isAnnotationPresent(Entity.class)) {
				Entity entity = getRequiredAnnotation(Entity.class);
				if (StringUtils.hasText(entity.name())) {
					builder.append(entity.name());
				}
				else {
					builder.append(getType().getSimpleName());
				}
			}
			return builder.toString();
		}

		if (isAnnotationPresent(Entity.class)) {
			Entity entity = getRequiredAnnotation(Entity.class);
			if (StringUtils.hasText(entity.name())) {
				return entity.name();
			}
		}
		return getType().getSimpleName();
	}

	@Override
	public FieldNamingStrategy getFieldNamingStrategy() {
		if (isAnnotationPresent(
				org.springframework.data.mybatis.annotation.FieldNamingStrategy.class)) {
			org.springframework.data.mybatis.annotation.FieldNamingStrategy fns = getRequiredAnnotation(
					org.springframework.data.mybatis.annotation.FieldNamingStrategy.class);
			if (StringUtils.hasText(fns.value())) {
				try {
					Class<?> clz = ClassUtils.forName(fns.value(),
							ClassUtils.getDefaultClassLoader());
					if (!(FieldNamingStrategy.class.isAssignableFrom(clz))) {
						throw new MappingException(
								"@FieldNamingStrategy value must implement org.springframework.data.mapping.model.FieldNamingStrategy");
					}
					return (FieldNamingStrategy) clz.newInstance();
				}
				catch (ClassNotFoundException e) {
					throw new MappingException("@FieldNamingStrategy value for "
							+ getName() + " class not found.");
				}
				catch (Exception e) {
					throw new MappingException(
							"@FieldNamingStrategy value for " + getName() + " set error.",
							e);
				}

			}
		}
		// USE GLOBAL SETTING
		return globalFieldNamingStrategy;
	}

	public void setGlobalFieldNamingStrategy(
			FieldNamingStrategy globalFieldNamingStrategy) {
		this.globalFieldNamingStrategy = globalFieldNamingStrategy;
	}

}
