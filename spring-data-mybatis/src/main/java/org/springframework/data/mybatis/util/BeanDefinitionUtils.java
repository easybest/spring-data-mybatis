/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.mybatis.util;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class BeanDefinitionUtils {

	public static BeanDefinition getBeanDefinition(String name, ConfigurableListableBeanFactory beanFactory) {

		try {
			return beanFactory.getBeanDefinition(name);
		}
		catch (NoSuchBeanDefinitionException o_O) {

			BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

			if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
				return getBeanDefinition(name, (ConfigurableListableBeanFactory) parentBeanFactory);
			}

			throw o_O;
		}
	}

}
