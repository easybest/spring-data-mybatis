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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.mybatis.domain.support.AuditingEntityListener;
import org.springframework.data.mybatis.domain.support.MybatisAuditingHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable {@link EnableMybatisAuditing}
 * annotation.
 *
 * @author JARVIS SONG
 * @since 2.0.1
 */
class MybatisAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMybatisAuditing.class;
	}

	@Override
	protected MybatisAuditingConfiguration getConfiguration(AnnotationMetadata annotationMetadata) {
		return new MybatisAnnotationAuditingConfiguration(annotationMetadata, this.getAnnotation());
	}

	@Override
	protected void registerAuditListenerBeanDefinition(BeanDefinition auditingHandlerDefinition,
			BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME)) {
			registry.registerBeanDefinition(BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME, //
					new RootBeanDefinition(MybatisMappingContextFactoryBean.class));
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AuditingEntityListener.class);
		builder.addPropertyValue("auditingHandler",
				ParsingUtils.getObjectFactoryBeanDefinition(this.getAuditingHandlerBeanName(), null));
		this.registerInfrastructureBeanWithId(builder.getRawBeanDefinition(), AuditingEntityListener.class.getName(),
				registry);
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
		Assert.notNull(annotationMetadata, "AnnotationMetadata must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		super.registerBeanDefinitions(annotationMetadata, registry);
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return "mybatisAuditingHandler";
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(AuditingConfiguration configuration) {
		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		BeanDefinitionBuilder builder = this.configureDefaultAuditHandlerAttributes(configuration,
				BeanDefinitionBuilder.rootBeanDefinition(MybatisAuditingHandler.class));
		return builder.addConstructorArgReference(BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME);
	}

	@Override
	protected BeanDefinitionBuilder configureDefaultAuditHandlerAttributes(AuditingConfiguration configuration,
			BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder beanDefinitionBuilder = super.configureDefaultAuditHandlerAttributes(configuration,
				builder);

		MybatisAuditingConfiguration mac = (MybatisAuditingConfiguration) configuration;
		if (StringUtils.hasText(mac.getSqlSessionTemplateRef())) {
			builder.addPropertyReference("sqlSessionTemplate", mac.getSqlSessionTemplateRef());
		}
		else {
			builder.addPropertyReference("sqlSessionTemplate", "sqlSessionTemplate");
		}

		return beanDefinitionBuilder;
	}

}
