package org.springframework.data.mybatis.repository.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.lang.Nullable;

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

		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.beanFactory = applicationContext;
	}

}
