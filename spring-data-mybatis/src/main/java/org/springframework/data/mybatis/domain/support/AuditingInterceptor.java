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
package org.springframework.data.mybatis.domain.support;

import java.util.Map;

import javax.persistence.Entity;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.mybatis.repository.support.ResidentParameterName;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }), })
public class AuditingInterceptor implements Interceptor {

	private MybatisAuditingHandler handler;

	public AuditingInterceptor(ObjectFactory<MybatisAuditingHandler> auditingHandler) {

		if (null != auditingHandler) {
			this.handler = auditingHandler.getObject();
		}
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (null == this.handler) {
			return invocation.proceed();
		}

		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		Object parameter = invocation.getArgs()[1];
		Object entity;
		if (parameter instanceof Map) {
			entity = ((Map) parameter).get(ResidentParameterName.ENTITY);
		}
		else {
			entity = parameter;
		}

		if (null == entity || !entity.getClass().isAnnotationPresent(Entity.class)) {
			return invocation.proceed();
		}

		if (sqlCommandType == SqlCommandType.INSERT) {
			this.handler.markCreated(entity);
		}
		else if (sqlCommandType == SqlCommandType.UPDATE) {
			this.handler.markModified(entity);
		}

		return invocation.proceed();
	}

}
