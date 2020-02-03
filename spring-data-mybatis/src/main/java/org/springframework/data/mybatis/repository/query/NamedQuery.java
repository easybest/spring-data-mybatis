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

import lombok.extern.slf4j.Slf4j;

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

	NamedQuery(MybatisQueryMethod method) {
		super(method);
	}

	private static boolean hasNamedQuery(String queryName) {
		// TODO
		return false;
	}

	@Nullable
	public static RepositoryQuery lookupFrom(MybatisQueryMethod method) {

		final String queryName = method.getNamedQueryName();

		log.debug("Looking up named query {}", queryName);

		if (!hasNamedQuery(queryName)) {
			return null;
		}

		try {
			RepositoryQuery query = new NamedQuery(method);
			log.debug("Found named query {}!", queryName);
			return query;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
