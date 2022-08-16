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

package io.easybest.mybatis.autoconfigure;

import java.util.List;

import javax.sql.DataSource;

import io.easybest.mybatis.mapping.DefaultEntityManager;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.NamingStrategy;
import io.easybest.mybatis.repository.MybatisRepository;
import io.easybest.mybatis.repository.config.MybatisRepositoryConfigExtension;
import io.easybest.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * .
 *
 * @author Jarvis Song
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(MybatisRepository.class)
@ConditionalOnMissingBean({ MybatisRepositoryFactoryBean.class, MybatisRepositoryConfigExtension.class })
@ConditionalOnProperty(prefix = SpringDataMybatisProperties.PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SpringDataMybatisProperties.class)
@Import(MybatisRepositoriesRegistrar.class)
@AutoConfigureAfter({ MybatisAutoConfiguration.class, TaskExecutionAutoConfiguration.class })
public class MybatisRepositoriesAutoConfiguration {

	private final SpringDataMybatisProperties properties;

	public MybatisRepositoriesAutoConfiguration(SpringDataMybatisProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public EntityManager entityManager(BeanFactory beanFactory, SqlSessionTemplate sqlSessionTemplate) {

		DefaultEntityManager entityManager = new DefaultEntityManager(sqlSessionTemplate);

		if (null != this.properties.getEntityPackages()) {
			entityManager.setEntityPackages(this.properties.getEntityPackages());
		}
		else {
			List<String> packageNames = EntityScanPackages.get(beanFactory).getPackageNames();
			if (packageNames.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
				packageNames = AutoConfigurationPackages.get(beanFactory);
			}
			if (!packageNames.isEmpty()) {
				entityManager.setEntityPackages(packageNames.toArray(new String[0]));
			}
		}

		if (null != this.properties.getNamingStrategyType()) {

			switch (this.properties.getNamingStrategyType()) {
			case UNDERSCORE:
				entityManager.setNamingStrategy(NamingStrategy.UNDERSCORE);
				break;
			case AS_IS:
				entityManager.setNamingStrategy(NamingStrategy.AS_IS);
				break;
			}
		}

		if (null != this.properties.getUniformTablePrefix()) {
			entityManager.setUniformTablePrefix(this.properties.getUniformTablePrefix());
		}

		return entityManager;
	}

}
