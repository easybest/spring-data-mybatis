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

package io.easybest.mybatis.domain.sample;

import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.easybest.mybatis.annotation.GetterOptional;

/**
 * Sample domain class representing roles. Mapped with XML.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Entity
public class Role {

	private static final String PREFIX = "ROLE_";

	@Id
	@GeneratedValue
	private Integer id;

	@GetterOptional
	private String name;

	public Role() {
	}

	public Role(final String name) {
		this.name = name;
	}

	public Integer getId() {

		return this.id;
	}

	public Optional<String> getName() {
		return Optional.ofNullable(this.name);
	}

	@Override
	public String toString() {

		return PREFIX + this.name;
	}

	public boolean isNew() {

		return this.id == null;
	}

}
