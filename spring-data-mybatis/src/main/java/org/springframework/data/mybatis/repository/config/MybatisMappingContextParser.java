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

import java.util.Set;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.mybatis.mapping.MybatisEntityClassScanner;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.util.StringUtils;

/**
 * Spring Data Mybatis XML namespace parser for the {@code mybatis:mapping} element.
 *
 * @author JARVIS SONG
 */
class MybatisMappingContextParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return MybatisMappingContext.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		String id = super.resolveId(element, definition, parserContext);

		return StringUtils.hasText(id) ? id : BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String packages = element.getAttribute("entity-base-packages");
		if (StringUtils.hasText(packages)) {
			try {
				Set<Class<?>> entityClasses = MybatisEntityClassScanner
						.scan(StringUtils.commaDelimitedListToStringArray(packages));

				builder.addPropertyValue("initialEntitySet", entityClasses);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(String.format(
						"encountered exception while scanning for entity classes in package(s) [%s]", packages), ex);
			}
		}

		String sqlSessionTemplateRef = element.getAttribute("sql-session-template-ref");
		if (StringUtils.hasText(sqlSessionTemplateRef)) {
			builder.addConstructorArgReference(sqlSessionTemplateRef);
		}

		String fieldNamingStrategyRef = element.getAttribute("field-naming-strategy-ref");
		if (StringUtils.hasText(fieldNamingStrategyRef)) {
			builder.addPropertyReference("fieldNamingStrategy", fieldNamingStrategyRef);
		}

	}

}
