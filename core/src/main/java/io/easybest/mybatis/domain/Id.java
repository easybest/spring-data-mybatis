/*
 * Copyright 2019-2023 the original author or authors.
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

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.MappedSuperclass;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> pk
 */
@MappedSuperclass
public abstract class Id<T extends Serializable> implements Identifiable<T> {

	@GeneratedValue(strategy = GenerationType.AUTO)
	@jakarta.persistence.Id
	protected T id;

	public Id() {
	}

	public Id(T id) {
		this.id = id;
	}

	@Override
	public T getId() {
		return this.id;
	}

	public void setId(T id) {
		this.id = id;
	}

}
