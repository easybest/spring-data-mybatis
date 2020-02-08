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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

import org.springframework.util.CollectionUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisQuery extends SqlSessionDaoSupport implements Query {

	private final String statementId;

	private Map<Param<?>, Object> params;

	private Map<String, Param<?>> nameToParam;

	private Map<Integer, Param<?>> positionToParam;

	public MybatisQuery(SqlSessionTemplate sqlSessionTemplate, String statementId) {
		this.setSqlSessionTemplate(sqlSessionTemplate);
		this.statementId = statementId;
	}

	public void addParam(Param<?> key, Object value) {
		if (null == this.params) {
			this.params = new HashMap<>();
		}
		this.params.put(key, value);
		if (null != key.getName()) {
			if (null == this.nameToParam) {
				this.nameToParam = new HashMap<>();
			}
			this.nameToParam.put(key.getName(), key);
		}
		if (null != key.getPosition()) {
			if (null == this.positionToParam) {
				this.positionToParam = new HashMap<>();
			}
			this.positionToParam.put(key.getPosition(), key);
		}
	}

	private Map<String, Object> getParamsMap() {
		return this.params.entrySet().stream().collect(Collectors.toMap(entry -> {
			Param<?> param = entry.getKey();
			if (null != param.getName()) {
				return param.getName();
			}
			return "__p" + param.getPosition();
		}, entry -> entry.getValue()));
	}

	@Override
	public List getResultList() {
		if (CollectionUtils.isEmpty(this.params)) {
			return this.getSqlSession().selectList(this.statementId);
		}
		return this.getSqlSession().selectList(this.statementId, this.getParamsMap());
	}

	@Override
	public Object getSingleResult() {
		if (CollectionUtils.isEmpty(this.params)) {
			return this.getSqlSession().selectOne(this.statementId);
		}
		return this.getSqlSession().selectOne(this.statementId, this.getParamsMap());
	}

	@Override
	public int executeUpdate() {
		if (CollectionUtils.isEmpty(this.params)) {
			return this.getSqlSession().update(this.statementId);
		}
		return this.getSqlSession().update(this.statementId, this.getParamsMap());
	}

	@Override
	public Query setMaxResults(int maxResult) {
		return this;
	}

	@Override
	public int getMaxResults() {
		return 0;
	}

	@Override
	public Query setFirstResult(int startPosition) {
		return this;
	}

	@Override
	public int getFirstResult() {
		return 0;
	}

	@Override
	public Query setHint(String hintName, Object value) {
		return this;
	}

	@Override
	public Map<String, Object> getHints() {
		return null;
	}

	@Override
	public <T> Query setParameter(Parameter<T> param, T value) {
		this.addParam(new Param<>(param), value);
		return this;
	}

	@Override
	public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		this.addParam(new Param<>(param), value);
		return this;
	}

	@Override
	public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		this.addParam(new Param<>(param), value);
		return this;
	}

	@Override
	public Query setParameter(String name, Object value) {
		this.addParam(new Param<>(name, null, null), value);
		return this;
	}

	@Override
	public Query setParameter(String name, Calendar value, TemporalType temporalType) {
		this.addParam(new Param<>(name, null, null), value);
		return this;
	}

	@Override
	public Query setParameter(String name, Date value, TemporalType temporalType) {
		this.addParam(new Param<>(name, null, null), value);
		return this;
	}

	@Override
	public Query setParameter(int position, Object value) {
		this.addParam(new Param<>(null, position, null), value);
		return this;
	}

	@Override
	public Query setParameter(int position, Calendar value, TemporalType temporalType) {
		this.addParam(new Param<>(null, position, null), value);
		return this;
	}

	@Override
	public Query setParameter(int position, Date value, TemporalType temporalType) {
		this.addParam(new Param<>(null, position, null), value);
		return this;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		if (null == this.params) {
			return Collections.emptySet();
		}
		return (Set) this.params.keySet();
	}

	@Override
	public Parameter<?> getParameter(String name) {
		if (null == this.nameToParam) {
			return null;
		}
		Param<?> param = this.nameToParam.get(name);
		return param;
	}

	@Override
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		Parameter<?> parameter = this.getParameter(name);
		if (parameter.getParameterType() == type) {
			return (Parameter<T>) parameter;
		}
		return null;
	}

	@Override
	public Parameter<?> getParameter(int position) {
		if (null == this.positionToParam) {
			return null;
		}
		Param<?> param = this.positionToParam.get(position);
		return param;
	}

	@Override
	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		Parameter<?> parameter = this.getParameter(position);
		if (parameter.getParameterType() == type) {
			return (Parameter<T>) parameter;
		}
		return null;
	}

	@Override
	public boolean isBound(Parameter<?> param) {
		return false;
	}

	@Override
	public <T> T getParameterValue(Parameter<T> param) {
		return (T) this.params.get(param);
	}

	@Override
	public Object getParameterValue(String name) {
		if (null == this.nameToParam) {
			return null;
		}
		Param<?> param = this.nameToParam.get(name);
		return this.getParameterValue(param);
	}

	@Override
	public Object getParameterValue(int position) {
		if (null == this.positionToParam) {
			return null;
		}
		Param<?> param = this.positionToParam.get(position);
		return this.getParameterValue(param);
	}

	@Override
	public Query setFlushMode(FlushModeType flushMode) {
		return this;
	}

	@Override
	public FlushModeType getFlushMode() {
		return null;
	}

	@Override
	public Query setLockMode(LockModeType lockMode) {
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return null;
	}

	public static class Param<T> implements Parameter<T> {

		private String name;

		private Integer position;

		private Class<T> parameterType;

		public Param(Parameter<T> parameter) {
			this(parameter.getName(), parameter.getPosition(), parameter.getParameterType());
		}

		public Param(String name, Integer position, Class<T> parameterType) {
			this.name = name;
			this.position = position;
			this.parameterType = parameterType;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Integer getPosition() {
			return this.position;
		}

		@Override
		public Class<T> getParameterType() {
			return this.parameterType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			Param<?> param = (Param<?>) o;
			return Objects.equals(this.name, param.name) && Objects.equals(this.position, param.position)
					&& Objects.equals(this.parameterType, param.parameterType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name, this.position, this.parameterType);
		}

		@Override
		public String toString() {
			return "Param{" + "name='" + this.name + '\'' + ", position=" + this.position + ", parameterType="
					+ this.parameterType + '}';
		}

	}

}
