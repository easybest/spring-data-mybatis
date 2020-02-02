/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.repository.config;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} to setup {@link MybatisMappingContext} instances from Spring
 * configuration.
 *
 * @author JARVIS SONG
 */
@Slf4j
class MybatisMappingContextFactoryBean extends AbstractFactoryBean<MybatisMappingContext>
		implements ApplicationContextAware {

	private @Nullable ListableBeanFactory beanFactory;

	private final Set<? extends Class<?>> initialEntitySet;

	MybatisMappingContextFactoryBean(Set<? extends Class<?>> initialEntitySet) {
		if (null == initialEntitySet) {
			initialEntitySet = new HashSet<>();
		}
		this.initialEntitySet = initialEntitySet;
	}

	@Override
	public Class<?> getObjectType() {
		return MybatisMappingContext.class;
	}

	@Override
	protected MybatisMappingContext createInstance() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Initializing MybatisMappingContext...");
		}
		MybatisMappingContext context = new MybatisMappingContext();
		context.setInitialEntitySet(this.initialEntitySet);
		context.initialize();
		if (log.isDebugEnabled()) {
			log.debug("Finished initializing MybatisMappingContext!");
		}

		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

}
