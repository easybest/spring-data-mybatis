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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationUtils;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable {@link EnableMybatisRepositories}
 * annotation.
 *
 * @author JARVIS SONG
 */
class MybatisRepositoriesRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	private Environment environment;

	private ResourceLoader resourceLoader;

	@Override
	public void setEnvironment(Environment environment) {

		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {

		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
			BeanNameGenerator generator) {

		Assert.notNull(metadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.notNull(this.resourceLoader, "ResourceLoader must not be null!");

		// Guard against calls for sub-classes
		if (metadata.getAnnotationAttributes(this.getAnnotation().getName()) == null) {
			return;
		}

		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				metadata, this.getAnnotation(), this.resourceLoader, this.environment, registry, generator);

		RepositoryConfigurationExtension extension = this.getExtension();
		RepositoryConfigurationUtils.exposeRegistration(extension, registry, configurationSource);

		RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configurationSource,
				this.resourceLoader, this.environment);

		delegate.registerRepositoriesIn(registry, extension);
	}

	private RepositoryConfigurationExtension getExtension() {
		return new MybatisRepositoryConfigExtension(this.resourceLoader);
	}

	private Class<? extends Annotation> getAnnotation() {
		return EnableMybatisRepositories.class;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
		this.registerBeanDefinitions(annotationMetadata, registry,
				ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR);
	}

}
