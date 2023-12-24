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

import java.util.List;

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Bind extends AbstractSegment {

	private String name;

	private String value;

	public static Builder builder() {
		return new Builder();
	}

	public static Bind of(String name, String value) {
		return Bind.builder().name(name).value(value).build();
	}

	public static Bind of(String name, Segment value) {
		return Bind.builder().name(name).value(value.toString()).build();
	}

	@Override
	public String toString() {

		return "<bind" + " name=\"" + this.name + "\"" + " value=\"" + this.value + "\"" + "/>";
	}

	public static class Builder {

		private List<? extends Segment> contents;

		private String name;

		private String value;

		public Bind build() {

			Bind bind = new Bind();
			bind.contents = this.contents;
			bind.name = this.name;
			bind.value = this.value;
			return bind;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder name(final String name) {
			this.name = name;
			return this;
		}

		public Builder value(final String value) {
			this.value = value;
			return this;
		}

	}

}
