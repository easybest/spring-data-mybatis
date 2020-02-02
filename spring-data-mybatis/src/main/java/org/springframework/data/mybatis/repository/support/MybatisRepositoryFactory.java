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
package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.query.EscapeCharacter;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * MyBatis specific generic repository factory.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

	private final MybatisMappingContext mappingContext;

	private final SqlSessionTemplate sqlSessionTemplate;

	private EntityPathResolver entityPathResolver;

	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	public MybatisRepositoryFactory(MybatisMappingContext mappingContext, SqlSessionTemplate sqlSessionTemplate) {
		this.mappingContext = mappingContext;
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return null;
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return null;
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return null;
	}

	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

}
