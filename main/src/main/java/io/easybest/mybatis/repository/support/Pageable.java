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

package io.easybest.mybatis.repository.support;

import java.io.Serializable;

import lombok.Data;

/**
 * .
 *
 * @author Jarvis Song
 */
@Data
public class Pageable implements Serializable {

	private static final long serialVersionUID = -764308195468450433L;

	/**
	 * Page Number.
	 */
	private int page;

	/**
	 * Page Size.
	 */
	private int size;

	private long offset;

	private boolean unpaged = false;

	public Pageable(int page, int size) {

		this.page = page;
		this.size = size;
		this.offset = (long) page * size;
	}

	public Pageable(int page, int size, long offset) {
		this.page = page;
		this.size = size;
		this.offset = offset;
	}

	public static Pageable of(org.springframework.data.domain.Pageable pageable) {

		if (pageable.isUnpaged()) {
			Pageable p = new Pageable(-1, -1);
			p.setUnpaged(true);
			return p;
		}

		return new Pageable(pageable.getPageNumber(), pageable.getPageSize(), pageable.getOffset());
	}

	public boolean isPaged() {
		return !this.unpaged;
	}

}
