package org.springframework.data.mybatis.repository.config;

import javax.annotation.Nonnull;

import org.springframework.data.auditing.config.AuditingHandlerBeanDefinitionParser;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mybatis.auditing.MybatisAuditingHandler;

import org.w3c.dom.Element;

public class MybatisAuditingHandlerBeanDefinitionParser
		extends AuditingHandlerBeanDefinitionParser {

	/**
	 * Creates a new {@link AuditingHandlerBeanDefinitionParser} to point to a
	 * {@link MappingContext} with the given bean name.
	 * @param mappingContextBeanName must not be {@literal null} or empty.
	 */
	public MybatisAuditingHandlerBeanDefinitionParser(String mappingContextBeanName) {
		super(mappingContextBeanName);
	}

	@Nonnull
	@Override
	protected Class<?> getBeanClass(Element element) {
		return MybatisAuditingHandler.class;
	}

}
