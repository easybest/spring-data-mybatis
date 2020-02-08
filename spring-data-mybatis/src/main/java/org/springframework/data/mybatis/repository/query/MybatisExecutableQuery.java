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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisExecutableQuery {

	private final SqlSessionTemplate sqlSessionTemplate;

	private final String statementId;

	private final Function<MybatisParametersParameterAccessor, Map<String, Object>> paramCallback;

	public MybatisExecutableQuery(SqlSessionTemplate sqlSessionTemplate, String statementId,
			Function<MybatisParametersParameterAccessor, Map<String, Object>> paramCallback) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.statementId = statementId;
		this.paramCallback = paramCallback;
	}

	public List getResultList(MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.paramCallback.apply(accessor);
		return this.sqlSessionTemplate.selectList(this.statementId, params);
	}

	public List getResultList() {
		return this.sqlSessionTemplate.selectList(this.statementId);
	}

	public Stream getResultStream(MybatisParametersParameterAccessor accessor) {
		return this.getResultList(accessor).stream();
	}

	public Stream getResultStream() {
		return this.getResultList().stream();
	}

	public Object getSingleResult(MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.paramCallback.apply(accessor);
		return this.sqlSessionTemplate.selectOne(this.statementId, params);
	}

	public Object getSingleResult() {
		return this.sqlSessionTemplate.selectOne(this.statementId);
	}

	public int executeUpdate(MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.paramCallback.apply(accessor);
		return this.sqlSessionTemplate.update(this.statementId, params);
	}

	public int executeDelete(MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.paramCallback.apply(accessor);
		return this.sqlSessionTemplate.delete(this.statementId, params);
	}

}
