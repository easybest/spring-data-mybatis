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
package io.easybest.mybatis.sample.domain;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

import org.springframework.data.mybatis.domain.LongId;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Entity
@Table(name = "user")
@Data
public class User extends LongId {

	private String firstname;

	private String lastname;

	private int age;

	private boolean active;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Column(nullable = false, unique = true)
	private String emailAddress;

	@ManyToMany
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles;

	@ManyToOne
	@JoinColumn(name = "manager_id", referencedColumnName = "id")
	private User manager;

	@Embedded
	private Address address;

	@Lob
	private byte[] binaryData;

	@ElementCollection
	private Set<String> attributes;

	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

}
