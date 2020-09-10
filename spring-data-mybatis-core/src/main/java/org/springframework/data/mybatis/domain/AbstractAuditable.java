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
package org.springframework.data.mybatis.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.domain.Auditable;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for auditable entities. Stores the audition values in persistent
 * fields.
 *
 * @param <PK> primary key type
 * @param <U> auditor
 * @author JARVIS SONG
 * @since 2.0.1
 */
@MappedSuperclass
public abstract class AbstractAuditable<U, PK extends Serializable> extends AbstractPersistable<PK>
		implements Auditable<U, PK, LocalDateTime> {

	private static final long serialVersionUID = 4103127148219197466L;

	@ManyToOne
	@JoinColumn(name = "created_by")
	private @Nullable U createdBy;

	@Column(name = "created_date")
	@Temporal(TemporalType.TIMESTAMP)
	private @Nullable Date createdDate;

	@ManyToOne
	@JoinColumn(name = "last_modified_by")
	private @Nullable U lastModifiedBy;

	@Column(name = "last_modified_date")
	@Temporal(TemporalType.TIMESTAMP)
	private @Nullable Date lastModifiedDate;

	@Override
	public Optional<U> getCreatedBy() {
		return Optional.ofNullable(this.createdBy);
	}

	@Override
	public void setCreatedBy(U createdBy) {
		this.createdBy = createdBy;
	}

	@Override
	public Optional<LocalDateTime> getCreatedDate() {
		return (null != this.createdDate)
				? Optional.of(LocalDateTime.ofInstant(this.createdDate.toInstant(), ZoneId.systemDefault()))
				: Optional.empty();
	}

	@Override
	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant());
	}

	@Override
	public Optional<U> getLastModifiedBy() {
		return Optional.ofNullable(this.lastModifiedBy);
	}

	@Override
	public void setLastModifiedBy(U lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	@Override
	public Optional<LocalDateTime> getLastModifiedDate() {
		return (null != this.lastModifiedDate)
				? Optional.of(LocalDateTime.ofInstant(this.lastModifiedDate.toInstant(), ZoneId.systemDefault()))
				: Optional.empty();
	}

	@Override
	public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
		this.lastModifiedDate = Date.from(lastModifiedDate.atZone(ZoneId.systemDefault()).toInstant());
	}

}
