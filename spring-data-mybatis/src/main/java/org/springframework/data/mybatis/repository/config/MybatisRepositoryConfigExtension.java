package org.springframework.data.mybatis.repository.config;

import static org.springframework.data.mybatis.repository.config.BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.util.StringUtils;

/**
 * Mybatis specific configuration extension parsing custom attributes from the XML
 * namespace and * {@link EnableMybatisRepositories} annotation.
 */
public class MybatisRepositoryConfigExtension
		extends RepositoryConfigurationExtensionSupport {

	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";

	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";

	public static final String DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME = "sqlSessionTemplate";

	public static final String SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE = "supportMultipleDatasources";

	@Override
	public String getModuleName() {
		return "Mybatis";
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
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		List<Class<? extends Annotation>> annotations = Arrays.asList(Entity.class,
				MappedSuperclass.class);
		return annotations;
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(MybatisRepository.class);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder,
			RepositoryConfigurationSource source) {

		Optional<String> transactionManagerRef = source
				.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager",
				transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));
		Optional<String> sqlSessionTemplateRef = source
				.getAttribute("sqlSessionTemplateRef");
		builder.addPropertyReference("sqlSessionTemplate",
				sqlSessionTemplateRef.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME));
		builder.addPropertyReference("mappingContext", MYBATIS_MAPPING_CONTEXT_BEAN_NAME);

	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder,
			AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();
		builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
				attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
		builder.addPropertyValue(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE,
				attributes.getBoolean(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE));
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder,
			XmlRepositoryConfigurationSource config) {
		Optional<String> enableDefaultTransactions = config
				.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);

		if (enableDefaultTransactions.isPresent()
				&& StringUtils.hasText(enableDefaultTransactions.get())) {
			builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE,
					enableDefaultTransactions.get());
		}

		Optional<String> supportMultipleDatasources = config
				.getAttribute(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE);
		if (supportMultipleDatasources.isPresent()) {
			builder.addPropertyValue(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE,
					supportMultipleDatasources.get());
		}

	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource config) {

		super.registerBeansForRoot(registry, config);

		Object source = config.getSource();

		registerLazyIfNotAlreadyRegistered(
				() -> new RootBeanDefinition(MybatisMappingContextFactoryBean.class),
				registry, MYBATIS_MAPPING_CONTEXT_BEAN_NAME, source);

		boolean supportMultipleDatasources = false;
		if (config instanceof AnnotationRepositoryConfigurationSource) {
			supportMultipleDatasources = ((AnnotationRepositoryConfigurationSource) config)
					.getAttributes().getBoolean(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE);
		}
		else if (config instanceof XmlRepositoryConfigurationSource) {
			supportMultipleDatasources = config
					.getAttribute(SUPPORT_MULTIPLE_DATA_SOURCES_ATTRIBUTE)
					.map(c -> "true".equals(c)).orElse(false);
		}

		if (supportMultipleDatasources) {

			registerIfNotAlreadyRegistered(
					() -> new RootBeanDefinition(MultipleDataSourceAspect.class),
					registry, "multipleDataSourceAspect", source);

		}

	}

}
