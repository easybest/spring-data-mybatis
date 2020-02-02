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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;

/**
 * MyBatis specific configuration extension parsing custom attributes from the XML
 * namespace and * {@link EnableMybatisRepositories} annotation.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";

	@Override
	public String getModuleName() {
		return "MyBatis";
	}

	@Override
	protected String getModulePrefix() {
		return getModuleName().toLowerCase(Locale.US);
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return MybatisRepositoryFactoryBean.class.getName();
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(MybatisRepository.class);
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(Entity.class, MappedSuperclass.class, Embeddable.class);
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {
		super.registerBeansForRoot(registry, configurationSource);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
		super.postProcess(builder, source);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
		super.postProcess(builder, config);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
		super.postProcess(builder, config);
	}

	private static Optional<Character> getEscapeCharacter(RepositoryConfigurationSource source) {
		try {
			return source.getAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class);
		}
		catch (IllegalArgumentException ___) {
			return Optional.empty();
		}
	}

}
