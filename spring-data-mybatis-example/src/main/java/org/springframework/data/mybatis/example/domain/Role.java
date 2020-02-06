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
package org.springframework.data.mybatis.example.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Role entity.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Entity
public class Role {

	private static final String PREFIX = "ROLE_";

	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	public Role() {
	}

	public Role(final String name) {
		this.name = name;
	}

	public Integer getId() {

		return this.id;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {

		return PREFIX + this.name;
	}

	public boolean isNew() {

		return this.id == null;
	}

}
