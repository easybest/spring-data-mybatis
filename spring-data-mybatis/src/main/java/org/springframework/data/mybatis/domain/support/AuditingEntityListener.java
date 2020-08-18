/*
 * Copyright 2008-2020 the original author or authors.
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

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Auditing entity listener.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Configurable
public class AuditingEntityListener {

	private @Nullable ObjectFactory<AuditingHandler> handler;

	public void setAuditingHandler(ObjectFactory<AuditingHandler> auditingHandler) {

		Assert.notNull(auditingHandler, "AuditingHandler must not be null!");
		this.handler = auditingHandler;
	}

	@PrePersist
	public void touchForCreate(Object target) {

		Assert.notNull(target, "Entity must not be null!");

		if (this.handler != null) {

			AuditingHandler object = this.handler.getObject();
			if (object != null) {
				object.markCreated(target);
			}
		}
	}

	@PreUpdate
	public void touchForUpdate(Object target) {

		Assert.notNull(target, "Entity must not be null!");

		if (this.handler != null) {

			AuditingHandler object = this.handler.getObject();
			if (object != null) {
				object.markModified(target);
			}
		}
	}

}
