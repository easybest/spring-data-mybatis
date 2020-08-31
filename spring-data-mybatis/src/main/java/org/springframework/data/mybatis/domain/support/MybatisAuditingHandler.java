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
package org.springframework.data.mybatis.domain.support;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

/**
 * Mybatis auditing handler.
 *
 * @author JARVIS SONG
 * @since 2.0.1
 */
public class MybatisAuditingHandler extends AuditingHandler {

	private SqlSessionTemplate sqlSessionTemplate;

	public MybatisAuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		super(mappingContext);
	}

	public MybatisAuditingHandler(PersistentEntities entities) {
		super(entities);
	}

	public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(this.sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		this.sqlSessionTemplate.getConfiguration().addInterceptor(new AuditingInterceptor(this));
	}

}
