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
import java.util.stream.Stream;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * MyBatis executor.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisExecutor {

	private final SqlSessionTemplate sqlSessionTemplate;

	private final MybatisExecuteFunction<Map<String, Object>> callback;

	public static MybatisExecutor create(SqlSessionTemplate sqlSessionTemplate,
			MybatisExecuteFunction<Map<String, Object>> callback) {
		return new MybatisExecutor(sqlSessionTemplate, callback);
	}

	public MybatisExecutor(SqlSessionTemplate sqlSessionTemplate,
			MybatisExecuteFunction<Map<String, Object>> executeFunction) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.callback = executeFunction;
	}

	public List getResultList(String statementId, MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.callback.execute(statementId, accessor);
		return this.sqlSessionTemplate.selectList(statementId, params);
	}

	public List getResultList(String statementId) {
		return this.sqlSessionTemplate.selectList(statementId);
	}

	public Stream getResultStream(String statementId, MybatisParametersParameterAccessor accessor) {
		return this.getResultList(statementId, accessor).stream();
	}

	public Stream getResultStream(String statementId) {
		return this.getResultList(statementId).stream();
	}

	public Object getSingleResult(String statementId, MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.callback.execute(statementId, accessor);
		return this.sqlSessionTemplate.selectOne(statementId, params);
	}

	public Object getSingleResult(String statementId) {
		return this.sqlSessionTemplate.selectOne(statementId);
	}

	public int executeUpdate(String statementId, MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.callback.execute(statementId, accessor);
		return this.sqlSessionTemplate.update(statementId, params);
	}

	public int executeDelete(String statementId, MybatisParametersParameterAccessor accessor) {
		Map<String, Object> params = this.callback.execute(statementId, accessor);
		return this.sqlSessionTemplate.delete(statementId, params);
	}

}
