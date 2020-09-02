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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
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

/**
 * .
 *
 * @author JARVIS SONG
 */
@Entity
@Table(name = "t_customer")
@Data
@NoArgsConstructor
public class Customer implements Serializable {

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

	@Version
	private Long version;

	@CreatedBy
	@Column(name = "created_by")
	private Long createdBy;

	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "creation_date")
	private Date creationDate;

	@LastModifiedBy
	@Column(name = "last_updated_by")
	private Long lastUpdatedBy;

	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_update_date")
	private Date lastUpdateDate;

	@OrderBy("name desc")
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "t_customer_goods",
			joinColumns = { @JoinColumn(name = "customer_firstname", referencedColumnName = "firstname"),
					@JoinColumn(name = "customer_lastname", referencedColumnName = "lastname") },
			inverseJoinColumns = @JoinColumn(name = "goods_id", referencedColumnName = "id"))
	private List<Goods> goods = Collections.emptyList();

	public Customer(String firstname, String lastname) {
		this.name = new Name(firstname, lastname);
	}

	public Customer(String firstname, String lastname, String emailAddress) {
		this.name = new Name(firstname, lastname);
		this.emailAddress = emailAddress;
	}

	public enum Gender {

		MALE, FEMALE

	}

	public enum Constellation {

		Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius, Capricorn, Aquarius, Pisces

	}

}
