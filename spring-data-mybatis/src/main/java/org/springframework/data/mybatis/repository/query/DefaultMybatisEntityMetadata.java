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
package org.springframework.data.mybatis.repository.query;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation for {@link MybatisEntityMetadata}.
 *
 * @param <T> entity type
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class DefaultMybatisEntityMetadata<T> implements MybatisEntityMetadata<T> {

	private final Class<T> domainType;

	public DefaultMybatisEntityMetadata(Class<T> domainType) {
		Assert.notNull(domainType, "Domain type must not be null.");
		this.domainType = domainType;
	}

	@Override
	public String getEntityName() {
		Entity entity = AnnotatedElementUtils.findMergedAnnotation(this.domainType, Entity.class);
		return ((null != entity) && StringUtils.hasText(entity.name())) ? entity.name()
				: this.domainType.getSimpleName();
	}

	@Override
	public String getTableName() {
		Table table = AnnotatedElementUtils.findMergedAnnotation(this.domainType, Table.class);
		if (null != table && StringUtils.hasText(table.name())) {
			return table.name();
		}
		return this.getEntityName();
	}

	@Override
	public Class<T> getJavaType() {
		return this.domainType;
	}

}
