package org.springframework.data.mybatis.repository.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.DialectFactoryBean;
import org.springframework.data.mybatis.repository.MyBatisRepository;
import org.springframework.data.mybatis.repository.support.MyBatisRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * MyBatis specific configuration extension parsing custom attributes from the XML namespace and
 * {@link EnableMyBatisRepositories} annotation.
 * 
 * @author Jarvis Song
 */
public class MyBatisRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {
	private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";
	private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
	private static final String DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME = "sqlSessionTemplate";

	@Override
	public String getModuleName() {
		return "MyBatis";
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return MyBatisRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return getModuleName().toLowerCase(Locale.US);
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {

		List<Class<? extends Annotation>> annotations = Arrays.asList(Entity.class, MappedSuperclass.class);
		return annotations;
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Collections.singleton(MyBatisRepository.class);
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {

		Optional<String> transactionManagerRef = source.getAttribute("transactionManagerRef");
		builder.addPropertyValue("transactionManager", transactionManagerRef.orElse(DEFAULT_TRANSACTION_MANAGER_BEAN_NAME));

		Optional<String> sqlSessionTemplateRef = source.getAttribute("sqlSessionTemplateRef");
		builder.addPropertyReference("sqlSessionTemplate",
				sqlSessionTemplateRef.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME));
		builder.addPropertyReference("mappingContext",
				sqlSessionTemplateRef.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME).concat("_mappingContext"));

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
		String sqlSessionTemplateRef = config.getAttribute("sqlSessionTemplateRef")
				.orElse(DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME);

		BeanDefinitionBuilder mappingContextBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(MyBatisMappingContextFactoryBean.class);
		mappingContextBeanDefinitionBuilder.addPropertyValue("repositoryConfigurationSource", config);
		registerIfNotAlreadyRegistered(mappingContextBeanDefinitionBuilder.getBeanDefinition(), registry,
				sqlSessionTemplateRef.concat("_mappingContext"), source);

		Class<?> dialectClass = Dialect.class;
		if (config instanceof AnnotationRepositoryConfigurationSource) {
			AnnotationAttributes attributes = ((AnnotationRepositoryConfigurationSource) config).getAttributes();
			dialectClass = attributes.getClass("dialect");
		} else {
			Optional<String> dialect = config.getAttribute("dialect");
			if (dialect.isPresent()) {
				try {
					dialectClass = ClassUtils.forName(dialect.get(), ClassUtils.getDefaultClassLoader());
				} catch (ClassNotFoundException e) {}
			}
		}
		if (null == dialectClass || dialectClass == Dialect.class) {
			BeanDefinitionBuilder dialectBeanDefinitionBuilder = BeanDefinitionBuilder
					.rootBeanDefinition(DialectFactoryBean.class);
			dialectBeanDefinitionBuilder.addConstructorArgReference(sqlSessionTemplateRef);
			registerIfNotAlreadyRegistered(dialectBeanDefinitionBuilder.getBeanDefinition(), registry,
					sqlSessionTemplateRef.concat("_dialect"), source);
		} else {
			BeanDefinitionBuilder dialectBeanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(dialectClass);
			registerIfNotAlreadyRegistered(dialectBeanDefinitionBuilder.getBeanDefinition(), registry,
					sqlSessionTemplateRef.concat("_dialect"), source);
		}
	}
}
