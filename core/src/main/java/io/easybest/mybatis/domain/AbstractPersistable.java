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

package io.easybest.mybatis.domain;

import java.io.Serializable;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for entities. Allows parameterization of identifier type, chooses
 * auto-generation and implements {@link #equals(Object)} and {@link #hashCode()} based on
 * that id.
 *
 * @author Jarvis Song
 * @param <PK> identifier type
 */
@MappedSuperclass
public abstract class AbstractPersistable<PK extends Serializable> implements Persistable<PK>, Serializable {

	private static final long serialVersionUID = -6173860901171822279L;

	@Id
	@GeneratedValue
	private @Nullable PK id;

	@Nullable
	@Override
	public PK getId() {
		return this.id;
	}

	public void setId(@Nullable PK id) {
		this.id = id;
	}

	@Transient
	@Override
	public boolean isNew() {
		return null == this.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (!this.getClass().equals(ProxyUtils.getUserClass(obj))) {
			return false;
		}
		AbstractPersistable<?> that = (AbstractPersistable<?>) obj;
		return null != this.getId() && this.getId().equals(that.getId());
	}

	@Override
	public int hashCode() {
		int hashCode = 17;
		hashCode += (null != this.getId()) ? this.getId().hashCode() * 31 : 0;
		return hashCode;
	}

	@Override
	public String toString() {
		return String.format("Entity of type %s with id: %s", this.getClass().getName(), this.getId());
	}

}
