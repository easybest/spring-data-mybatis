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

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mybatis.domain.AbstractPersistable;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
public class User extends AbstractPersistable<Long> {

	private String name;

	private String password;

	@OneToOne
	@JoinColumns({ @JoinColumn(name = "employee_firstname", referencedColumnName = "firstname"),
			@JoinColumn(name = "employee_lastname", referencedColumnName = "lastname") })
	private Employee employee;

	@OrderBy("name asc")
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
	private Set<Role> roles;

}
