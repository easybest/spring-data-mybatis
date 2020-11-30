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
package org.springframework.data.mybatis.auditing;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

/**
 * Mybatis interceptor of auditing.
 *
 * @author JARVIS SONG
 * @since 2.0.1
 */
@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
public class AuditingInterceptor implements Interceptor {

	private final MybatisAuditingHandler handler;

	public AuditingInterceptor(MybatisAuditingHandler auditingHandler) {
		this.handler = auditingHandler;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (null == this.handler) {
			return invocation.proceed();
		}

		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		Object rawParameter = invocation.getArgs()[1];
		if (rawParameter instanceof Map) {
			Set<Object> auditedEntities = new HashSet<>();
			Map<?, ?> rawParameterMap = (Map<?, ?>) rawParameter;
			for (Map.Entry<?, ?> entry : rawParameterMap.entrySet()) {
				Object parameter = entry.getValue();
				if (parameter instanceof Iterable) {
					((Iterable<?>) parameter).forEach(entity -> {
						if (auditedEntities.contains(entity)) {
							return;
						}
						auditEntity(entity, sqlCommandType);
						auditedEntities.add(entity);
					});
				}
				else {
					if (auditedEntities.contains(parameter)) {
						continue;
					}
					auditEntity(parameter, sqlCommandType);
					auditedEntities.add(parameter);
				}
			}
		}
		else {
			auditEntity(rawParameter, sqlCommandType);
		}

		return invocation.proceed();
	}

	private void auditEntity(Object entity, SqlCommandType sqlCommandType) {
		if (null == entity || !entity.getClass().isAnnotationPresent(Entity.class)) {
			return;
		}

		if (sqlCommandType == SqlCommandType.INSERT) {
			this.handler.markCreated(entity);
		}
		else if (sqlCommandType == SqlCommandType.UPDATE) {
			this.handler.markModified(entity);
		}
	}

}
