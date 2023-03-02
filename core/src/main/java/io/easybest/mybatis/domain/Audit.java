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
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

/**
 * Base audit.
 *
 * @author Jarvis Song
 * @param <ID> identifier type
 * @param <AUDITOR> auditor identifier type
 */
@MappedSuperclass
public abstract class Audit<AUDITOR, ID extends Serializable> extends Id<ID> {

	private static final long serialVersionUID = -6950555360571982081L;

	/**
	 * 创建者.
	 */
	@CreatedBy
	@Column(name = "created_by")
	private @Nullable AUDITOR createdBy;

	/**
	 * 最后修改者.
	 */
	@LastModifiedBy
	@Column(name = "last_modified_by")
	private @Nullable AUDITOR lastModifiedBy;

	/**
	 * 创建时间.
	 */
	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_date")
	private @Nullable LocalDateTime createdDate;

	/**
	 * 最后修改时间.
	 */
	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_modified_date")
	private @Nullable LocalDateTime lastModifiedDate;

	@Nullable
	public AUDITOR getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(@Nullable AUDITOR createdBy) {
		this.createdBy = createdBy;
	}

	@Nullable
	public AUDITOR getLastModifiedBy() {
		return this.lastModifiedBy;
	}

	public void setLastModifiedBy(@Nullable AUDITOR lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	@Nullable
	public LocalDateTime getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(@Nullable LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	@Nullable
	public LocalDateTime getLastModifiedDate() {
		return this.lastModifiedDate;
	}

	public void setLastModifiedDate(@Nullable LocalDateTime lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

}
