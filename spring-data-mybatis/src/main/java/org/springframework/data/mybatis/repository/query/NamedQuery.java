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

import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link RepositoryQuery} based on
 * {@link javax.persistence.NamedQuery}s.
 *
 * @author JARVIS SONG
 */
@Slf4j
final class NamedQuery extends AbstractMybatisQuery {

	NamedQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method) {
		super(sqlSessionTemplate, method);
	}

	private static boolean hasNamedQuery(String queryName, MybatisMappingContext mappingContext) {

		return mappingContext.getPersistentEntities().stream().anyMatch((MybatisPersistentEntity<?> entity) -> {
			javax.persistence.NamedQuery namedQuery = entity.findAnnotation(javax.persistence.NamedQuery.class);
			if ((null != namedQuery) && queryName.equals(namedQuery.name())) {
				return true;
			}
			NamedQueries namedQueries = entity.findAnnotation(NamedQueries.class);
			if (null != namedQueries) {
				for (javax.persistence.NamedQuery nq : namedQueries.value()) {
					if (queryName.equals(nq.name())) {
						return true;
					}
				}
			}

			NamedNativeQuery namedNativeQuery = entity.findAnnotation(NamedNativeQuery.class);
			if (null != namedNativeQuery && queryName.equals(namedNativeQuery.name())) {
				return true;
			}
			NamedNativeQueries namedNativeQueries = entity.findAnnotation(NamedNativeQueries.class);
			if (null != namedNativeQueries) {
				for (NamedNativeQuery nq : namedNativeQueries.value()) {
					if (queryName.equals(nq.name())) {
						return true;
					}
				}
			}

			return false;
		});

	}

	@Nullable
	public static RepositoryQuery lookupFrom(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method,
			MybatisMappingContext mappingContext) {

		final String queryName = method.getNamedQueryName();

		log.debug("Looking up named query {}", queryName);

		if (!hasNamedQuery(queryName, mappingContext)) {
			return null;
		}

		try {
			RepositoryQuery query = new NamedQuery(sqlSessionTemplate, method);
			log.debug("Found named query {}!", queryName);
			return query;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
