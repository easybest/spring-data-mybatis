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

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * A wrapper for a String representation of a query offering information about the query.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public interface DeclaredQuery {

	static DeclaredQuery of(@Nullable String query) {
		return StringUtils.isEmpty(query) ? EmptyDeclaredQuery.EMPTY_QUERY : new StringQuery(query);
	}

	boolean hasNamedParameter();

	String getQueryString();

	@Nullable
	String getAlias();

	boolean hasConstructorExpression();

	boolean isDefaultProjection();

	List<StringQuery.ParameterBinding> getParameterBindings();

	DeclaredQuery deriveCountQuery(@Nullable String countQuery, @Nullable String countQueryProjection);

	default boolean usesPaging() {
		return false;
	}

	boolean usesJdbcStyleParameters();

}
