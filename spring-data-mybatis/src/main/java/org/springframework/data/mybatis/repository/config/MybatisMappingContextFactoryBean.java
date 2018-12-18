package org.springframework.data.mybatis.repository.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mybatis.id.Snowflake;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MybatisMappingContextFactoryBean extends
		AbstractFactoryBean<MybatisMappingContext> implements ApplicationContextAware {

	private @Nullable ListableBeanFactory beanFactory;

	@Override
	public Class<?> getObjectType() {
		return MybatisMappingContext.class;
	}

	@Override
	protected MybatisMappingContext createInstance() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Initializing MybatisMappingContext…");
		}

		MybatisMappingContext context = new MybatisMappingContext();
		context.initialize();

		if (log.isDebugEnabled()) {
			log.debug("Finished initializing MybatisMappingContext!");
		}

		try {
			Snowflake snowflake = beanFactory.getBean(Snowflake.class);
			if (null != snowflake) {
				context.setSnowflake(snowflake);
			}
		}
		catch (NoSuchBeanDefinitionException e) {
		}

		try {
			FieldNamingStrategy fieldNamingStrategy = beanFactory
					.getBean(FieldNamingStrategy.class);
			if (null != fieldNamingStrategy) {
				context.setFieldNamingStrategy(fieldNamingStrategy);
			}
		}
		catch (NoSuchBeanDefinitionException e) {
		}

		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.beanFactory = applicationContext;
	}

}
