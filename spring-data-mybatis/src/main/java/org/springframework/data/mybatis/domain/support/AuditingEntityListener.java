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

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Configurable
public class AuditingEntityListener {

	public void setAuditingHandler(ObjectFactory<MybatisAuditingHandler> auditingHandler) {
		Assert.notNull(auditingHandler, "AuditingHandler must not be null!");
		MybatisAuditingHandler handler = auditingHandler.getObject();
		if (null != handler.getSqlSessionTemplate()) {
			handler.getSqlSessionTemplate().getConfiguration().addInterceptor(new AuditingInterceptor(auditingHandler));
		}
	}

}
