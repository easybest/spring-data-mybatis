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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * MyBatis specific configuration extension parsing custom attributes from the XML
 * namespace and * {@link EnableMybatisRepositories} annotation.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
@Slf4j
public class MybatisRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = TxUtils.DEFAULT_TRANSACTION_MANAGER;

	private static final String DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME = "sqlSessionTemplate";

	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";

	private static final String ESCAPE_CHARACTER_PROPERTY = "escapeCharacter";

	private final ResourceLoader resourceLoader;

	public MybatisRepositoryConfigExtension(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

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
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {
		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();
		Optional<String> sqlSessionTemplateRef = config.getAttribute("sqlSessionTemplateRef");
		registerIfNotAlreadyRegistered(
				() -> BeanDefinitionBuilder.rootBeanDefinition(MybatisMappingContextFactoryBean.class)
						.addConstructorArgValue(this.scanDomains(config))
						.addConstructorArgReference(
								sqlSessionTemplateRef.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME))
						.getBeanDefinition(),
				registry, BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME, source);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
		Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));

		Optional<String> sqlSessionTemplateRef = source.getAttribute("sqlSessionTemplateRef");
		builder.addPropertyReference("sqlSessionTemplate",
				sqlSessionTemplateRef.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME));
		builder.addPropertyValue(ESCAPE_CHARACTER_PROPERTY, getEscapeCharacter(source).orElse('\\'));
		builder.addPropertyReference("mappingContext", BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME);
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

	private Map<? extends Class<?>, ? extends Class<?>> scanDomains(RepositoryConfigurationSource source) {
		StopWatch watch = new StopWatch();

		if (log.isDebugEnabled()) {
			log.debug("Scanning domains for repositories in packages {}.",
					source.getBasePackages().stream().collect(Collectors.joining(", ")));
		}
		watch.start();

		Collection<RepositoryConfiguration<RepositoryConfigurationSource>> repositoryConfigurations = getRepositoryConfigurations(
				source, this.resourceLoader, false);

		if (CollectionUtils.isEmpty(repositoryConfigurations)) {
			return Collections.emptyMap();
		}

		Map<? extends Class<?>, ? extends Class<?>> mapping = repositoryConfigurations.stream()
				.collect(Collectors.toMap(configuration -> {
					try {
						return Class.forName(configuration.getRepositoryInterface());
					}
					catch (ClassNotFoundException ex) {
						throw new MappingException(ex.getMessage(), ex);
					}
				}, configuration -> {
					try {
						Class<?> repositoryClass = Class.forName(configuration.getRepositoryInterface());
						return AbstractRepositoryMetadata.getMetadata(repositoryClass).getDomainType();
					}
					catch (ClassNotFoundException ex) {
						throw new MappingException(ex.getMessage(), ex);
					}
				}));

		watch.stop();

		if (log.isInfoEnabled()) {
			log.info("Finished Domains scanning in {}ms. Found {} domains.", //
					watch.getLastTaskTimeMillis(), mapping.size());
		}

		return mapping;
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
