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
public class Column implements Segment {

	private String alias;

	private String value;

	public static Column of(String value) {
		return new Column(value);
	}

	public static Column of(String alias, String value) {
		return Column.builder().alias(alias).value(value).build();
	}

	public static Column base(String value) {
		return Column.builder().alias(ROOT_ALIAS.getValue()).value(value).build();
	}

	public static Column base(String value, boolean baseAlias) {
		return Column.builder().alias(baseAlias ? ROOT_ALIAS.getValue() : null).value(value).build();
	}

	public Column(String value) {
		this.value = value;
	}

	@Override
	public String toString() {

		if (StringUtils.hasText(this.alias)) {
			return this.alias + '.' + this.value;
		}

		return this.value;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String alias;

		private String value;

		public Column build() {

			Column instance = new Column(this.value);
			instance.alias = this.alias;

			return instance;
		}

		public Builder alias(final String alias) {
			this.alias = alias;
			return this;
		}

		public Builder value(final String value) {
			this.value = value;
			return this;
		}

	}

}
