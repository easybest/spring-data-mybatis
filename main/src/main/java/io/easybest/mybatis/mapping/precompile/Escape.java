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

import io.easybest.mybatis.dialect.Dialect;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Escape implements Segment {

	private Dialect dialect;

	public static Escape of(Dialect dialect) {

		return Escape.builder().dialect(dialect).build();
	}

	@Override
	public String toString() {

		return this.dialect.escape(Parameter.of("entityManager.escapeCharacter.escapeCharacter").toString());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private Dialect dialect;

		public Escape build() {

			Escape instance = new Escape();
			instance.dialect = this.dialect;

			return instance;
		}

		public Builder dialect(final Dialect dialect) {
			this.dialect = dialect;
			return this;
		}

	}

}
