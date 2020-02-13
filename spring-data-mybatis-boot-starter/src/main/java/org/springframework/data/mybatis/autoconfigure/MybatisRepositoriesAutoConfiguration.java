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
package org.springframework.data.mybatis.autoconfigure;

import javax.sql.DataSource;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.config.EnableMybatisRepositories;
import org.springframework.data.mybatis.repository.config.MybatisRepositoryConfigExtension;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's MyBatis
 * Repositories.
 * <p>
 * Activates when there is a bean of type {@link javax.sql.DataSource} configured in the
 * context, the Spring Data MyBatis
 * {@link org.springframework.data.mybatis.repository.MybatisRepository} type is on the
 * classpath, and there is no other, existing
 * {@link org.springframework.data.mybatis.repository.MybatisRepository} configured.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling MyBatis
 * repositories using the {@link EnableMybatisRepositories} annotation.
 * <p>
 * This configuration class will activate <em>after</em> the MyBatis auto-configuration.
 *
 * @author JARVIS SONG
 * @see EnableMybatisRepositories
 * @since 2.0.0
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(MybatisRepository.class)
@ConditionalOnMissingBean({ MybatisRepositoryFactoryBean.class, MybatisRepositoryConfigExtension.class })
@ConditionalOnProperty(prefix = "spring.data.mybatis.repositories", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@Import(MybatisRepositoriesAutoConfigureRegistrar.class)
@AutoConfigureAfter({ MybatisAutoConfiguration.class, TaskExecutionAutoConfiguration.class })
@EnableConfigurationProperties(SpringDataMybatisProperties.class)
public class MybatisRepositoriesAutoConfiguration {

}
