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

import java.util.List;

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Choose extends AbstractSegment {

	private String test;

	private Segment when;

	private Segment otherwise;

	public static Choose of(String test, Segment when, Segment otherwise) {
		return Choose.builder().test(test).when(when).otherwise(otherwise).build();
	}

	@Override
	public String toString() {

		return "<choose>" + "<when test=\"" + this.test + "\">" + this.when + "</when>" + "<otherwise>" + this.otherwise
				+ "</otherwise>" + "</choose>";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String test;

		private Segment when;

		private Segment otherwise;

		public Choose build() {

			Choose instance = new Choose();
			instance.contents = this.contents;
			instance.test = this.test;
			instance.when = this.when;
			instance.otherwise = this.otherwise;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder test(final String test) {
			this.test = test;
			return this;
		}

		public Builder when(final Segment when) {
			this.when = when;
			return this;
		}

		public Builder otherwise(final Segment otherwise) {
			this.otherwise = otherwise;
			return this;
		}

	}

}
