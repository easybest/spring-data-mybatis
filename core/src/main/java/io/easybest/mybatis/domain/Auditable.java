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

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import static javax.persistence.TemporalType.TIMESTAMP;

/**
 * Base audit.
 *
 * @author Jarvis Song
 * @param <ID> identifier type
 * @param <AUDITOR> auditor identifier type
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
public abstract class Auditable<AUDITOR, ID extends Serializable> extends Id<ID> {

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
	@Temporal(TIMESTAMP)
	@Column(name = "created_date")
	private @Nullable LocalDateTime createdDate;

	/**
	 * 最后修改时间.
	 */
	@LastModifiedDate
	@Temporal(TIMESTAMP)
	@Column(name = "last_modified_date")
	private @Nullable LocalDateTime lastModifiedDate;

}
