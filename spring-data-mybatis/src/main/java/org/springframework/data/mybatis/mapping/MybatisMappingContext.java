package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.CamelCaseSplittingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mybatis.id.Snowflake;
import org.springframework.data.util.TypeInformation;

import lombok.Getter;
import lombok.Setter;

public class MybatisMappingContext extends
		AbstractMappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentProperty> {

	private static final FieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = new CamelCaseSplittingFieldNamingStrategy(
			"_");

	@Getter
	@Setter
	private FieldNamingStrategy fieldNamingStrategy = DEFAULT_FIELD_NAMING_STRATEGY;

	@Getter
	@Setter
	private Snowflake snowflake;

	@Override
	protected <T> MybatisPersistentEntityImpl<?> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		MybatisPersistentEntityImpl<T> entity = new MybatisPersistentEntityImpl<>(
				typeInformation, this);
		entity.setGlobalFieldNamingStrategy(fieldNamingStrategy);
		return entity;
	}

	@Override
	protected MybatisPersistentProperty createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

}
