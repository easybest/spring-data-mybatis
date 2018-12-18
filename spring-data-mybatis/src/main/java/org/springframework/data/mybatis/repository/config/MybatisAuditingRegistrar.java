package org.springframework.data.mybatis.repository.config;

import static org.springframework.data.mybatis.repository.config.BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME;
import static org.springframework.data.mybatis.repository.config.MybatisRepositoryConfigExtension.DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.mybatis.auditing.MybatisAuditingHandler;
import org.springframework.data.mybatis.auditing.AuditingEntityListener;
import org.springframework.util.Assert;

class MybatisAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMybatisAuditing.class;
	}

	@Override
	protected void registerAuditListenerBeanDefinition(
			BeanDefinition auditingHandlerDefinition, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(MYBATIS_MAPPING_CONTEXT_BEAN_NAME)) {
			registry.registerBeanDefinition(MYBATIS_MAPPING_CONTEXT_BEAN_NAME, //
					new RootBeanDefinition(MybatisMappingContextFactoryBean.class));
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(AuditingEntityListener.class);
		builder.addPropertyValue("auditingHandler", ParsingUtils
				.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), null));
		builder.addPropertyReference("sqlSessionTemplate",
				DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME);
		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(),
				AuditingEntityListener.class.getName(), registry);
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return "mybatisAuditingHandler";
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(
			AuditingConfiguration configuration) {
		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		BeanDefinitionBuilder builder = configureDefaultAuditHandlerAttributes(
				configuration,
				BeanDefinitionBuilder.rootBeanDefinition(MybatisAuditingHandler.class));
		return builder.addConstructorArgReference(MYBATIS_MAPPING_CONTEXT_BEAN_NAME);
	}

}
