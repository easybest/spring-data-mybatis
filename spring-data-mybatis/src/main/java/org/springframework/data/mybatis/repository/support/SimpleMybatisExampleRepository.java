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

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.mybatis.repository.MybatisExampleRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * SimpleMybatisExampleRepository.
 *
 * @param <T> entity type
 * @param <ID> entity id
 * @param <EXAMPLE> entity's example type
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Repository
@Transactional(readOnly = true)
public class SimpleMybatisExampleRepository<T, ID, EXAMPLE> extends SimpleMybatisRepository<T, ID>
		implements MybatisRepositoryImplementation<T, ID>, MybatisExampleRepository<T, ID, EXAMPLE> {

	public SimpleMybatisExampleRepository(MybatisEntityInformation<T, ID> entityInformation,
			RepositoryInformation repositoryInformation, SqlSessionTemplate sqlSessionTemplate,
			AuditingHandler auditingHandler) {
		super(entityInformation, repositoryInformation, sqlSessionTemplate, auditingHandler);
	}

	@Override
	public <S extends T> List<S> findByExample(EXAMPLE example) {
		return this.selectList(ResidentStatementName.FIND_BY_EXAMPLE, example);
	}

	@Override
	public long countByExample(EXAMPLE example) {
		return this.selectOne(ResidentStatementName.COUNT_BY_EXAMPLE, example);
	}

	@Override
	public int deleteByExample(EXAMPLE example) {
		return this.delete(ResidentStatementName.DELETE_BY_EXAMPLE, example);
	}

}
