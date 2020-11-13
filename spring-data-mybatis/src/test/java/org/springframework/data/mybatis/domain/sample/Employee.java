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
package org.springframework.data.mybatis.domain.sample;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mybatis.annotation.Fetch;
import org.springframework.data.mybatis.annotation.FetchMode;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
public class Employee implements Serializable {

	@EmbeddedId
	private Name name;

	private String emailAddress;

	private Integer age;

	@Enumerated(EnumType.ORDINAL)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	private Constellation constellation;

	@Lob
	private byte[] binaryData;

	@Embedded
	private Address address;

	@Version
	private Long version;

	@CreatedBy
	@Column(name = "created_by")
	private Long createdBy;

	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_date")
	private Timestamp createdDate;

	@LastModifiedBy
	@Column(name = "last_modified_by")
	private Long lastModifiedBy;

	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_modified_date")
	private Timestamp lastModifiedDate;

	@ManyToOne
	@JoinColumn(name = "dept_id")
	private Department department;

	@Fetch(FetchMode.JOIN)
	@OneToOne(mappedBy = "employee")
	private User user;

	public Employee(String firstname, String lastname) {
		this.name = new Name(firstname, lastname);
	}

	public enum Gender {

		MALE, FEMALE

	}

	public enum Constellation {

		Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius, Capricorn, Aquarius, Pisces

	}

}
