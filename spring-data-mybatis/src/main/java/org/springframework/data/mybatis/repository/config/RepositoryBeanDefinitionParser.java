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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationUtils;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.lang.Nullable;

/**
 * Base class to implement repository namespaces.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class RepositoryBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parser) {
		XmlReaderContext readerContext = parser.getReaderContext();

		try {

			ResourceLoader resourceLoader = ConfigurationUtils.getRequiredResourceLoader(readerContext);
			Environment environment = readerContext.getEnvironment();
			BeanDefinitionRegistry registry = parser.getRegistry();

			XmlRepositoryConfigurationSource configSource = new XmlRepositoryConfigurationSource(element, parser,
					environment);
			RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configSource, resourceLoader,
					environment);

			MybatisRepositoryConfigExtension extension = new MybatisRepositoryConfigExtension(resourceLoader);

			RepositoryConfigurationUtils.exposeRegistration(extension, registry, configSource);

			for (BeanComponentDefinition definition : delegate.registerRepositoriesIn(registry, extension)) {
				readerContext.fireComponentRegistered(definition);
			}

		}
		catch (RuntimeException ex) {
			handleError(ex, element, readerContext);
		}

		return null;
	}

	private void handleError(Exception e, Element source, ReaderContext reader) {
		reader.error(e.getMessage(), reader.extractSource(source), e);
	}

}
