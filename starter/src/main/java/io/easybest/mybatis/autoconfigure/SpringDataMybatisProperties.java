/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * .
 *
 * @author Jarvis Song
 */
@ConfigurationProperties(SpringDataMybatisProperties.PREFIX)
public class SpringDataMybatisProperties {

	/**
	 * Properties prefix.
	 */
	public static final String PREFIX = "spring.data.mybatis.repositories";

	private boolean enabled;

	private String[] entityPackages;

	public String[] getEntityPackages() {
		return this.entityPackages;
	}

	public void setEntityPackages(String[] entityPackages) {
		this.entityPackages = entityPackages;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
