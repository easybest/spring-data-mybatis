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
package org.springframework.data.mybatis.repository.support;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

/**
 * MyBatis session support.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public abstract class SqlSessionRepositorySupport extends SqlSessionDaoSupport {

	/**
	 * DOT.
	 */
	public static final char DOT = '.';

	protected SqlSessionRepositorySupport(SqlSessionTemplate sqlSessionTemplate) {
		this.setSqlSessionTemplate(sqlSessionTemplate);
	}

	/**
	 * Sub class can override this method.
	 * @return namespace
	 */
	protected abstract String getNamespace();

	/**
	 * get the mapper statement include namespace.
	 * @param partStatement partStatement
	 * @return statement
	 */
	protected String getStatement(String partStatement) {
		return this.getNamespace() + DOT + partStatement;
	}

	/**
	 * select one query.
	 * @param statement statement
	 * @param <T> entity class
	 * @return result
	 */
	protected <T> T selectOne(String statement) {
		return this.getSqlSession().selectOne(this.getStatement(statement));
	}

	protected <T> T selectOne(String statement, Object parameter) {
		return this.getSqlSession().selectOne(getStatement(statement), parameter);
	}

	protected <T> List<T> selectList(String statement) {
		return this.getSqlSession().selectList(getStatement(statement));
	}

	protected <T> List<T> selectList(String statement, Object parameter) {
		return this.getSqlSession().selectList(this.getStatement(statement), parameter);
	}

	protected int insert(String statement, Object parameter) {
		return this.getSqlSession().insert(this.getStatement(statement), parameter);
	}

	protected int update(String statement, Object parameter) {
		return this.getSqlSession().update(this.getStatement(statement), parameter);
	}

	protected int delete(String statement) {
		return this.getSqlSession().delete(this.getStatement(statement));
	}

	protected int delete(String statement, Object parameter) {
		return this.getSqlSession().delete(this.getStatement(statement), parameter);
	}

}
