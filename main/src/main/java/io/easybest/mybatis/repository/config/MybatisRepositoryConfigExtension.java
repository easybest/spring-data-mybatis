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

package io.easybest.mybatis.repository.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import io.easybest.mybatis.repository.MybatisRepository;
import io.easybest.mybatis.repository.support.MybatisEvaluationContextExtension;
import io.easybest.mybatis.repository.support.MybatisRepositoryFactoryBean;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = TxUtils.DEFAULT_TRANSACTION_MANAGER;

	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";

	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";

	@Override
	public String getModuleName() {
		return "MyBatis";
	}

	@Override
	protected String getModulePrefix() {
		return this.getModuleName().toLowerCase(Locale.US);
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return MybatisRepositoryFactoryBean.class.getName();
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Arrays.asList(Entity.class, MappedSuperclass.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(MybatisRepository.class);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		builder.addPropertyReference("entityManager", source.getAttribute("entityManagerRef").orElse("entityManager"));

	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();

		builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
				attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Optional<String> enableDefaultTransactions = config.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);

		if (enableDefaultTransactions.isPresent() && StringUtils.hasText(enableDefaultTransactions.get())) {
			builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, enableDefaultTransactions.get());
		}
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();

		registerIfNotAlreadyRegistered(() -> {

			Object value = config instanceof AnnotationRepositoryConfigurationSource
					? config.getRequiredAttribute(ESCAPE_CHARACTER_PROPERTY, Character.class)
					: config.getAttribute(ESCAPE_CHARACTER_PROPERTY).orElse("\\");
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.rootBeanDefinition(MybatisEvaluationContextExtension.class);
			builder.addConstructorArgValue(value);

			return builder.getBeanDefinition();
		}, registry, MybatisEvaluationContextExtension.class.getName(), source);

	}

}
