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

import java.lang.annotation.Annotation;
import java.util.Locale;

import io.easybest.mybatis.repository.config.EnableMybatisRepositories;
import io.easybest.mybatis.repository.config.MybatisRepositoryConfigExtension;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data MyBatis
 * Repositories.
 *
 * @author Jarvis Song
 */
class MybatisRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	private BootstrapMode bootstrapMode = null;

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMybatisRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableMybatisRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {

		return new MybatisRepositoryConfigExtension();
	}

	@Override
	protected BootstrapMode getBootstrapMode() {

		return null == this.bootstrapMode ? BootstrapMode.DEFAULT : this.bootstrapMode;
	}

	@Override
	public void setEnvironment(Environment environment) {

		super.setEnvironment(environment);
		this.configureBootstrapMode(environment);
	}

	private void configureBootstrapMode(Environment environment) {

		String property = environment.getProperty("spring.data.mybatis.repositories.bootstrap-mode");
		if (StringUtils.hasText(property)) {
			this.bootstrapMode = BootstrapMode.valueOf(property.toUpperCase(Locale.ENGLISH));
		}
	}

	@EnableMybatisRepositories
	private static class EnableMybatisRepositoriesConfiguration {

	}

}
