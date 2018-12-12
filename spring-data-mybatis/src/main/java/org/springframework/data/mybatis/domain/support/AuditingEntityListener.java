/*
 * Copyright 2008-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.domain.support;

import static org.apache.ibatis.mapping.SqlCommandType.INSERT;
import static org.apache.ibatis.mapping.SqlCommandType.UPDATE;

import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.mybatis.auditing.MybatisAuditingHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.mybatis.spring.SqlSessionTemplate;

@Intercepts({ @Signature(type = Executor.class, method = "update", args = {
		MappedStatement.class, Object.class }) })
@Configurable
public class AuditingEntityListener implements InitializingBean, Interceptor {

	private @Nullable ObjectFactory<MybatisAuditingHandler> handler;

	private SqlSessionTemplate sqlSessionTemplate;

	public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	/**
	 * Configures the {@link AuditingHandler} to be used to set the current auditor on the
	 * domain types touched.
	 * @param auditingHandler must not be {@literal null}.
	 */
	public void setAuditingHandler(
			ObjectFactory<MybatisAuditingHandler> auditingHandler) {

		Assert.notNull(auditingHandler, "AuditingHandler must not be null!");
		this.handler = auditingHandler;
	}

	public void touchForCreate(Object target) {

		Assert.notNull(target, "Entity must not be null!");

		if (handler != null) {

			MybatisAuditingHandler object = handler.getObject();
			if (object != null) {
				object.markCreated(target);
			}
		}
	}

	public void touchForUpdate(Object target) {

		Assert.notNull(target, "Entity must not be null!");

		if (handler != null) {

			MybatisAuditingHandler object = handler.getObject();
			if (object != null) {
				object.markModified(target);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.sqlSessionTemplate.getConfiguration().addInterceptor(this);
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {

		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		Object target = invocation.getArgs()[1];
		if (INSERT == sqlCommandType) {
			touchForCreate(target);
		}
		else if (UPDATE == sqlCommandType) {
			touchForUpdate(target);
		}
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {

	}

}
