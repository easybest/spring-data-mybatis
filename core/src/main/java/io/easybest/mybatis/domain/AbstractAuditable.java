/*
 * Copyright 2019-2022 the original author or authors.
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
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.domain.Auditable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.easybest.mybatis.annotation.GetterOptional;

/**
 * Abstract base class for auditable entities. Stores the audition values in persistent
 * fields.
 *
 * @author Jarvis Song
 * @param <PK> identifier type
 * @param <UID> auditor identifier type
 */
@MappedSuperclass
public abstract class AbstractAuditable<UID, PK extends Serializable> extends AbstractPersistable<PK>
		implements Auditable<UID, PK, LocalDateTime> {

	private static final long serialVersionUID = -5136973663465530271L;

	@GetterOptional
	@Column(name = "created_by")
	protected @Nullable UID createdBy;

	@GetterOptional
	@Column(name = "created_date")
	@Temporal(TemporalType.TIMESTAMP)
	protected @Nullable LocalDateTime createdDate;

	@GetterOptional
	@Column(name = "last_modified_by")
	private @Nullable UID lastModifiedBy;

	@GetterOptional
	@Column(name = "last_modified_date")
	@Temporal(TemporalType.TIMESTAMP)
	private @Nullable LocalDateTime lastModifiedDate;

	@NonNull
	@Override
	public Optional<UID> getCreatedBy() {
		return Optional.ofNullable(this.createdBy);
	}

	@Override
	public void setCreatedBy(@NonNull UID createdBy) {
		this.createdBy = createdBy;
	}

	@NonNull
	@Override
	public Optional<LocalDateTime> getCreatedDate() {
		return Optional.ofNullable(this.createdDate);
	}

	@Override
	public void setCreatedDate(@NonNull LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}

	@NonNull
	@Override
	public Optional<UID> getLastModifiedBy() {
		return Optional.ofNullable(this.lastModifiedBy);
	}

	@Override
	public void setLastModifiedBy(@NonNull UID lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	@NonNull
	@Override
	public Optional<LocalDateTime> getLastModifiedDate() {
		return Optional.ofNullable(this.lastModifiedDate);
	}

	@Override
	public void setLastModifiedDate(@NonNull LocalDateTime lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

}
