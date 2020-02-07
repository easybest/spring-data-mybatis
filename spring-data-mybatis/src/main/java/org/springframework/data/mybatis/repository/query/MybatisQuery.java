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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisQuery extends SqlSessionDaoSupport implements Query {

	protected final String statementId;

	protected Map<String, Object> params;

	public MybatisQuery(SqlSessionTemplate sqlSessionTemplate, String statementId) {
		this.setSqlSessionTemplate(sqlSessionTemplate);
		this.statementId = statementId;
	}

	public MybatisQuery(SqlSessionTemplate sqlSessionTemplate, String statementId, Map<String, Object> params) {
		this.setSqlSessionTemplate(sqlSessionTemplate);
		this.statementId = statementId;
		this.params = params;
	}

	public void addParam(String key, String value) {
		if (null == this.params) {
			this.params = new HashMap<>();
		}
		this.params.put(key, value);
	}

	@Override
	public List getResultList() {
		return this.getSqlSession().selectList(this.statementId, this.getParams());
	}

	@Override
	public Object getSingleResult() {
		return this.getSqlSession().selectOne(this.statementId, this.getParams());
	}

	@Override
	public int executeUpdate() {
		return this.getSqlSession().update(this.statementId, this.getParams());
	}

	@Override
	public Query setMaxResults(int i) {
		return this;
	}

	@Override
	public int getMaxResults() {
		return 0;
	}

	@Override
	public Query setFirstResult(int i) {
		return this;
	}

	@Override
	public int getFirstResult() {
		return 0;
	}

	@Override
	public Query setHint(String s, Object o) {
		return null;
	}

	@Override
	public Map<String, Object> getHints() {
		return null;
	}

	@Override
	public <T> Query setParameter(Parameter<T> parameter, T t) {

		return this;
	}

	@Override
	public Query setParameter(Parameter<Calendar> parameter, Calendar calendar, TemporalType temporalType) {
		return this;
	}

	@Override
	public Query setParameter(Parameter<Date> parameter, Date date, TemporalType temporalType) {
		return this;
	}

	@Override
	public Query setParameter(String s, Object o) {
		return this;
	}

	@Override
	public Query setParameter(String s, Calendar calendar, TemporalType temporalType) {
		return this;
	}

	@Override
	public Query setParameter(String s, Date date, TemporalType temporalType) {
		return this;
	}

	@Override
	public Query setParameter(int i, Object o) {
		return this;
	}

	@Override
	public Query setParameter(int i, Calendar calendar, TemporalType temporalType) {
		return this;
	}

	@Override
	public Query setParameter(int i, Date date, TemporalType temporalType) {
		return this;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		return null;
	}

	@Override
	public Parameter<?> getParameter(String s) {
		return null;
	}

	@Override
	public <T> Parameter<T> getParameter(String s, Class<T> aClass) {
		return null;
	}

	@Override
	public Parameter<?> getParameter(int i) {
		return null;
	}

	@Override
	public <T> Parameter<T> getParameter(int i, Class<T> aClass) {
		return null;
	}

	@Override
	public boolean isBound(Parameter<?> parameter) {
		return false;
	}

	@Override
	public <T> T getParameterValue(Parameter<T> parameter) {
		return null;
	}

	@Override
	public Object getParameterValue(String s) {
		return null;
	}

	@Override
	public Object getParameterValue(int i) {
		return null;
	}

	@Override
	public Query setFlushMode(FlushModeType flushModeType) {
		return null;
	}

	@Override
	public FlushModeType getFlushMode() {
		return null;
	}

	@Override
	public Query setLockMode(LockModeType lockModeType) {
		return null;
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> aClass) {
		return null;
	}

	public Map<String, Object> getParams() {
		return this.params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

}
