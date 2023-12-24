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

package io.easybest.mybatis.mapping.precompile;

import java.util.List;

import org.springframework.util.CollectionUtils;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.repository.query.IndicatingMybatisQuery;
import io.easybest.mybatis.repository.query.PartTreeMybatisQuery;

/**
 * .
 *
 * @author Jarvis Song
 */
public final class MybatisAggregateRootPrecompile {

	private MybatisAggregateRootPrecompile() {
	}

	public static void compile(EntityManager entityManager, StagingMappers stagingMappers,
			MybatisPersistentEntityImpl<?> entity) {

		MybatisSimpleMapperSnippet snippet = new MybatisSimpleMapperSnippet(entityManager, stagingMappers, entity);

		MybatisMapperBuilder builder = MybatisMapperBuilder
				.create(entityManager.getSqlSessionTemplate().getConfiguration(), entity.getType().getName());

		builder //
				.add(snippet.pureTableName()) //
				.add(snippet.tableName()) //
				.add(snippet.pureColumnList()) //
				.add(snippet.columnList()) //
				.add(snippet.columnListUsingType()) //
				.add(snippet.baseResultMap()) //
				.add(snippet.basicResultMap()) //
				.add(snippet.lazyResultMap()) //
				.add(snippet.resultMap()) //
				.add(snippet.insert(false)) //
				.add(snippet.insert(true)) //
				.add(snippet.update(false, false)) //
				.add(snippet.update(true, false)) //
				.add(snippet.update(false, true)) //
				.add(snippet.update(true, true)) //
				.add(snippet.deleteById(true)) //
				.add(snippet.deleteById(false)) //
				.add(snippet.deleteAllByIdInBatch()) //
				.add(snippet.deleteAllByEntitiesInBatch()) //
				.add(snippet.deleteAllInBatch()) //
				.add(snippet.countAll()) //
				.add(snippet.existsById()) //
				.add(snippet.findById()) //
				.add(snippet.findByIds()) //
				.add(snippet.findAll()) //
				.add(snippet.findAllWithSort()) //
				.add(snippet.findByPage()) //
				.add(snippet.count()) //
				.add(snippet.queryByExample()) //
				.add(snippet.queryByExampleWithSort()) //
				.add(snippet.queryByExampleWithPage()) //
				.add(snippet.countByExample()) //
				.add(snippet.existsByExample()) //
				.add(snippet.findByCriteria()) //

		;

		builder.build();
	}

	public static void compile(EntityManager entityManager, StagingMappers stagingMappers) {

		List<MybatisAssociation> associations = stagingMappers.getAssociations();
		if (CollectionUtils.isEmpty(associations)) {
			return;
		}

		for (MybatisAssociation association : associations) {

			MybatisStagingMapperSnippet snippet = new MybatisStagingMapperSnippet(entityManager, association);
			MybatisMapperBuilder builder = MybatisMapperBuilder
					.create(entityManager.getSqlSessionTemplate().getConfiguration(), snippet.getNamespace());
			builder.add(snippet.select());
			builder.build();
		}

	}

	public static void compile(IndicatingMybatisQuery query) {

		MybatisMapperBuilder builder = MybatisMapperBuilder.create(
				query.getEntityManager().getSqlSessionTemplate().getConfiguration(),
				query.getQueryMethod().getNamespace());
		builder.add(query.createSqlDefinition());
		builder.build();
	}

	public static void compile(PartTreeMybatisQuery query) {

		MybatisMapperBuilder builder = MybatisMapperBuilder.create(
				query.getEntityManager().getSqlSessionTemplate().getConfiguration(),
				query.getQueryMethod().getNamespace());
		builder.add(query.createSqlDefinition());
		builder.build();
	}

}
