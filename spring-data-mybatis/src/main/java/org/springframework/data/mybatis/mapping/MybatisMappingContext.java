package org.springframework.data.mybatis.mapping;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.CamelCaseSplittingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class MybatisMappingContext extends
		AbstractMappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentProperty>
		implements EnvironmentAware {

	private static final FieldNamingStrategy DEFAULT_FIELD_NAMING_STRATEGY = new CamelCaseSplittingFieldNamingStrategy(
			"_");

	private FieldNamingStrategy fieldNamingStrategy = DEFAULT_FIELD_NAMING_STRATEGY;

	private Environment environment;

	@Override
	protected <T> MybatisPersistentEntityImpl<?> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		MybatisPersistentEntityImpl<T> entity = new MybatisPersistentEntityImpl<>(
				typeInformation);
		entity.setGlobalFieldNamingStrategy(fieldNamingStrategy);
		return entity;
	}

	@Override
	protected MybatisPersistentProperty createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		String property = environment
				.getProperty("spring.data.mybatis.field-naming-strategy");
		if (StringUtils.hasText(property)) {
			try {
				this.fieldNamingStrategy = (FieldNamingStrategy) ClassUtils
						.forName(property, ClassUtils.getDefaultClassLoader())
						.newInstance();
			}
			catch (Exception e) {
				throw new MappingException(e.getMessage(), e);
			}

		}
	}

	@Override
	public void setEnvironment(Environment environment) {

		this.environment = environment;
	}

}
