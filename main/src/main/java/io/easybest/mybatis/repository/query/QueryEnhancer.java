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

package io.easybest.mybatis.repository.query;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface QueryEnhancer {

	default String applySorting(Sort sort) {

		return this.applySorting(sort, this.detectAlias());
	}

	String applySorting(Sort sort, @Nullable String alias);

	@Nullable
	String detectAlias();

	default String createCountQueryFor() {
		return this.createCountQueryFor(null);
	}

	String createCountQueryFor(@Nullable String countProjection);

	default boolean hasConstructorExpression() {
		return QueryUtils.hasConstructorExpression(this.getQuery().getQueryString());
	}

	String getProjection();

	Set<String> getJoinAliases();

	DeclaredQuery getQuery();

}
