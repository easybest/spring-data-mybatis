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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basic Audit.
 *
 * @param <AUDITOR> auditor's type
 * @param <ID> id's type
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class Audit<AUDITOR, ID extends Serializable> extends Id<ID> {

	@Column(name = "created_by")
	private AUDITOR createdBy;

	@Column(name = "creation_date")
	private Date creationDate;

	@Column(name = "last_updated_by")
	private AUDITOR lastUpdatedBy;

	@Column(name = "last_update_date")
	private Date lastUpdateDate;

}
