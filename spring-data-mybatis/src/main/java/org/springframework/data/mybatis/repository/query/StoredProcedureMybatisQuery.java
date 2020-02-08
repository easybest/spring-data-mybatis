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

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.QueryMethod;

/**
 * {@link AbstractMybatisQuery} implementation that inspects a {@link MybatisQueryMethod}
 * for the existence of an {@link Procedure} annotation.
 *
 * @author JARVIS SONG
 */
class StoredProcedureMybatisQuery extends AbstractMybatisQuery {

	private final StoredProcedureAttributes procedureAttributes;

	private final boolean useNamedParameters;

	StoredProcedureMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method) {
		super(sqlSessionTemplate, method);

		this.procedureAttributes = method.getProcedureAttributes();
		this.useNamedParameters = useNamedParameters(method);
	}

	private static boolean useNamedParameters(QueryMethod method) {
		for (Parameter parameter : method.getParameters()) {
			if (parameter.isNamedParameter()) {
				return true;
			}
		}
		return false;
	}

}
