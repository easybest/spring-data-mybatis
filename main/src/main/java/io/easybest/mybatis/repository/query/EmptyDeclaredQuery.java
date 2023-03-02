/*
 * Copyright 2019-2023 the original author or authors.
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

import java.util.List;

import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 */
class EmptyDeclaredQuery implements DeclaredQuery {

	static final DeclaredQuery EMPTY_QUERY = new EmptyDeclaredQuery();

	@Override
	public boolean hasNamedParameter() {
		return false;
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public boolean hasConstructorExpression() {
		return false;
	}

	@Override
	public boolean isDefaultProjection() {
		return false;
	}

	@Override
	public List<StringQuery.ParameterBinding> getParameterBindings() {
		return null;
	}

	@Override
	public DeclaredQuery deriveCountQuery(String countQuery, String countQueryProjection) {

		Assert.hasText(countQuery, "CountQuery must not be empty!");

		return DeclaredQuery.of(countQuery);
	}

}
