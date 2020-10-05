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
package org.springframework.data.mybatis.mapping.model;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class EmbeddedDomain extends Domain implements Embedding {

	private static final long serialVersionUID = 1341386972281765879L;

	private final MybatisPersistentProperty property;

	public EmbeddedDomain(MybatisMappingContext mappingContext, Model owner, MybatisPersistentProperty property,
			String alias) {
		super(mappingContext, owner, property.getType(), alias);

		this.property = property;
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	@Override
	public String getTableAlias() {
		String alias = super.getTableAlias();
		return alias.substring(0, alias.lastIndexOf("."));
	}

}
