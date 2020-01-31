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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

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

	/**
	 * Calculate total mount.
	 * @param pager pager
	 * @param result result
	 * @param <X> entity type
	 * @return if return -1 means can not judge ,need count from database.
	 */
	protected <X> long calculateTotal(Pageable pager, List<X> result) {
		if (pager.hasPrevious()) {
			if (CollectionUtils.isEmpty(result)) {
				return -1;
			}
			if (result.size() == pager.getPageSize()) {
				return -1;
			}
			return (pager.getPageNumber() - 1) * pager.getPageSize() + result.size();
		}
		if (result.size() < pager.getPageSize()) {
			return result.size();
		}
		return -1;
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition,
			Map<String, Object> otherParams) {
		Map<String, Object> params = new HashMap<>();
		params.put("__offset", pager.getOffset());
		params.put("__pageSize", pager.getPageSize());
		params.put("__offsetEnd", pager.getOffset() + pager.getPageSize());
		if (condition instanceof Sort && ((Sort) condition).isSorted()) {
			params.put("__sort", condition);
		}
		else if (null != pager && null != pager.getSort() && pager.getSort().isSorted()) {
			params.put("__sort", pager.getSort());
		}
		params.put("__condition", condition);

		if (!CollectionUtils.isEmpty(otherParams)) {
			params.putAll(otherParams);
		}
		List<X> result = selectList(selectStatement, params);

		long total = calculateTotal(pager, result);
		if (total < 0) {
			total = selectOne(countStatement, params);
		}

		return new PageImpl<>(result, pager, total);
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement, String countStatement, Y condition) {
		return findByPager(pager, selectStatement, countStatement, condition, null);
	}

}
