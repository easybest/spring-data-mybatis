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

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Constant implements Segment {

	private final String value;

	public static Constant of(String value) {
		return new Constant(value);
	}

	public Constant(String value) {
		this.value = value;
	}

	/**
	 * Comma.
	 */
	public static final Constant COMMA = new Constant(",");

	/**
	 * Equals.
	 */
	public static final Constant EQUALS = new Constant("=");

	@Override
	public String toString() {
		return this.value;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String value;

		public Constant build() {

			return new Constant(this.value);
		}

		public Builder value(final String value) {
			this.value = value;
			return this;
		}

	}

}
