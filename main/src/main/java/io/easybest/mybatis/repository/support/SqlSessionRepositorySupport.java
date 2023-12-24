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

package io.easybest.mybatis.repository.support;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

/**
 * .
 *
 * @author Jarvis Song
 */
public abstract class SqlSessionRepositorySupport extends SqlSessionDaoSupport {

	private final String namespace;

	protected SqlSessionRepositorySupport(SqlSessionTemplate sqlSessionTemplate, String namespace) {
		this.namespace = namespace;
		this.setSqlSessionTemplate(sqlSessionTemplate);
	}

	protected String getNamespace() {
		return this.namespace;
	}

	protected String getStatementName(String statement) {
		return this.getNamespace() + '.' + statement;
	}

	protected <T> T selectOne(String statement, Object parameter) {
		return this.getSqlSession().selectOne(this.getStatementName(statement), parameter);
	}

	protected <T> T selectOne(String statement) {
		return this.getSqlSession().selectOne(this.getStatementName(statement));
	}

	protected <T> List<T> selectList(String statement) {
		return this.getSqlSession().selectList(this.getStatementName(statement));
	}

	protected <T> List<T> selectList(String statement, Object parameter) {
		return this.getSqlSession().selectList(this.getStatementName(statement), parameter);
	}

	protected int insert(String statement, Object parameter) {
		return this.getSqlSession().insert(this.getStatementName(statement), parameter);
	}

	protected int insert(String statement) {
		return this.getSqlSession().insert(this.getStatementName(statement));
	}

	protected int update(String statement, Object parameter) {
		return this.getSqlSession().update(this.getStatementName(statement), parameter);
	}

	protected int update(String statement) {
		return this.getSqlSession().update(this.getStatementName(statement));
	}

	protected int delete(String statement, Object parameter) {
		return this.getSqlSession().delete(this.getStatementName(statement), parameter);
	}

	protected int delete(String statement) {
		return this.getSqlSession().delete(this.getStatementName(statement));
	}

}
