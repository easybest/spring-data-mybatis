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

package io.easybest.mybatis.mapping.precompile;

import lombok.Getter;

import org.springframework.util.StringUtils;

import static io.easybest.mybatis.mapping.precompile.SQL.ROOT_ALIAS;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Table implements Segment {

	private String alias;

	private final String name;

	public static Builder builder() {
		return new Builder();
	}

	public static Table of(String name) {
		return new Table(name);
	}

	public static Table of(String name, String alias) {
		return Table.builder().alias(alias).name(name).build();
	}

	public static Table base(String value) {
		return Table.builder().alias(ROOT_ALIAS.getValue()).name(value).build();
	}

	public static Table base(String value, boolean baseAlias) {
		return Table.builder().alias(baseAlias ? ROOT_ALIAS.getValue() : null).name(value).build();
	}

	public Table(String value) {
		this.name = value;
	}

	@Override
	public String toString() {

		if (StringUtils.hasText(this.alias)) {
			return this.name + " " + this.alias;
		}

		return this.name;
	}

	public static class Builder {

		private String alias;

		private String name;

		public Table build() {

			Table bind = new Table(this.name);
			bind.alias = this.alias;
			return bind;
		}

		public Builder name(final String name) {
			this.name = name;
			return this;
		}

		public Builder alias(final String alias) {
			this.alias = alias;
			return this;
		}

	}

}
