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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.data.auditing.config.AuditingHandlerBeanDefinitionParser;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mybatis.domain.support.MybatisAuditingHandler;
import org.springframework.util.StringUtils;

/**
 * {@link BeanDefinitionParser} for the {@code auditing} element.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class AuditingBeanDefinitionParser extends AuditingHandlerBeanDefinitionParser {

	/**
	 * Creates a new {@link AuditingHandlerBeanDefinitionParser} to point to a
	 * {@link MappingContext} with the given bean name.
	 * @param mappingContextBeanName must not be {@literal null} or empty.
	 */
	public AuditingBeanDefinitionParser(String mappingContextBeanName) {
		super(mappingContextBeanName);
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return MybatisAuditingHandler.class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		super.doParse(element, builder);

		String sqlSessionTemplateRef = element.getAttribute("sql-session-template-ref");

		if (StringUtils.hasText(sqlSessionTemplateRef)) {
			builder.addPropertyReference("sqlSessionTemplate", sqlSessionTemplateRef);
		}
	}

}
